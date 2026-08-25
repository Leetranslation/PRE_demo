package com.token.erp.service;

import com.token.erp.common.BizException;
import com.token.erp.dto.MrpConfirmRequest;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MRP 建议确认服务（44.10 用户确认后批量生成采购/生产单）
 * 44.12：已转换的不重复生成
 * 44.9/44.13：不自动下单，必须用户确认
 */
@Service
@RequiredArgsConstructor
public class MrpConfirmService {

    private final MrpRequirementMapper requirementMapper;
    private final MrpPlanMapper planMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final ProductionOrderMapper productionOrderMapper;
    private final CodeGenerator codeGenerator;

    /**
     * 批量转换 MRP 建议为采购订单/生产工单
     */
    @Transactional
    public void confirm(MrpConfirmRequest request) {
        if (request.getPlanId() == null) throw new BizException("计划ID不能为空");
        if (request.getRequirementIds() == null || request.getRequirementIds().isEmpty()) {
            throw new BizException("请选择要转换的建议");
        }

        // 按供应方式分组
        List<MrpRequirement> purchaseReqs = new ArrayList<>();
        List<MrpRequirement> makeReqs = new ArrayList<>();

        for (Long reqId : request.getRequirementIds()) {
            MrpRequirement req = requirementMapper.selectById(reqId);
            if (req == null) throw new BizException(404, "建议不存在");
            if (!"none".equals(req.getConvertedStatus())) {
                throw new BizException("建议已转换，不能重复生成：" + req.getItemId());
            }
            if (req.getNetQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("净需求为0，无需转换：" + req.getItemId());
            }
            if ("purchase".equals(req.getSupplyType())) {
                purchaseReqs.add(req);
            } else if ("make".equals(req.getSupplyType())) {
                makeReqs.add(req);
            } else {
                throw new BizException("物料供应方式为采购+自制，请先选择采购或自制：" + req.getItemId());
            }
        }

        // 生成采购订单（合并同一计划的所有采购建议）
        if (!purchaseReqs.isEmpty()) {
            generatePurchaseOrders(purchaseReqs);
        }

        // 生成生产工单
        for (MrpRequirement req : makeReqs) {
            generateProductionOrder(req);
        }

        // 更新计划状态为已确认
        MrpPlan plan = planMapper.selectById(request.getPlanId());
        if (plan != null) {
            plan.setStatus("confirmed");
            planMapper.updateById(plan);
        }
    }

    /** 生成采购订单（采购建议合并为一张采购订单，来源 MRP） */
    private void generatePurchaseOrders(List<MrpRequirement> reqs) {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(codeGenerator.next("PO"));
        order.setSupplierId(null); // MRP 采购单供应商待用户补充
        order.setOrderDate(LocalDate.now());
        order.setPurchaseType("mrp");
        order.setApproveStatus("pending");
        order.setBizStatus("pending_inbound");
        order.setSourceType("mrp");
        order.setSourceId(reqs.get(0).getPlanId());
        order.setTotalAmount(BigDecimal.ZERO);
        purchaseOrderMapper.insert(order);

        for (MrpRequirement req : reqs) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrderId(order.getId());
            item.setMaterialId(req.getItemId());
            item.setQuantity(req.getNetQty());
            item.setPrice(BigDecimal.ZERO);
            item.setTaxRate(new BigDecimal("13"));
            item.setAmountExTax(BigDecimal.ZERO);
            item.setTaxAmount(BigDecimal.ZERO);
            item.setTotalAmount(BigDecimal.ZERO);
            item.setExpectedDate(req.getSuggestDate());
            item.setInboundQty(BigDecimal.ZERO);
            purchaseOrderItemMapper.insert(item);

            // 更新建议转换状态
            req.setConvertedQty(req.getNetQty());
            req.setConvertedStatus("purchase");
            requirementMapper.updateById(req);
        }
    }

    /** 生成生产工单 */
    private void generateProductionOrder(MrpRequirement req) {
        ProductionOrder order = new ProductionOrder();
        order.setOrderNo(codeGenerator.next("MO"));
        order.setProductId(req.getItemId());
        order.setQuantity(req.getNetQty());
        order.setProductionType("mrp");
        order.setPlanDate(req.getSuggestDate() != null ? req.getSuggestDate() : LocalDate.now());
        order.setSourceType("mrp");
        order.setSourceId(req.getPlanId());
        order.setApproveStatus("pending");
        order.setBizStatus("pending");
        order.setCompletedQty(BigDecimal.ZERO);
        productionOrderMapper.insert(order);

        // 更新建议转换状态
        req.setConvertedQty(req.getNetQty());
        req.setConvertedStatus("make");
        requirementMapper.updateById(req);
    }
}
