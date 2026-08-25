package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.dto.DashboardStats;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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
                s.getLowStockItems().add(item);
            }
        }

        return s;
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
