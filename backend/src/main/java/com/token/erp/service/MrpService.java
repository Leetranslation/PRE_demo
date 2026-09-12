package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.common.BizException;
import com.token.erp.dto.MrpRequest;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MRP 核心服务
 * 严格对照文档 44 节（MRP 计划规则冻结）+ C 段（MRP 冻结结论）
 *
 * 净需求公式：净需求 = max(0, 毛需求 + 安全库存 - 可用库存 - 在途供应)
 * 可用库存 = 实际 - 预留 - 待检 - 不合格
 * 需求 = 数量 × (1 + 损耗率)
 * 多层 BOM 展开，同一物料汇总
 */
@Service
@RequiredArgsConstructor
public class MrpService {

    private final MrpPlanMapper planMapper;
    private final MrpRequirementMapper requirementMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final BomMapper bomMapper;
    private final BomItemMapper bomItemMapper;
    private final MaterialMapper materialMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final ProductionOrderMapper productionOrderMapper;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    /** 计算中的需求聚合项 */
    private static class Demand {
        Long itemId;
        String itemType;
        BigDecimal grossQty = BigDecimal.ZERO;      // 毛需求
        String sourceType;                          // 需求来源类型
        Long sourceId;                              // 需求来源ID
        String sourceNo;                            // 需求来源编号
        LocalDate demandDate;                       // 需求日期
    }

    /**
     * 执行 MRP 全局计算（44.1 按计划范围全局计算）
     * @return 生成的计划
     */
    @Transactional
    public MrpPlan calculate(MrpRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BizException("计划日期范围不能为空");
        }

        // 1. 汇总范围内已审批销售订单的产品需求（44.2）
        Map<String, Demand> demandMap = new LinkedHashMap<>();
        collectSalesDemand(request.getStartDate(), request.getEndDate(), demandMap);

        if (demandMap.isEmpty()) {
            throw new BizException("计划范围内无已审批销售订单需求");
        }

        // 2. 多层 BOM 展开（44.4，递归展开，防止循环）
        expandBom(demandMap);

        // 3. 创建计划
        MrpPlan plan = new MrpPlan();
        plan.setPlanNo(codeGenerator.next("MRP"));
        plan.setPlanDate(LocalDate.now());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setStatus("calculated");
        planMapper.insert(plan);

        // 4. 逐项计算净需求并保存建议（44.3 汇总，C 段净需求公式）
        for (Demand demand : demandMap.values()) {
            MrpRequirement req = calcRequirement(plan, demand);
            requirementMapper.insert(req);
        }

        return plan;
    }

    /** 收集销售需求（已审批销售订单的产品明细） */
    private void collectSalesDemand(LocalDate start, LocalDate end, Map<String, Demand> demandMap) {
        List<SalesOrder> orders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getApproveStatus, "approved")
                        .ge(SalesOrder::getOrderDate, start)
                        .le(SalesOrder::getOrderDate, end));
        for (SalesOrder order : orders) {
            List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                    new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, order.getId()));
            for (SalesOrderItem item : items) {
                // 已出库的数量不再产生需求（只算未出库部分）
                BigDecimal pendingQty = item.getQuantity().subtract(
                        item.getOutboundQty() == null ? BigDecimal.ZERO : item.getOutboundQty());
                if (pendingQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                String key = "product:" + item.getProductId();
                Demand d = demandMap.computeIfAbsent(key, k -> new Demand());
                d.itemId = item.getProductId();
                d.itemType = "product";
                d.grossQty = d.grossQty.add(pendingQty);
                // 来源保留第一个销售订单（44.11 保存需求来源）
                if (d.sourceId == null) {
                    d.sourceType = "sales_order";
                    d.sourceId = order.getId();
                    d.sourceNo = order.getOrderNo();
                }
                // 需求日期取最早的交货日期
                LocalDate dd = order.getDeliveryDate() != null ? order.getDeliveryDate() : order.getOrderDate();
                if (d.demandDate == null || dd.isBefore(d.demandDate)) {
                    d.demandDate = dd;
                }
            }
        }
    }

    /** 多层 BOM 展开（递归，损耗率，防循环） */
    private void expandBom(Map<String, Demand> demandMap) {
        // 用队列处理，不断展开产品需求为物料/子产品需求
        Set<String> expanded = new HashSet<>();
        boolean changed = true;
        int guard = 0;
        while (changed && guard < 100) {
            changed = false;
            guard++;
            // 快照当前所有 product 需求
            List<Demand> productDemands = new ArrayList<>();
            for (Demand d : demandMap.values()) {
                if ("product".equals(d.itemType) && !expanded.contains("product:" + d.itemId + ":" + d.sourceId + ":" + d.sourceNo)) {
                    productDemands.add(d);
                }
            }
            for (Demand d : productDemands) {
                // 查该产品的有效 BOM
                Bom bom = bomMapper.selectOne(
                        new LambdaQueryWrapper<Bom>()
                                .eq(Bom::getProductId, d.itemId)
                                .eq(Bom::getStatus, 1)
                                .orderByDesc(Bom::getId)
                                .last("LIMIT 1"));
                expanded.add("product:" + d.itemId + ":" + d.sourceId + ":" + d.sourceNo);
                if (bom == null) {
                    continue; // 无 BOM 的产品（可能直接采购）不再展开
                }
                // 展开 BOM 明细
                List<BomItem> bomItems = bomItemMapper.selectList(
                        new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, bom.getId()));
                for (BomItem bi : bomItems) {
                    // 需求 = 父项数量 × BOM子项数量 × (1+损耗率)（C 段损耗率）
                    BigDecimal lossRate = bi.getLossRate() == null ? BigDecimal.ZERO : bi.getLossRate();
                    BigDecimal perUnit = bi.getQuantity().multiply(
                            BigDecimal.ONE.add(lossRate)).setScale(6, RoundingMode.HALF_UP);
                    BigDecimal childQty = d.grossQty.multiply(perUnit).setScale(6, RoundingMode.HALF_UP);

                    String key = bi.getChildType() + ":" + bi.getChildId();
                    Demand child = demandMap.computeIfAbsent(key, k -> new Demand());
                    child.itemId = bi.getChildId();
                    child.itemType = bi.getChildType();
                    child.grossQty = child.grossQty.add(childQty);
                    if (child.sourceId == null) {
                        child.sourceType = d.sourceType;
                        child.sourceId = d.sourceId;
                        child.sourceNo = d.sourceNo;
                    }
                    if (child.demandDate == null || (d.demandDate != null && d.demandDate.isBefore(child.demandDate))) {
                        child.demandDate = d.demandDate;
                    }
                    // 如果子项还是 product，需要继续展开
                    if ("product".equals(bi.getChildType())) {
                        changed = true;
                    }
                }
            }
        }
        if (guard >= 100) {
            throw new BizException("BOM 展开层级过深或存在循环引用");
        }
    }

    /** 计算单条需求的净需求并生成建议 */
    private MrpRequirement calcRequirement(MrpPlan plan, Demand demand) {
        // 安全库存（物料）
        BigDecimal safetyStock = BigDecimal.ZERO;
        String supplyType = "purchase";
        Integer leadDays = 0;
        if ("material".equals(demand.itemType)) {
            Material material = materialMapper.selectById(demand.itemId);
            if (material != null) {
                safetyStock = material.getSafetyStock() == null ? BigDecimal.ZERO : material.getSafetyStock();
                supplyType = material.getSupplyType() == null ? "purchase" : material.getSupplyType();
                leadDays = material.getPurchaseLeadDays() == null ? 0 : material.getPurchaseLeadDays();
            }
        } else {
            Product product = productMapper.selectById(demand.itemId);
            if (product != null) {
                // 产品默认自制（如果无 BOM 则采购）
                supplyType = "make";
                leadDays = product.getProductionLeadDays() == null ? 0 : product.getProductionLeadDays();
            }
        }

        // 可用库存 = 实际 - 预留 - 待检 - 不合格（C 段）
        BigDecimal availableQty = calcAvailableQty(demand.itemId, demand.itemType);

        // 在途供应 = 已审批未完成入库的采购 + 已审批未完成的生产（44.4）
        BigDecimal inTransitQty = calcInTransitQty(demand.itemId, demand.itemType);

        // 净需求 = max(0, 毛需求 + 安全库存 - 可用库存 - 在途供应)（C 段）
        BigDecimal netQty = demand.grossQty.add(safetyStock)
                .subtract(availableQty).subtract(inTransitQty);
        if (netQty.compareTo(BigDecimal.ZERO) < 0) {
            netQty = BigDecimal.ZERO;
        }

        // 建议日期 = 需求日期 - 提前期（44.6 倒推）
        LocalDate suggestDate = demand.demandDate;
        if (demand.demandDate != null && leadDays > 0) {
            suggestDate = demand.demandDate.minusDays(leadDays);
        }

        MrpRequirement req = new MrpRequirement();
        req.setPlanId(plan.getId());
        req.setItemId(demand.itemId);
        req.setItemType(demand.itemType);
        req.setGrossQty(demand.grossQty);
        req.setAvailableQty(availableQty);
        req.setInTransitQty(inTransitQty);
        req.setNetQty(netQty);
        req.setSupplyType(supplyType);
        req.setDemandDate(demand.demandDate);
        req.setSuggestDate(suggestDate);
        req.setSourceType(demand.sourceType);
        req.setSourceId(demand.sourceId);
        req.setSourceNo(demand.sourceNo);
        req.setConvertedQty(BigDecimal.ZERO);
        req.setConvertedStatus("none");
        return req;
    }

    /** 可用库存 = 实际 - 预留（待检/不合格不计入，因为它们本就不在 available 维度） */
    private BigDecimal calcAvailableQty(Long itemId, String itemType) {
        List<Inventory> invs = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getItemId, itemId)
                        .eq(Inventory::getItemType, itemType)
                        .eq(Inventory::getStockType, "available"));
        BigDecimal total = BigDecimal.ZERO;
        for (Inventory inv : invs) {
            BigDecimal qty = inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity();
            BigDecimal reserved = inv.getReservedQty() == null ? BigDecimal.ZERO : inv.getReservedQty();
            total = total.add(qty.subtract(reserved));
        }
        return total;
    }

    /** 在途供应 = 已审批未完成入库的采购 + 已审批未完成的生产 */
    private BigDecimal calcInTransitQty(Long itemId, String itemType) {
        BigDecimal total = BigDecimal.ZERO;

        if ("material".equals(itemType)) {
            // 采购在途：已审批采购订单，未入库数量
            List<PurchaseOrder> pos = purchaseOrderMapper.selectList(
                    new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getApproveStatus, "approved"));
            for (PurchaseOrder po : pos) {
                if ("completed".equals(po.getBizStatus())) continue;
                List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                        new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, po.getId()));
                for (PurchaseOrderItem item : items) {
                    if (item.getMaterialId().equals(itemId)) {
                        BigDecimal unInbound = item.getQuantity().subtract(
                                item.getInboundQty() == null ? BigDecimal.ZERO : item.getInboundQty());
                        total = total.add(unInbound);
                    }
                }
            }
        } else {
            // 生产在途：已审批生产工单，未完成数量
            List<ProductionOrder> mos = productionOrderMapper.selectList(
                    new LambdaQueryWrapper<ProductionOrder>().eq(ProductionOrder::getApproveStatus, "approved"));
            for (ProductionOrder mo : mos) {
                if ("completed".equals(mo.getBizStatus())) continue;
                if (mo.getProductId().equals(itemId)) {
                    BigDecimal uncompleted = mo.getQuantity().subtract(
                            mo.getCompletedQty() == null ? BigDecimal.ZERO : mo.getCompletedQty());
                    total = total.add(uncompleted);
                }
            }
        }
        return total;
    }

    /** 查询计划建议列表 */
    public List<MrpRequirement> requirements(Long planId) {
        List<MrpRequirement> list = requirementMapper.selectList(
                new LambdaQueryWrapper<MrpRequirement>()
                        .eq(MrpRequirement::getPlanId, planId)
                        .orderByDesc(MrpRequirement::getId));
        if (!list.isEmpty()) {
            List<Long> pids = list.stream().filter(r -> "product".equals(r.getItemType()))
                    .map(MrpRequirement::getItemId).collect(Collectors.toList());
            List<Long> mids = list.stream().filter(r -> !"product".equals(r.getItemType()))
                    .map(MrpRequirement::getItemId).collect(Collectors.toList());
            Map<Long, String[]> pm = nameFillUtil.productMap(pids);
            Map<Long, String[]> mm = nameFillUtil.materialMap(mids);
            list.forEach(r -> {
                String[] arr = "product".equals(r.getItemType()) ? pm.get(r.getItemId()) : mm.get(r.getItemId());
                if (arr != null) {
                    r.setItemCode(arr[0]);
                    r.setItemName(arr[1]);
                }
            });
        }
        return list;
    }

    /** 查询计划列表 */
    public List<MrpPlan> plans() {
        return planMapper.selectList(
                new LambdaQueryWrapper<MrpPlan>().orderByDesc(MrpPlan::getId));
    }
}
