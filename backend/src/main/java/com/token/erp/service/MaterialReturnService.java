package com.token.erp.service;

import com.token.erp.common.BizException;
import com.token.erp.dto.MaterialReturnRequest;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.MaterialReturn;
import com.token.erp.entity.MaterialReturnItem;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.mapper.MaterialReturnItemMapper;
import com.token.erp.mapper.MaterialReturnMapper;
import com.token.erp.mapper.ProductionOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 生产退料服务：退料增加库存（入库方向）
 */
@Service
@RequiredArgsConstructor
public class MaterialReturnService {

    private final MaterialReturnMapper returnMapper;
    private final MaterialReturnItemMapper returnItemMapper;
    private final ProductionOrderMapper orderMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;

    @Transactional
    public MaterialReturn create(MaterialReturnRequest request) {
        ProductionOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("仅已审批工单可退料");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("退料明细不能为空");

        MaterialReturn ret = new MaterialReturn();
        ret.setReturnNo(codeGenerator.next("TL"));
        ret.setOrderId(request.getOrderId());
        ret.setWarehouseId(request.getWarehouseId());
        ret.setReturnDate(request.getReturnDate() != null ? request.getReturnDate() : LocalDate.now());
        ret.setStatus("completed");
        ret.setRemark(request.getRemark());
        returnMapper.insert(ret);

        for (MaterialReturnItem item : request.getItems()) {
            if (item.getMaterialId() == null) throw new BizException("物料不能为空");

            Inventory dim = new Inventory();
            dim.setItemId(item.getMaterialId());
            dim.setItemType("material");
            dim.setWarehouseId(request.getWarehouseId());
            dim.setLocationId(item.getLocationId());
            dim.setStockType("available");
            inventoryService.change(dim, "in", item.getQuantity(), "return", ret.getId(), ret.getReturnNo());

            item.setId(null);
            item.setReturnId(ret.getId());
            returnItemMapper.insert(item);
        }
        return ret;
    }
}
