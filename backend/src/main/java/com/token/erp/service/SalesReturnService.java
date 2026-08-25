package com.token.erp.service;

import com.token.erp.common.BizException;
import com.token.erp.dto.SalesReturnRequest;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.SalesOrder;
import com.token.erp.mapper.SalesOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 销售退货服务：退货增加库存（入库方向）
 */
@Service
@RequiredArgsConstructor
public class SalesReturnService {

    private final SalesOrderMapper orderMapper;
    private final InventoryService inventoryService;

    @Transactional
    public void create(SalesReturnRequest request) {
        SalesOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "销售订单不存在");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("仅已审批订单可退货");
        if (request.getQuantity() == null || request.getQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BizException("退货数量必须大于0");
        }

        Inventory dim = new Inventory();
        dim.setItemId(request.getProductId());
        dim.setItemType("product");
        dim.setWarehouseId(request.getWarehouseId());
        dim.setStockType("available");
        inventoryService.change(dim, "in", request.getQuantity(), "sales_return", order.getId(), order.getOrderNo());
    }
}
