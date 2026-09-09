package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.StockAdjustment;
import com.token.erp.entity.Stocktake;
import com.token.erp.entity.StocktakeItem;
import com.token.erp.mapper.InventoryMapper;
import com.token.erp.mapper.StockAdjustmentMapper;
import com.token.erp.mapper.StocktakeItemMapper;
import com.token.erp.mapper.StocktakeMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存盘点：盘点方案 → 录实际数 → 差异生成调整（盘盈盘亏）→ 调库存
 */
@Service
@RequiredArgsConstructor
public class StocktakeService {

    private final StocktakeMapper stocktakeMapper;
    private final StocktakeItemMapper stocktakeItemMapper;
    private final StockAdjustmentMapper stockAdjustmentMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final SecurityUtils securityUtils;

    public Page<Stocktake> page(long page, long pageSize, Long warehouseId, String status) {
        LambdaQueryWrapper<Stocktake> qw = new LambdaQueryWrapper<>();
        if (warehouseId != null) qw.eq(Stocktake::getWarehouseId, warehouseId);
        if (status != null && !status.isEmpty()) qw.eq(Stocktake::getStatus, status);
        qw.orderByDesc(Stocktake::getId);
        return stocktakeMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public List<StocktakeItem> items(Long stocktakeId) {
        return stocktakeItemMapper.selectList(
                new LambdaQueryWrapper<StocktakeItem>().eq(StocktakeItem::getStocktakeId, stocktakeId));
    }

    /** 创建盘点方案：自动带入该仓库全部可用库存为账面数 */
    @Transactional
    public Stocktake create(Long warehouseId, String remark) {
        if (warehouseId == null) throw new BizException("请选择仓库");
        List<Inventory> invs = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getWarehouseId, warehouseId)
                        .eq(Inventory::getStockType, "available"));
        Stocktake s = new Stocktake();
        s.setStocktakeNo(codeGenerator.next("PD"));
        s.setWarehouseId(warehouseId);
        s.setStocktakeDate(LocalDate.now());
        s.setStatus("created");
        s.setApproveStatus("pending");
        s.setRemark(remark);
        s.setCreateBy(securityUtils.currentUsername());
        s.setCreateTime(LocalDateTime.now());
        stocktakeMapper.insert(s);
        for (Inventory inv : invs) {
            StocktakeItem it = new StocktakeItem();
            it.setStocktakeId(s.getId());
            it.setItemId(inv.getItemId());
            it.setItemType(inv.getItemType());
            it.setLocationId(inv.getLocationId());
            it.setBatchNo(inv.getBatchNo());
            it.setBookQty(inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity());
            it.setActualQty(inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity());
            it.setDiffQty(BigDecimal.ZERO);
            stocktakeItemMapper.insert(it);
        }
        return s;
    }

    /** 录入实际盘点数（items 只需 id + actualQty），自动算差异 */
    @Transactional
    public void count(List<StocktakeItem> items) {
        if (items == null || items.isEmpty()) throw new BizException("盘点明细不能为空");
        Long stocktakeId = null;
        for (StocktakeItem in : items) {
            StocktakeItem db = stocktakeItemMapper.selectById(in.getId());
            if (db == null) throw new BizException("盘点明细不存在");
            if (in.getActualQty() == null) throw new BizException("实际数量不能为空");
            if (stocktakeId == null) stocktakeId = db.getStocktakeId();
            db.setActualQty(in.getActualQty());
            db.setDiffQty(in.getActualQty().subtract(db.getBookQty()));
            stocktakeItemMapper.updateById(db);
        }
        Stocktake s = stocktakeMapper.selectById(stocktakeId);
        if (s != null && "created".equals(s.getStatus())) {
            s.setStatus("counted");
            stocktakeMapper.updateById(s);
        }
    }

    /** 差异生成调整：盘盈入库 / 盘亏出库，写调整单 */
    @Transactional
    public void adjust(Long stocktakeId) {
        Stocktake s = stocktakeMapper.selectById(stocktakeId);
        if (s == null) throw new BizException("盘点单不存在");
        if ("adjusted".equals(s.getStatus())) throw new BizException("已调整，不能重复调整");
        List<StocktakeItem> items = items(stocktakeId);
        for (StocktakeItem it : items) {
            BigDecimal diff = it.getDiffQty() == null ? BigDecimal.ZERO : it.getDiffQty();
            if (diff.compareTo(BigDecimal.ZERO) == 0) continue;
            String direction = diff.compareTo(BigDecimal.ZERO) > 0 ? "in" : "out";
            BigDecimal qty = diff.abs();
            Inventory dim = new Inventory();
            dim.setItemId(it.getItemId());
            dim.setItemType(it.getItemType());
            dim.setWarehouseId(s.getWarehouseId());
            dim.setLocationId(it.getLocationId());
            dim.setBatchNo(it.getBatchNo());
            dim.setStockType("available");
            inventoryService.change(dim, direction, qty, "stocktake", stocktakeId, s.getStocktakeNo());

            StockAdjustment adj = new StockAdjustment();
            adj.setAdjustNo(codeGenerator.next("TZ"));
            adj.setItemId(it.getItemId());
            adj.setItemType(it.getItemType());
            adj.setWarehouseId(s.getWarehouseId());
            adj.setLocationId(it.getLocationId());
            adj.setAdjustQty(diff);
            adj.setReason("盘点差异调整");
            adj.setSourceType("stocktake");
            adj.setSourceId(stocktakeId);
            adj.setCreateBy(securityUtils.currentUsername());
            adj.setCreateTime(LocalDateTime.now());
            stockAdjustmentMapper.insert(adj);
        }
        s.setStatus("adjusted");
        stocktakeMapper.updateById(s);
    }
}
