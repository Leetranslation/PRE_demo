package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.dto.CockpitStats;
import com.token.erp.dto.DashboardStats;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页驾驶舱服务（文档 15.1 节）
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SalesOrderMapper salesOrderMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final ProductionOrderMapper productionOrderMapper;
    private final CustomerMapper customerMapper;
    private final SupplierMapper supplierMapper;
    private final MaterialMapper materialMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final OutsourceOrderMapper outsourceMapper;

    public DashboardStats stats() {
        DashboardStats s = new DashboardStats();

        // 待办
        s.setPendingSalesApprove(count(salesOrderMapper, "approving"));
        s.setPendingPurchaseApprove(count(purchaseOrderMapper, "approving"));
        s.setPendingProductionApprove(count(productionOrderMapper, "approving"));

        s.setPendingInbound(purchaseOrderMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getApproveStatus, "approved")
                        .ne(PurchaseOrder::getBizStatus, "completed")));
        s.setPendingOutbound(salesOrderMapper.selectCount(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getApproveStatus, "approved")
                        .ne(SalesOrder::getBizStatus, "completed")));

        // 基础数据量
        s.setCustomerCount(customerMapper.selectCount(null));
        s.setSupplierCount(supplierMapper.selectCount(null));
        s.setMaterialCount(materialMapper.selectCount(null));
        s.setProductCount(productMapper.selectCount(null));

        // 低库存预警：物料可用库存 < 安全库存
        List<Material> materials = materialMapper.selectList(
                new LambdaQueryWrapper<Material>().isNotNull(Material::getSafetyStock));
        for (Material m : materials) {
            if (m.getSafetyStock() == null || m.getSafetyStock().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            // 计算该物料可用库存
            BigDecimal available = calcAvailable(m.getId(), "material");
            if (available.compareTo(m.getSafetyStock()) < 0) {
                DashboardStats.LowStockItem item = new DashboardStats.LowStockItem();
                item.setItemId(m.getId());
                item.setItemType("material");
                item.setQuantity(available);
                item.setSafetyStock(m.getSafetyStock());
                item.setItemCode(m.getCode());
                item.setItemName(m.getName());
                s.getLowStockItems().add(item);
            }
        }

        return s;
    }

    /** 驾驶舱：9 KPI + 待办中心 + 图表 */
    public CockpitStats cockpit() {
        CockpitStats c = new CockpitStats();
        BigDecimal zero = BigDecimal.ZERO;

        // 销售订单
        List<SalesOrder> salesOrders = salesOrderMapper.selectList(null);
        BigDecimal salesAmount = zero;
        long salesTotal = salesOrders.size();
        long salesDone = 0;
        Map<String, BigDecimal> monthMap = new HashMap<>();
        for (SalesOrder so : salesOrders) {
            BigDecimal amt = so.getTotalAmount() == null ? zero : so.getTotalAmount();
            salesAmount = salesAmount.add(amt);
            if ("completed".equals(so.getBizStatus())) salesDone++;
            String m = so.getOrderDate() != null
                    ? so.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM")) : "未知";
            monthMap.merge(m, amt, BigDecimal::add);
        }
        c.setSalesOrderAmount(salesAmount);
        c.setSalesOnTimeRate(rate(salesDone, salesTotal));

        // 生产工单
        List<ProductionOrder> pos = productionOrderMapper.selectList(null);
        long poTotal = pos.size();
        long poDone = 0;
        BigDecimal output = zero;
        for (ProductionOrder o : pos) {
            if ("finished".equals(o.getBizStatus()) || "completed".equals(o.getBizStatus())) poDone++;
            output = output.add(o.getCompletedQty() == null ? zero : o.getCompletedQty());
        }
        c.setProductionOnTimeRate(rate(poDone, poTotal));
        c.setProductionOutput(output);

        // 采购订单
        List<PurchaseOrder> purs = purchaseOrderMapper.selectList(null);
        BigDecimal purAmount = zero;
        for (PurchaseOrder o : purs) {
            purAmount = purAmount.add(o.getTotalAmount() == null ? zero : o.getTotalAmount());
        }
        c.setPurchaseOrderAmount(purAmount);
        c.setPayableAmount(purAmount);
        c.setPaidAmount(zero);

        // 库存
        List<Inventory> invs = inventoryMapper.selectList(null);
        BigDecimal matQty = zero, prodQty = zero;
        for (Inventory inv : invs) {
            BigDecimal q = inv.getQuantity() == null ? zero : inv.getQuantity();
            if ("material".equals(inv.getItemType())) matQty = matQty.add(q);
            else if ("product".equals(inv.getItemType())) prodQty = prodQty.add(q);
        }
        c.setMaterialStockAmount(matQty);
        c.setProductStockAmount(prodQty);

        // 库存周转率 = 出库流水量 / 库存量
        List<InventoryTransaction> txs = transactionMapper.selectList(
                new LambdaQueryWrapper<InventoryTransaction>().eq(InventoryTransaction::getDirection, "out"));
        BigDecimal outQty = zero;
        for (InventoryTransaction tx : txs) outQty = outQty.add(tx.getQuantity() == null ? zero : tx.getQuantity());
        BigDecimal totalStock = matQty.add(prodQty);
        c.setInventoryTurnover(totalStock.compareTo(zero) > 0
                ? outQty.divide(totalStock, 2, RoundingMode.HALF_UP) : zero);

        // 待办中心
        c.getTodos().add(todo("待审批销售单", count(salesOrderMapper, "approving"), "/sales/order"));
        c.getTodos().add(todo("待审批采购单", count(purchaseOrderMapper, "approving"), "/purchase/order"));
        c.getTodos().add(todo("待审批生产工单", count(productionOrderMapper, "approving"), "/production/order"));
        c.getTodos().add(todo("待审批外协单", outsourceMapper.selectCount(
                new LambdaQueryWrapper<OutsourceOrder>().eq(OutsourceOrder::getApproveStatus, "approving")), "/outsource/order"));
        c.getTodos().add(todo("待入库采购单", purchaseOrderMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getApproveStatus, "approved")
                        .ne(PurchaseOrder::getBizStatus, "completed")), "/purchase/inbound"));
        c.getTodos().add(todo("待出库销售单", salesOrderMapper.selectCount(
                new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getApproveStatus, "approved")
                        .ne(SalesOrder::getBizStatus, "completed")), "/sales/outbound"));

        // 销售趋势（按月，取最近6个）
        List<String> months = new ArrayList<>(monthMap.keySet());
        months.sort(null);
        List<CockpitStats.TrendItem> trend = new ArrayList<>();
        for (String m : months) {
            CockpitStats.TrendItem t = new CockpitStats.TrendItem();
            t.setMonth(m);
            t.setAmount(monthMap.get(m));
            trend.add(t);
        }
        if (trend.size() > 6) trend = new ArrayList<>(trend.subList(trend.size() - 6, trend.size()));
        c.setSalesTrend(trend);

        // 库存分布
        CockpitStats.StockItem si1 = new CockpitStats.StockItem();
        si1.setName("物料库存"); si1.setQuantity(matQty);
        CockpitStats.StockItem si2 = new CockpitStats.StockItem();
        si2.setName("成品库存"); si2.setQuantity(prodQty);
        c.getStockDistribution().add(si1);
        c.getStockDistribution().add(si2);

        return c;
    }

    private CockpitStats.TodoItem todo(String name, long count, String path) {
        CockpitStats.TodoItem t = new CockpitStats.TodoItem();
        t.setName(name);
        t.setCount(count);
        t.setPath(path);
        return t;
    }

    private BigDecimal rate(long done, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(done * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private long count(Object mapper, String approveStatus) {
        if (mapper == salesOrderMapper) {
            return salesOrderMapper.selectCount(
                    new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getApproveStatus, approveStatus));
        } else if (mapper == purchaseOrderMapper) {
            return purchaseOrderMapper.selectCount(
                    new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getApproveStatus, approveStatus));
        } else {
            return productionOrderMapper.selectCount(
                    new LambdaQueryWrapper<ProductionOrder>().eq(ProductionOrder::getApproveStatus, approveStatus));
        }
    }

    private BigDecimal calcAvailable(Long itemId, String itemType) {
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
}
