package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.InventoryTransaction;
import com.token.erp.mapper.InventoryMapper;
import com.token.erp.mapper.InventoryTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存核心服务
 * 所有库存数量变化必须经过此服务，保证台账+流水事务一致
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper transactionMapper;

    /**
     * 库存变动（统一入口）
     * @param dim 库存维度
     * @param direction in入库 / out出库
     * @param quantity 变动数量（正数）
     * @param bizType 业务类型
     * @param bizId 业务单据ID
     * @param bizNo 业务单据编号
     */
    @Transactional
    public Inventory change(Inventory dim, String direction, BigDecimal quantity,
                            String bizType, Long bizId, String bizNo) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("库存变动数量必须大于0");
        }

        // 锁定库存行（按维度查找或创建）
        Inventory inv = findOrCreate(dim);

        if ("in".equals(direction)) {
            inv.setQuantity(inv.getQuantity().add(quantity));
        } else if ("out".equals(direction)) {
            BigDecimal available = inv.getQuantity().subtract(
                    inv.getReservedQty() == null ? BigDecimal.ZERO : inv.getReservedQty());
            if (quantity.compareTo(available) > 0) {
                throw new BizException("库存不足，当前可用：" + available);
            }
            inv.setQuantity(inv.getQuantity().subtract(quantity));
        } else {
            throw new BizException("非法库存方向：" + direction);
        }
        inventoryMapper.updateById(inv);

        // 记录流水
        InventoryTransaction tx = new InventoryTransaction();
        tx.setItemId(inv.getItemId());
        tx.setItemType(inv.getItemType());
        tx.setWarehouseId(inv.getWarehouseId());
        tx.setLocationId(inv.getLocationId());
        tx.setBatchNo(inv.getBatchNo());
        tx.setSerialNo(inv.getSerialNo());
        tx.setStockType(inv.getStockType());
        tx.setDirection(direction);
        tx.setQuantity(quantity);
        tx.setBalanceQty(inv.getQuantity());
        tx.setBizType(bizType);
        tx.setBizId(bizId);
        tx.setBizNo(bizNo);
        tx.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tx);

        return inv;
    }

    /**
     * 按维度查找库存行，不存在则创建
     */
    private Inventory findOrCreate(Inventory dim) {
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getItemId, dim.getItemId())
                .eq(Inventory::getItemType, dim.getItemType())
                .eq(Inventory::getWarehouseId, dim.getWarehouseId())
                .eq(Inventory::getStockType, dim.getStockType());
        // 库位、批次、序列号可能为 null
        if (dim.getLocationId() != null) {
            qw.eq(Inventory::getLocationId, dim.getLocationId());
        } else {
            qw.isNull(Inventory::getLocationId);
        }
        if (dim.getBatchNo() != null) {
            qw.eq(Inventory::getBatchNo, dim.getBatchNo());
        } else {
            qw.isNull(Inventory::getBatchNo);
        }
        if (dim.getSerialNo() != null) {
            qw.eq(Inventory::getSerialNo, dim.getSerialNo());
        } else {
            qw.isNull(Inventory::getSerialNo);
        }

        Inventory inv = inventoryMapper.selectOne(qw);
        if (inv == null) {
            inv = new Inventory();
            inv.setItemId(dim.getItemId());
            inv.setItemType(dim.getItemType());
            inv.setWarehouseId(dim.getWarehouseId());
            inv.setLocationId(dim.getLocationId());
            inv.setBatchNo(dim.getBatchNo());
            inv.setSerialNo(dim.getSerialNo());
            inv.setStockType(dim.getStockType());
            inv.setQuantity(BigDecimal.ZERO);
            inv.setReservedQty(BigDecimal.ZERO);
            inv.setCreateTime(LocalDateTime.now());
            inv.setUpdateTime(LocalDateTime.now());
            inventoryMapper.insert(inv);
        }
        return inv;
    }

    /** 库存查询 */
    public Page<Inventory> page(long page, long pageSize, Long itemId, String itemType,
                                Long warehouseId, String stockType, String keyword) {
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<>();
        if (itemId != null) qw.eq(Inventory::getItemId, itemId);
        if (itemType != null && !itemType.isEmpty()) qw.eq(Inventory::getItemType, itemType);
        if (warehouseId != null) qw.eq(Inventory::getWarehouseId, warehouseId);
        if (stockType != null && !stockType.isEmpty()) qw.eq(Inventory::getStockType, stockType);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Inventory::getBatchNo, keyword)
                    .or().like(Inventory::getSerialNo, keyword));
        }
        qw.orderByDesc(Inventory::getId);
        return inventoryMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 库存流水查询 */
    public Page<InventoryTransaction> txPage(long page, long pageSize, Long itemId, String bizType) {
        LambdaQueryWrapper<InventoryTransaction> qw = new LambdaQueryWrapper<>();
        if (itemId != null) qw.eq(InventoryTransaction::getItemId, itemId);
        if (bizType != null && !bizType.isEmpty()) qw.eq(InventoryTransaction::getBizType, bizType);
        qw.orderByDesc(InventoryTransaction::getId);
        return transactionMapper.selectPage(new Page<>(page, pageSize), qw);
    }
}
