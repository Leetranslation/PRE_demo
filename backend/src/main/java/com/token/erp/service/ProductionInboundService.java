package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.ProductionInboundRequest;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生产入库服务
 * 规则：入库 ≤ 未完成数量（禁止超收）；可配置「先领料才能入库」；质检物料入待检
 */
@Service
@RequiredArgsConstructor
public class ProductionInboundService {

    private final ProductionInboundMapper inboundMapper;
    private final ProductionInboundItemMapper inboundItemMapper;
    private final ProductionOrderMapper orderMapper;
    private final ProductionOrderService orderService;
    private final ProductMapper productMapper;
    private final MaterialIssueMapper issueMapper;
    private final MaterialIssueItemMapper issueItemMapper;
    private final OutsourceOrderMapper outsourceMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final JdbcTemplate jdbcTemplate;
    private final IdempotencyService idempotencyService;
    private final NameFillUtil nameFillUtil;

    public Page<ProductionInbound> page(long page, long pageSize, Long orderId, String inboundType) {
        LambdaQueryWrapper<ProductionInbound> qw = new LambdaQueryWrapper<>();
        if (orderId != null) qw.eq(ProductionInbound::getOrderId, orderId);
        if (StringUtils.hasText(inboundType)) {
            qw.eq(ProductionInbound::getInboundType, inboundType);
        } else {
            qw.eq(ProductionInbound::getInboundType, "production");
        }
        qw.orderByDesc(ProductionInbound::getId);
        return inboundMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 生产入库单明细 */
    public List<ProductionInboundItem> items(Long inboundId) {
        List<ProductionInboundItem> list = inboundItemMapper.selectList(
                new LambdaQueryWrapper<ProductionInboundItem>().eq(ProductionInboundItem::getInboundId, inboundId));
        if (!list.isEmpty()) {
            Map<Long, String[]> pm = nameFillUtil.productMap(
                    list.stream().map(ProductionInboundItem::getProductId).collect(Collectors.toList()));
            list.forEach(i -> {
                String[] arr = pm.get(i.getProductId());
                if (arr != null) {
                    i.setProductCode(arr[0]);
                    i.setProductName(arr[1]);
                }
            });
        }
        return list;
    }

    @Transactional
    public ProductionInbound create(ProductionInboundRequest request) {
        // 幂等控制：防止重复点击/网络重试导致重复入库
        idempotencyService.checkAndOccupy(request.getRequestId(), "production_inbound");
        ProductionOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("仅已审批工单可入库");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("入库明细不能为空");

        // 入库类型：production生产 / outsource_whole整单外协 / outsource_process工序外协
        String inboundType = StringUtils.hasText(request.getInboundType()) ? request.getInboundType() : "production";
        if (!"production".equals(inboundType)) {
            if (request.getOutsourceId() == null) throw new BizException("外协入库必须关联外协单");
            OutsourceOrder outsource = outsourceMapper.selectById(request.getOutsourceId());
            if (outsource == null) throw new BizException(404, "外协单不存在");
            if (!"approved".equals(outsource.getApproveStatus())) throw new BizException("外协单未审批，不能入库");
        }

        // 开关：先领料才能入库（默认 false）
        if (isRequireIssueBeforeInbound() && !isIssued(order.getId())) {
            throw new BizException("系统配置要求先领料才能入库，请先完成领料");
        }

        ProductionInbound inbound = new ProductionInbound();
        inbound.setInboundNo(codeGenerator.next("RK"));
        inbound.setInboundType(inboundType);
        inbound.setOrderId(request.getOrderId());
        inbound.setOutsourceId(request.getOutsourceId());
        inbound.setWarehouseId(request.getWarehouseId());
        inbound.setInboundDate(request.getInboundDate() != null ? request.getInboundDate() : LocalDate.now());
        inbound.setStatus("completed");
        inbound.setRemark(request.getRemark());
        inboundMapper.insert(inbound);

        BigDecimal totalInbound = BigDecimal.ZERO;
        for (ProductionInboundItem item : request.getItems()) {
            if (item.getProductId() == null) throw new BizException("产品不能为空");
            // 校验入库产品与工单产品一致，防止串产品入库
            if (!item.getProductId().equals(order.getProductId())) {
                throw new BizException("入库产品与工单产品不一致，禁止入库");
            }

            // 校验：入库 ≤ 未完成数量（禁止超收）
            BigDecimal uncompleted = order.getQuantity().subtract(order.getCompletedQty());
            if (item.getQuantity().compareTo(uncompleted) > 0) {
                throw new BizException("入库数量超过未完成数量，当前可入库：" + uncompleted);
            }

            // 判断是否质检产品
            Product product = productMapper.selectById(item.getProductId());
            boolean needQc = product != null && product.getProductionQc() != null && product.getProductionQc() == 1;

            String stockType = needQc ? "pending_qc" : "available";
            Inventory dim = new Inventory();
            dim.setItemId(item.getProductId());
            dim.setItemType("product");
            dim.setWarehouseId(request.getWarehouseId());
            dim.setLocationId(item.getLocationId());
            dim.setBatchNo(item.getBatchNo());
            dim.setSerialNo(item.getSerialNo());
            dim.setStockType(stockType);
            inventoryService.change(dim, "in", item.getQuantity(), "production_inbound", inbound.getId(), inbound.getInboundNo());

            item.setId(null);
            item.setInboundId(inbound.getId());
            inboundItemMapper.insert(item);

            totalInbound = totalInbound.add(item.getQuantity());
        }

        // 更新工单完成数量（工序外协入库为半成品收回，不计成品完工）
        if (!"outsource_process".equals(inboundType)) {
            orderService.updateCompletedQty(order.getId(), totalInbound);
        }
        return inbound;
    }

    /** 读取系统参数：先领料才能入库 */
    private boolean isRequireIssueBeforeInbound() {
        try {
            String val = jdbcTemplate.queryForObject(
                    "SELECT config_value FROM sys_config WHERE config_key='require_issue_before_inbound'",
                    String.class);
            return "true".equalsIgnoreCase(val);
        } catch (Exception e) {
            return false; // 未配置默认 false
        }
    }

    /** 判断工单是否已领料 */
    private boolean isIssued(Long orderId) {
        Long count = issueMapper.selectCount(
                new LambdaQueryWrapper<MaterialIssue>().eq(MaterialIssue::getOrderId, orderId));
        return count != null && count > 0;
    }
}
