package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PurchaseReturnRequest;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.PurchaseOrder;
import com.token.erp.entity.PurchaseReturn;
import com.token.erp.entity.PurchaseReturnItem;
import com.token.erp.mapper.PurchaseOrderMapper;
import com.token.erp.mapper.PurchaseReturnItemMapper;
import com.token.erp.mapper.PurchaseReturnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购退货服务：退回供应商，库存【出库】(out)
 */
@Service
@RequiredArgsConstructor
public class PurchaseReturnService {

    private final PurchaseReturnMapper returnMapper;
    private final PurchaseReturnItemMapper returnItemMapper;
    private final PurchaseOrderMapper orderMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final IdempotencyService idempotencyService;
    private final NameFillUtil nameFillUtil;

    public Page<PurchaseReturn> page(long page, long pageSize, Long orderId) {
        LambdaQueryWrapper<PurchaseReturn> qw = new LambdaQueryWrapper<>();
        if (orderId != null) qw.eq(PurchaseReturn::getOrderId, orderId);
        qw.orderByDesc(PurchaseReturn::getId);
        Page<PurchaseReturn> result = returnMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> om = nameFillUtil.purchaseOrderNoMap(
                    result.getRecords().stream().map(PurchaseReturn::getOrderId).collect(Collectors.toList()));
            result.getRecords().forEach(r -> r.setSourceOrderNo(om.get(r.getOrderId())));
        }
        return result;
    }

    public List<PurchaseReturnItem> items(Long returnId) {
        List<PurchaseReturnItem> list = returnItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseReturnItem>().eq(PurchaseReturnItem::getReturnId, returnId));
        if (!list.isEmpty()) {
            Map<Long, String[]> mm = nameFillUtil.materialMap(
                    list.stream().map(PurchaseReturnItem::getMaterialId).collect(Collectors.toList()));
            list.forEach(i -> {
                String[] arr = mm.get(i.getMaterialId());
                if (arr != null) {
                    i.setMaterialCode(arr[0]);
                    i.setMaterialName(arr[1]);
                }
            });
        }
        return list;
    }

    @Transactional
    public PurchaseReturn create(PurchaseReturnRequest request) {
        idempotencyService.checkAndOccupy(request.getOrderId() + "_" + System.currentTimeMillis(), "purchase_return");
        PurchaseOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "采购订单不存在");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("退货明细不能为空");

        PurchaseReturn pr = new PurchaseReturn();
        pr.setReturnNo(codeGenerator.next("TH"));
        pr.setOrderId(request.getOrderId());
        pr.setSupplierId(order.getSupplierId());
        pr.setWarehouseId(request.getWarehouseId());
        pr.setReturnDate(LocalDate.now());
        pr.setStatus("completed");
        pr.setRemark(request.getRemark());
        returnMapper.insert(pr);

        for (PurchaseReturnRequest.Item item : request.getItems()) {
            if (item.getMaterialId() == null) throw new BizException("物料不能为空");
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("退货数量必须大于0");
            }

            // 采购退货 = 库存出库（out）
            Inventory dim = new Inventory();
            dim.setItemId(item.getMaterialId());
            dim.setItemType("material");
            dim.setWarehouseId(request.getWarehouseId());
            dim.setStockType("available");
            inventoryService.change(dim, "out", item.getQuantity(), "purchase_return", pr.getId(), pr.getReturnNo());

            PurchaseReturnItem ri = new PurchaseReturnItem();
            ri.setReturnId(pr.getId());
            ri.setMaterialId(item.getMaterialId());
            ri.setQuantity(item.getQuantity());
            ri.setUnit(item.getUnit());
            ri.setRemark(item.getRemark());
            returnItemMapper.insert(ri);
        }
        return pr;
    }
}
