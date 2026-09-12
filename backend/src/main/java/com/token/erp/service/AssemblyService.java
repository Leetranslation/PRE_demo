package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.AssemblyRequest;
import com.token.erp.entity.AssemblyOrder;
import com.token.erp.entity.AssemblyOrderItem;
import com.token.erp.entity.Bom;
import com.token.erp.entity.BomItem;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.Product;
import com.token.erp.mapper.AssemblyOrderItemMapper;
import com.token.erp.mapper.AssemblyOrderMapper;
import com.token.erp.mapper.BomItemMapper;
import com.token.erp.mapper.BomMapper;
import com.token.erp.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组装/拆卸：按 BOM 展开子件，成品与子件反向变动库存
 * 组装 assembly：子件出库 + 成品入库
 * 拆卸 disassembly：成品出库 + 子件入库
 */
@Service
@RequiredArgsConstructor
public class AssemblyService {

    private final AssemblyOrderMapper assemblyOrderMapper;
    private final AssemblyOrderItemMapper assemblyOrderItemMapper;
    private final BomMapper bomMapper;
    private final BomItemMapper bomItemMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<AssemblyOrder> page(long page, long pageSize, String type) {
        LambdaQueryWrapper<AssemblyOrder> qw = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) qw.eq(AssemblyOrder::getType, type);
        qw.orderByDesc(AssemblyOrder::getId);
        Page<AssemblyOrder> result = assemblyOrderMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String[]> pm = nameFillUtil.productMap(
                    result.getRecords().stream().map(AssemblyOrder::getProductId).collect(Collectors.toList()));
            Map<Long, String> wm = nameFillUtil.warehouseNameMap(
                    result.getRecords().stream().map(AssemblyOrder::getWarehouseId).collect(Collectors.toList()));
            result.getRecords().forEach(o -> {
                String[] arr = pm.get(o.getProductId());
                if (arr != null) {
                    o.setProductCode(arr[0]);
                    o.setProductName(arr[1]);
                }
                o.setWarehouseName(wm.get(o.getWarehouseId()));
            });
        }
        return result;
    }

    public List<AssemblyOrderItem> items(Long orderId) {
        List<AssemblyOrderItem> list = assemblyOrderItemMapper.selectList(
                new LambdaQueryWrapper<AssemblyOrderItem>().eq(AssemblyOrderItem::getOrderId, orderId));
        if (!list.isEmpty()) {
            Map<Long, String[]> mm = nameFillUtil.materialMap(
                    list.stream().map(AssemblyOrderItem::getMaterialId).collect(Collectors.toList()));
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
    public AssemblyOrder create(AssemblyRequest req) {
        String type = req.getType() == null ? "assembly" : req.getType();
        if (!"assembly".equals(type) && !"disassembly".equals(type))
            throw new BizException("类型必须为 assembly 或 disassembly");
        if (req.getProductId() == null) throw new BizException("请选择产品");
        if (req.getWarehouseId() == null) throw new BizException("请选择仓库");
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
            throw new BizException("数量必须大于0");

        Product product = productMapper.selectById(req.getProductId());
        if (product == null) throw new BizException("产品不存在");

        // 查 BOM 子件清单
        Long bomId = req.getBomId();
        if (bomId == null) {
            // 未指定 BOM 时取该产品的第一个 BOM
            Bom b = bomMapper.selectOne(new LambdaQueryWrapper<Bom>()
                    .eq(Bom::getProductId, req.getProductId()).orderByDesc(Bom::getId).last("limit 1"));
            if (b != null) bomId = b.getId();
        }
        List<BomItem> bomItems = bomId == null ? List.of()
                : bomItemMapper.selectList(new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, bomId));
        if (bomItems.isEmpty()) throw new BizException("该产品无 BOM 子件，无法组装/拆卸");

        AssemblyOrder order = new AssemblyOrder();
        order.setOrderNo(codeGenerator.next("assembly".equals(type) ? "ZZ" : "CX"));
        order.setType(type);
        order.setProductId(req.getProductId());
        order.setBomId(bomId);
        order.setQuantity(req.getQuantity());
        order.setWarehouseId(req.getWarehouseId());
        order.setBizDate(req.getBizDate() != null ? req.getBizDate() : LocalDate.now());
        order.setStatus("completed");
        order.setRemark(req.getRemark());
        assemblyOrderMapper.insert(order);

        // 成品维度
        Inventory prod = new Inventory();
        prod.setItemId(req.getProductId());
        prod.setItemType("product");
        prod.setWarehouseId(req.getWarehouseId());
        prod.setStockType("available");
        // 成品方向：组装 in / 拆卸 out
        inventoryService.change(prod, "assembly".equals(type) ? "in" : "out", req.getQuantity(),
                "assembly", order.getId(), order.getOrderNo());

        // 子件方向：组装 out / 拆卸 in
        for (BomItem bi : bomItems) {
            BigDecimal qty = bi.getQuantity().multiply(req.getQuantity());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
            Inventory child = new Inventory();
            child.setItemId(bi.getChildId());
            child.setItemType(bi.getChildType() == null ? "material" : bi.getChildType());
            child.setWarehouseId(req.getWarehouseId());
            child.setStockType("available");
            inventoryService.change(child, "assembly".equals(type) ? "out" : "in", qty,
                    "assembly", order.getId(), order.getOrderNo());

            AssemblyOrderItem oi = new AssemblyOrderItem();
            oi.setOrderId(order.getId());
            oi.setMaterialId(bi.getChildId());
            oi.setQuantity(qty);
            oi.setUnit(bi.getUnit());
            assemblyOrderItemMapper.insert(oi);
        }
        return order;
    }
}
