package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.SalesOutboundRequest;
import com.token.erp.entity.*;
import com.token.erp.mapper.SalesOrderItemMapper;
import com.token.erp.mapper.SalesOrderMapper;
import com.token.erp.mapper.SalesOutboundItemMapper;
import com.token.erp.mapper.SalesOutboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 销售出库服务
 * 规则：出库 ≤ 未出库数量（禁止超发），调库存服务出库
 */
@Service
@RequiredArgsConstructor
public class SalesOutboundService {

    private final SalesOutboundMapper outboundMapper;
    private final SalesOutboundItemMapper outboundItemMapper;
    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final IdempotencyService idempotencyService;
    private final NameFillUtil nameFillUtil;

    public Page<SalesOutbound> page(long page, long pageSize, Long orderId) {
        LambdaQueryWrapper<SalesOutbound> qw = new LambdaQueryWrapper<>();
        if (orderId != null) qw.eq(SalesOutbound::getOrderId, orderId);
        qw.orderByDesc(SalesOutbound::getId);
        return outboundMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 出库单明细 */
    public List<SalesOutboundItem> items(Long outboundId) {
        List<SalesOutboundItem> list = outboundItemMapper.selectList(
                new LambdaQueryWrapper<SalesOutboundItem>().eq(SalesOutboundItem::getOutboundId, outboundId));
        if (!list.isEmpty()) {
            Map<Long, String[]> pm = nameFillUtil.productMap(
                    list.stream().map(SalesOutboundItem::getProductId).collect(Collectors.toList()));
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
    public SalesOutbound create(SalesOutboundRequest request) {
        // 幂等控制：防止重复点击/网络重试导致重复出库
        idempotencyService.checkAndOccupy(request.getRequestId(), "sales_outbound");
        SalesOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "销售订单不存在");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("仅已审批订单可出库");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("出库明细不能为空");

        SalesOutbound outbound = new SalesOutbound();
        outbound.setOutboundNo(codeGenerator.next("CK"));
        outbound.setOrderId(request.getOrderId());
        outbound.setWarehouseId(request.getWarehouseId());
        outbound.setOutboundDate(request.getOutboundDate() != null ? request.getOutboundDate() : LocalDate.now());
        outbound.setStatus("completed");
        outbound.setRemark(request.getRemark());
        outboundMapper.insert(outbound);

        for (SalesOutboundItem item : request.getItems()) {
            if (item.getProductId() == null) throw new BizException("产品不能为空");
            SalesOrderItem orderItem = item.getOrderItemId() != null
                    ? orderItemMapper.selectById(item.getOrderItemId())
                    : orderItemMapper.selectOne(
                            new LambdaQueryWrapper<SalesOrderItem>()
                                    .eq(SalesOrderItem::getOrderId, request.getOrderId())
                                    .eq(SalesOrderItem::getProductId, item.getProductId())
                                    .orderByAsc(SalesOrderItem::getId)
                                    .last("LIMIT 1"));
            if (orderItem == null) throw new BizException("订单中不存在该产品");

            // 校验：出库 ≤ 未出库数量（禁止超发）
            BigDecimal unOutbound = orderItem.getQuantity().subtract(
                    orderItem.getOutboundQty() == null ? BigDecimal.ZERO : orderItem.getOutboundQty());
            if (item.getQuantity().compareTo(unOutbound) > 0) {
                throw new BizException("出库数量超过未出库数量，当前可出库：" + unOutbound);
            }
            // 补全 orderItemId
            item.setOrderItemId(orderItem.getId());

            // 调库存服务出库
            Inventory dim = new Inventory();
            dim.setItemId(item.getProductId());
            dim.setItemType("product");
            dim.setWarehouseId(request.getWarehouseId());
            dim.setLocationId(item.getLocationId());
            dim.setBatchNo(item.getBatchNo());
            dim.setSerialNo(item.getSerialNo());
            dim.setStockType("available");
            inventoryService.change(dim, "out", item.getQuantity(), "sales_outbound", outbound.getId(), outbound.getOutboundNo());

            item.setId(null);
            item.setOutboundId(outbound.getId());
            item.setUnit(orderItem.getUnit());
            outboundItemMapper.insert(item);

            // 更新订单明细已出库数量
            orderItem.setOutboundQty(orderItem.getOutboundQty().add(item.getQuantity()));
            orderItemMapper.updateById(orderItem);
        }

        // 更新订单业务状态
        updateOrderBizStatus(order.getId());
        return outbound;
    }

    private void updateOrderBizStatus(Long orderId) {
        Long pendingCount = orderItemMapper.selectCount(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getOrderId, orderId)
                        .apply("outbound_qty < quantity"));
        SalesOrder order = orderMapper.selectById(orderId);
        if (pendingCount == 0) {
            order.setBizStatus("completed");
        } else {
            order.setBizStatus("partial_outbound");
        }
        orderMapper.updateById(order);
    }
}
