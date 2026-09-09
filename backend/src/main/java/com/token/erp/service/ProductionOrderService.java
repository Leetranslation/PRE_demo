package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.ProductionOrderRequest;
import com.token.erp.entity.Bom;
import com.token.erp.entity.BomVersion;
import com.token.erp.entity.ProcessRoute;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.mapper.BomMapper;
import com.token.erp.mapper.BomVersionMapper;
import com.token.erp.mapper.ProcessRouteMapper;
import com.token.erp.mapper.ProductionOrderMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProductionOrderService {

    private final ProductionOrderMapper orderMapper;
    private final BomMapper bomMapper;
    private final BomVersionMapper bomVersionMapper;
    private final ProcessRouteMapper processRouteMapper;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    public Page<ProductionOrder> page(long page, long pageSize, String keyword, String approveStatus, String orderType) {
        LambdaQueryWrapper<ProductionOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(ProductionOrder::getOrderNo, keyword);
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(ProductionOrder::getApproveStatus, approveStatus);
        }
        if (StringUtils.hasText(orderType)) {
            qw.eq(ProductionOrder::getOrderType, orderType);
        } else {
            // 默认只查成品工单，半成品在独立页面查询
            qw.eq(ProductionOrder::getOrderType, "product");
        }
        qw.orderByDesc(ProductionOrder::getId);
        return orderMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public ProductionOrder create(ProductionOrderRequest request) {
        if (request.getProductId() == null) throw new BizException("产品不能为空");
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("生产数量必须大于0");
        }

        ProductionOrder order = new ProductionOrder();
        order.setOrderNo(codeGenerator.next("MO"));
        order.setOrderType(StringUtils.hasText(request.getOrderType()) ? request.getOrderType() : "product");
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setProductionType(StringUtils.hasText(request.getProductionType()) ? request.getProductionType() : "stock");
        order.setPlanDate(request.getPlanDate() != null ? request.getPlanDate() : LocalDate.now());
        order.setPlanFinishDate(request.getPlanFinishDate());
        order.setDepartmentId(request.getDepartmentId());
        order.setWorkshopId(request.getWorkshopId());
        order.setMachineId(request.getMachineId());
        order.setProcessRouteId(request.getProcessRouteId());
        order.setOwner(request.getOwner());
        order.setUrgency(StringUtils.hasText(request.getUrgency()) ? request.getUrgency() : "normal");
        order.setOutsourceStatus(StringUtils.hasText(request.getOutsourceStatus()) ? request.getOutsourceStatus() : "none");
        order.setSourceType(request.getSourceType());
        order.setSourceId(request.getSourceId());
        order.setApproveStatus("pending");
        order.setBizStatus("pending");
        order.setFinishStatus("unfinished");
        order.setInboundStatus("none");
        order.setIssueStatus("none");
        order.setQcStatus("not_needed");
        order.setMachineStatus(request.getMachineId() != null ? "linked" : "unlinked");
        order.setCompletedQty(BigDecimal.ZERO);
        order.setRemark(request.getRemark());

        // 自动带出当前有效 BOM 及版本（文档 10.3：选产品自动带出有效 BOM）
        Bom bom = bomMapper.selectOne(
                new LambdaQueryWrapper<Bom>()
                        .eq(Bom::getProductId, request.getProductId())
                        .eq(Bom::getStatus, 1)
                        .orderByDesc(Bom::getId)
                        .last("LIMIT 1"));
        if (bom != null) {
            order.setBomId(bom.getId());
            BomVersion version = bomVersionMapper.selectOne(
                    new LambdaQueryWrapper<BomVersion>()
                            .eq(BomVersion::getBomId, bom.getId())
                            .eq(BomVersion::getStatus, 1)
                            .orderByDesc(BomVersion::getId)
                            .last("LIMIT 1"));
            if (version != null) {
                order.setBomVersionId(version.getId());
            }
        }

        // 选产品自动带出该产品的有效工艺路线（未手动指定时）
        if (order.getProcessRouteId() == null) {
            ProcessRoute route = processRouteMapper.selectOne(
                    new LambdaQueryWrapper<ProcessRoute>()
                            .eq(ProcessRoute::getProductId, request.getProductId())
                            .eq(ProcessRoute::getStatus, 1)
                            .orderByDesc(ProcessRoute::getId)
                            .last("LIMIT 1"));
            if (route != null) {
                order.setProcessRouteId(route.getId());
            }
        }

        orderMapper.insert(order);
        return order;
    }

    @Transactional
    public void submit(Long id) {
        ProductionOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (!"pending".equals(order.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        order.setApproveStatus("approving");
        orderMapper.updateById(order);
    }

    @Transactional
    public void approve(Long id) {
        ProductionOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (!"approving".equals(order.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        // 禁止自审校验
        checkSelfApprove(order.getCreateBy());
        order.setApproveStatus("approved");
        // 状态机升级：审批通过后仍为「未开始」，由「工单开始」动作触发进入「执行中」
        order.setBizStatus("pending");
        orderMapper.updateById(order);
        // 通知制单人审批结果
        notificationService.sendToUsername(order.getCreateBy(), "approve_result",
                "生产工单已审批", "生产工单 " + order.getOrderNo() + " 已审批通过",
                "production_order", order.getId());
    }

    /** 禁止自审校验（配置 allow_self_approve=false 时生效） */
    private void checkSelfApprove(String createBy) {
        if (!sysConfigService.getBoolean("allow_self_approve")) {
            String currentUser = securityUtils.currentUsername();
            if (createBy != null && createBy.equals(currentUser)) {
                throw new BizException("禁止自审：不能审批自己提交的单据");
            }
        }
    }

    @Transactional
    public void unApprove(Long id) {
        ProductionOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("仅已审批可反审核");
        if (!"pending".equals(order.getBizStatus()) && !"producing".equals(order.getBizStatus())) {
            throw new BizException("已产生领料/入库的工单不能反审核，需先处理下游业务");
        }
        order.setApproveStatus("pending");
        order.setBizStatus("pending");
        orderMapper.updateById(order);
    }

    /** 工单开始：未开始 → 执行中 */
    @Transactional
    public void start(Long id) {
        ProductionOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (!"pending".equals(order.getBizStatus())) throw new BizException("仅未开始状态的工单可开始");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("工单未审批，不能开始");
        order.setBizStatus("producing");
        orderMapper.updateById(order);
    }

    /** 工单完工：执行中 → 已完工 */
    @Transactional
    public void finish(Long id) {
        ProductionOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (!"producing".equals(order.getBizStatus())) throw new BizException("仅执行中状态的工单可完工");
        order.setBizStatus("finished");
        order.setFinishStatus("finished");
        orderMapper.updateById(order);
    }

    /** 工单作废 */
    @Transactional
    public void voidOrder(Long id) {
        ProductionOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "生产工单不存在");
        if ("completed".equals(order.getBizStatus())) throw new BizException("已完成的工单不能作废");
        order.setBizStatus("voided");
        orderMapper.updateById(order);
    }

    /** 更新工单完成数量（入库后调用） */
    public void updateCompletedQty(Long orderId, BigDecimal qty) {
        ProductionOrder order = orderMapper.selectById(orderId);
        if (order == null) return;
        BigDecimal completed = order.getCompletedQty().add(qty);
        order.setCompletedQty(completed);
        // 业务状态：部分完成 / 已完成
        if (completed.compareTo(order.getQuantity()) >= 0) {
            order.setBizStatus("completed");
        } else if (completed.compareTo(BigDecimal.ZERO) > 0) {
            order.setBizStatus("partial");
        }
        orderMapper.updateById(order);
    }
}
