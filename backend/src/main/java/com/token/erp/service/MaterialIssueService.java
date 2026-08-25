package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.common.BizException;
import com.token.erp.dto.MaterialIssueRequest;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 生产领料服务
 * 规则：领料数量 ≤ BOM 需求（禁止超领），调库存服务出库
 */
@Service
@RequiredArgsConstructor
public class MaterialIssueService {

    private final MaterialIssueMapper issueMapper;
    private final MaterialIssueItemMapper issueItemMapper;
    private final ProductionOrderMapper orderMapper;
    private final BomItemMapper bomItemMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;

    @Transactional
    public MaterialIssue create(MaterialIssueRequest request) {
        ProductionOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("仅已审批工单可领料");
        if (order.getBomId() == null) throw new BizException("该工单无 BOM，无法领料");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("领料明细不能为空");

        MaterialIssue issue = new MaterialIssue();
        issue.setIssueNo(codeGenerator.next("LL"));
        issue.setOrderId(request.getOrderId());
        issue.setWarehouseId(request.getWarehouseId());
        issue.setIssueDate(request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now());
        issue.setStatus("completed");
        issue.setRemark(request.getRemark());
        issueMapper.insert(issue);

        // BOM 明细（该工单使用的 BOM 版本）
        List<BomItem> bomItems = bomItemMapper.selectList(
                new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, order.getBomId()));

        for (MaterialIssueItem item : request.getItems()) {
            if (item.getMaterialId() == null) throw new BizException("物料不能为空");
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("领料数量必须大于0");
            }

            // 校验超领：领料累计 ≤ BOM 需求
            BigDecimal requirement = calcRequirement(bomItems, item.getMaterialId(), order.getQuantity());
            BigDecimal alreadyIssued = calcAlreadyIssued(order.getId(), item.getMaterialId());
            BigDecimal availableToIssue = requirement.subtract(alreadyIssued);
            if (item.getQuantity().compareTo(availableToIssue) > 0) {
                throw new BizException("领料超量：物料 " + item.getMaterialId()
                        + " 需求 " + requirement + "，已领 " + alreadyIssued
                        + "，本次可领 " + availableToIssue);
            }

            // 调库存服务出库
            Inventory dim = new Inventory();
            dim.setItemId(item.getMaterialId());
            dim.setItemType("material");
            dim.setWarehouseId(request.getWarehouseId());
            dim.setLocationId(item.getLocationId());
            dim.setBatchNo(item.getBatchNo());
            dim.setSerialNo(item.getSerialNo());
            dim.setStockType("available");
            inventoryService.change(dim, "out", item.getQuantity(), "material_issue", issue.getId(), issue.getIssueNo());

            item.setId(null);
            item.setIssueId(issue.getId());
            issueItemMapper.insert(item);
        }
        return issue;
    }

    /** 计算某物料的 BOM 需求 = bom数量 × 工单数量 × (1+损耗率) */
    private BigDecimal calcRequirement(List<BomItem> bomItems, Long materialId, BigDecimal orderQty) {
        for (BomItem bi : bomItems) {
            if (bi.getChildId().equals(materialId) && "material".equals(bi.getChildType())) {
                BigDecimal lossRate = bi.getLossRate() == null ? BigDecimal.ZERO : bi.getLossRate();
                // 需求 = bom数量 × (1+损耗率) × 工单数量
                BigDecimal perUnit = bi.getQuantity().multiply(
                        BigDecimal.ONE.add(lossRate)).setScale(6, RoundingMode.HALF_UP);
                return perUnit.multiply(orderQty).setScale(6, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    /** 计算已领料数量（该工单所有领料单的该物料合计） */
    private BigDecimal calcAlreadyIssued(Long orderId, Long materialId) {
        // 查该工单所有领料单
        List<MaterialIssue> issues = issueMapper.selectList(
                new LambdaQueryWrapper<MaterialIssue>().eq(MaterialIssue::getOrderId, orderId));
        BigDecimal total = BigDecimal.ZERO;
        for (MaterialIssue issue : issues) {
            List<MaterialIssueItem> items = issueItemMapper.selectList(
                    new LambdaQueryWrapper<MaterialIssueItem>()
                            .eq(MaterialIssueItem::getIssueId, issue.getId())
                            .eq(MaterialIssueItem::getMaterialId, materialId));
            for (MaterialIssueItem item : items) {
                total = total.add(item.getQuantity());
            }
        }
        return total;
    }
}
