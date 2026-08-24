package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PurchaseInboundRequest;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PurchaseInboundService {

    private final PurchaseInboundMapper inboundMapper;
    private final PurchaseInboundItemMapper inboundItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper orderItemMapper;
    private final MaterialMapper materialMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;

    public Page<PurchaseInbound> page(long page, long pageSize, Long orderId) {
        LambdaQueryWrapper<PurchaseInbound> qw = new LambdaQueryWrapper<>();
        if (orderId != null) qw.eq(PurchaseInbound::getOrderId, orderId);
        qw.orderByDesc(PurchaseInbound::getId);
        return inboundMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /**
     * 采购入库：校验 + 调库存服务
     * 质检物料 → 待检库存；非质检物料 → 可用库存
     */
    @Transactional
    public PurchaseInbound create(PurchaseInboundRequest request) {
        PurchaseOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "采购订单不存在");
        if (!"approved".equals(order.getApproveStatus())) {
            throw new BizException("仅已审批的订单可入库");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("入库明细不能为空");
        }

        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setInboundNo(codeGenerator.next("RK"));
        inbound.setOrderId(request.getOrderId());
        inbound.setWarehouseId(request.getWarehouseId());
        inbound.setInboundDate(request.getInboundDate() != null ? request.getInboundDate() : LocalDate.now());
        inbound.setStatus("completed");
        inbound.setRemark(request.getRemark());
        inboundMapper.insert(inbound);

        for (PurchaseInboundItem item : request.getItems()) {
            if (item.getMaterialId() == null) throw new BizException("物料不能为空");
            PurchaseOrderItem orderItem = orderItemMapper.selectById(item.getOrderItemId());
            if (orderItem == null) throw new BizException("订单明细不存在");

            // 校验：入库数量 ≤ 未入库数量（禁止超收）
            BigDecimal unInbound = orderItem.getQuantity().subtract(
                    orderItem.getInboundQty() == null ? BigDecimal.ZERO : orderItem.getInboundQty());
            if (item.getQuantity().compareTo(unInbound) > 0) {
                throw new BizException("入库数量超过未入库数量，当前可入库：" + unInbound);
            }

            // 判断是否质检物料
            Material material = materialMapper.selectById(item.getMaterialId());
            boolean needQc = material != null && material.getPurchaseQc() != null && material.getPurchaseQc() == 1;

            // 入库到库存（质检→待检，非质检→可用）
            String stockType = needQc ? "pending_qc" : "available";
            Inventory dim = new Inventory();
            dim.setItemId(item.getMaterialId());
            dim.setItemType("material");
            dim.setWarehouseId(request.getWarehouseId());
            dim.setLocationId(item.getLocationId());
            dim.setBatchNo(item.getBatchNo());
            dim.setSerialNo(item.getSerialNo());
            dim.setStockType(stockType);
            inventoryService.change(dim, "in", item.getQuantity(), "purchase_inbound", inbound.getId(), inbound.getInboundNo());

            // 保存入库明细
            item.setId(null);
            item.setInboundId(inbound.getId());
            item.setUnit(orderItem.getUnit());
            inboundItemMapper.insert(item);

            // 更新订单明细已入库数量
            orderItem.setInboundQty(orderItem.getInboundQty().add(item.getQuantity()));
            orderItemMapper.updateById(orderItem);
        }

        // 更新订单业务状态
        updateOrderBizStatus(order.getId());
        return inbound;
    }

    /** 更新订单业务状态：判断是否全部入库完成 */
    private void updateOrderBizStatus(Long orderId) {
        Long pendingCount = orderItemMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getOrderId, orderId)
                        .apply("inbound_qty < quantity"));
        PurchaseOrder order = orderMapper.selectById(orderId);
        if (pendingCount == 0) {
            order.setBizStatus("completed");
        } else {
            order.setBizStatus("partial_inbound");
        }
        orderMapper.updateById(order);
    }
}
