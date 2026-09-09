package com.token.erp.service;

import com.token.erp.common.BizException;
import com.token.erp.dto.ProductionOrderRequest;
import com.token.erp.dto.PurchaseRequisitionRequest;
import com.token.erp.entity.Material;
import com.token.erp.entity.Product;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.entity.PurchaseRequisition;
import com.token.erp.mapper.MaterialMapper;
import com.token.erp.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 库存预警联动：物料预警生成采购申请单，产品预警生成生产工单
 */
@Service
@RequiredArgsConstructor
public class WarningActionService {

    private final InventoryService inventoryService;
    private final PurchaseRequisitionService purchaseRequisitionService;
    private final ProductionOrderService productionOrderService;
    private final MaterialMapper materialMapper;
    private final ProductMapper productMapper;

    /** 物料预警 → 生成采购申请单（补到最低/安全库存） */
    public PurchaseRequisition generatePurchase(Long itemId) {
        Material m = materialMapper.selectById(itemId);
        if (m == null) throw new BizException(404, "物料不存在");
        BigDecimal target = m.getMinStock() != null ? m.getMinStock() : m.getSafetyStock();
        if (target == null) throw new BizException("该物料未设置最低/安全库存，无法计算补货量");

        BigDecimal available = inventoryService.availableQty(itemId, "material");
        BigDecimal qty = target.subtract(available);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("库存已充足，无需补货");

        PurchaseRequisitionRequest req = new PurchaseRequisitionRequest();
        PurchaseRequisitionRequest.Item item = new PurchaseRequisitionRequest.Item();
        item.setMaterialId(itemId);
        item.setSpec(m.getSpec());
        item.setUnit(m.getUnit());
        item.setQuantity(qty);
        int leadDays = m.getPurchaseLeadDays() == null ? 7 : m.getPurchaseLeadDays();
        item.setRequiredDate(LocalDate.now().plusDays(leadDays));
        req.setItems(List.of(item));
        req.setRemark("库存预警自动生成补货申请");
        return purchaseRequisitionService.create(req);
    }

    /** 产品预警 → 生成生产工单（补到最低/安全库存） */
    public ProductionOrder generateWorkOrder(Long itemId) {
        Product p = productMapper.selectById(itemId);
        if (p == null) throw new BizException(404, "产品不存在");
        BigDecimal target = p.getMinStock() != null ? p.getMinStock() : p.getSafetyStock();
        if (target == null) throw new BizException("该产品未设置最低/安全库存，无法计算补货量");

        BigDecimal available = inventoryService.availableQty(itemId, "product");
        BigDecimal qty = target.subtract(available);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("库存已充足，无需补货");

        ProductionOrderRequest req = new ProductionOrderRequest();
        req.setProductId(itemId);
        req.setQuantity(qty);
        req.setPlanDate(LocalDate.now());
        req.setRemark("库存预警自动生成生产工单");
        return productionOrderService.create(req);
    }
}
