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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final IdempotencyService idempotencyService;
    private final NameFillUtil nameFillUtil;

    /** 退料单列表 */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<MaterialReturn> page(
            long page, long pageSize, Long orderId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MaterialReturn> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (orderId != null) qw.eq(MaterialReturn::getOrderId, orderId);
        qw.orderByDesc(MaterialReturn::getId);
        return returnMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize), qw);
    }

    /** 退料单明细 */
    public List<MaterialReturnItem> items(Long returnId) {
        List<MaterialReturnItem> list = returnItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MaterialReturnItem>()
                        .eq(MaterialReturnItem::getReturnId, returnId));
        if (!list.isEmpty()) {
            Map<Long, String[]> mm = nameFillUtil.materialMap(
                    list.stream().map(MaterialReturnItem::getMaterialId).collect(Collectors.toList()));
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
    public MaterialReturn create(MaterialReturnRequest request) {
        // 幂等控制：防止重复点击/网络重试导致重复退料
        idempotencyService.checkAndOccupy(request.getRequestId(), "material_return");
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
