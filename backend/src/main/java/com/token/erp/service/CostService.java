package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.common.BizException;
import com.token.erp.dto.CostResult;
import com.token.erp.entity.BomItem;
import com.token.erp.entity.Material;
import com.token.erp.entity.Process;
import com.token.erp.entity.Product;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.entity.ReportWork;
import com.token.erp.mapper.BomItemMapper;
import com.token.erp.mapper.MaterialMapper;
import com.token.erp.mapper.ProcessMapper;
import com.token.erp.mapper.ProductMapper;
import com.token.erp.mapper.ProductionOrderMapper;
import com.token.erp.mapper.ReportWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 成本核算：材料成本 + 人工成本 + 制造费用
 * - 材料成本 = Σ(BOM 子件数量 × (1+损耗率) × 子件标准成本)
 * - 人工成本 = Σ(已审报工单，按工序计价方式：计件×良品 / 计时×工时 / 固定价)
 * - 制造费用 = 材料成本 × 制造费用率（sys_config.overhead_rate）
 */
@Service
@RequiredArgsConstructor
public class CostService {

    private final ProductionOrderMapper orderMapper;
    private final BomItemMapper bomItemMapper;
    private final MaterialMapper materialMapper;
    private final ProductMapper productMapper;
    private final ReportWorkMapper reportWorkMapper;
    private final ProcessMapper processMapper;
    private final SysConfigService sysConfigService;

    @Transactional
    public CostResult calculate(Long orderId) {
        ProductionOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(404, "生产工单不存在");

        BigDecimal materialCost = calcMaterialCost(order);
        BigDecimal laborCost = calcLaborCost(orderId);
        BigDecimal overheadCost = materialCost.multiply(overheadRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = materialCost.add(laborCost).add(overheadCost);

        BigDecimal qty = order.getCompletedQty() != null && order.getCompletedQty().compareTo(BigDecimal.ZERO) > 0
                ? order.getCompletedQty() : order.getQuantity();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("工单完工数量为 0，无法核算单位成本");
        }
        BigDecimal unitCost = totalCost.divide(qty, 6, RoundingMode.HALF_UP);

        // 回写工单成本字段
        order.setMaterialCost(materialCost);
        order.setLaborCost(laborCost);
        order.setOverheadCost(overheadCost);
        order.setTotalCost(totalCost);
        order.setUnitCost(unitCost);
        orderMapper.updateById(order);

        CostResult r = new CostResult();
        r.setOrderId(order.getId());
        r.setOrderNo(order.getOrderNo());
        r.setCompletedQty(qty);
        r.setMaterialCost(materialCost);
        r.setLaborCost(laborCost);
        r.setOverheadCost(overheadCost);
        r.setTotalCost(totalCost);
        r.setUnitCost(unitCost);
        return r;
    }

    /** 材料成本：BOM 展开 × 子件标准成本 × (1+损耗率) */
    private BigDecimal calcMaterialCost(ProductionOrder order) {
        if (order.getBomId() == null) return BigDecimal.ZERO;
        List<BomItem> items = bomItemMapper.selectList(
                new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, order.getBomId()));
        BigDecimal total = BigDecimal.ZERO;
        for (BomItem item : items) {
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal lossRate = item.getLossRate() == null ? BigDecimal.ZERO : item.getLossRate();
            BigDecimal cost = getItemCost(item.getChildId(), item.getChildType());
            total = total.add(qty.multiply(BigDecimal.ONE.add(lossRate)).multiply(cost));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getItemCost(Long itemId, String itemType) {
        if (itemId == null) return BigDecimal.ZERO;
        if ("product".equals(itemType)) {
            Product p = productMapper.selectById(itemId);
            return p != null && p.getStandardCost() != null ? p.getStandardCost() : BigDecimal.ZERO;
        }
        Material m = materialMapper.selectById(itemId);
        return m != null && m.getStandardCost() != null ? m.getStandardCost() : BigDecimal.ZERO;
    }

    /** 人工成本：已审报工单，按工序计价方式 */
    private BigDecimal calcLaborCost(Long orderId) {
        List<ReportWork> list = reportWorkMapper.selectList(
                new LambdaQueryWrapper<ReportWork>()
                        .eq(ReportWork::getWorkOrderId, orderId)
                        .eq(ReportWork::getAuditStatus, "approved"));
        BigDecimal total = BigDecimal.ZERO;
        for (ReportWork rw : list) {
            if (rw.getProcessId() == null) continue;
            Process p = processMapper.selectById(rw.getProcessId());
            if (p == null || p.getStandardPrice() == null) continue;
            BigDecimal price = p.getStandardPrice();
            if ("time".equals(p.getPriceType())) {
                BigDecimal hours = rw.getWorkHours() == null ? BigDecimal.ZERO : rw.getWorkHours();
                total = total.add(price.multiply(hours));
            } else if ("piece".equals(p.getPriceType())) {
                BigDecimal qty = rw.getQualifiedQty() == null ? BigDecimal.ZERO : rw.getQualifiedQty();
                total = total.add(price.multiply(qty));
            } else {
                total = total.add(price);
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** 制造费用率（sys_config.overhead_rate，如 0.1 表示 10%） */
    private BigDecimal overheadRate() {
        String v = sysConfigService.getString("overhead_rate");
        if (v == null || v.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
