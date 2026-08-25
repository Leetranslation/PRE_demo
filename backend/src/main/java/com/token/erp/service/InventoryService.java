package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.InventoryTransaction;
import com.token.erp.mapper.InventoryMapper;
import com.token.erp.mapper.InventoryTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 库存核心服务
 * 所有库存数量变化必须经过此服务，保证台账+流水事务一致
 *
 * 并发安全策略（对应文档 20.2）：
 * 1. 已存在库存行：SELECT ... FOR UPDATE 行级悲观锁，串行化同维度并发操作
 * 2. 库存行不存在：Redis 分布式锁按维度串行化，配合唯一索引兜底，避免并发双插入
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final StringRedisTemplate redisTemplate;

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
        // 规范化可空维度：与数据库默认值对齐（location=0, batch/serial=''）
        normalize(dim);

        // 分布式锁：按库存维度串行化，防止并发双插入/丢失更新
        String lockKey = buildLockKey(dim);
        if (!tryLock(lockKey)) {
            throw new BizException(409, "库存操作繁忙，请稍后重试");
        }
        try {
            return doChange(dim, direction, quantity, bizType, bizId, bizNo);
        } finally {
            // 在事务提交后释放锁，避免其他线程读到未提交数据
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                redisTemplate.delete(lockKey);
                            }
                        });
            } else {
                redisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 尝试获取分布式锁：短暂自旋等待，最多 2 秒，每 50ms 重试
     * 让并发请求自动排队，而非立即失败
     */
    private boolean tryLock(String lockKey) {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
            if (Boolean.TRUE.equals(locked)) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private Inventory doChange(Inventory dim, String direction, BigDecimal quantity,
                               String bizType, Long bizId, String bizNo) {
        // 加锁查询已存在的行
        Inventory inv = inventoryMapper.selectForUpdate(
                dim.getItemId(), dim.getItemType(), dim.getWarehouseId(), dim.getStockType(),
                dim.getLocationId(), dim.getBatchNo(), dim.getSerialNo());

        if (inv == null) {
            inv = create(dim);
        }

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
     * 创建库存行（在分布式锁保护下，配合唯一索引兜底）
     */
    private Inventory create(Inventory dim) {
        Inventory newInv = new Inventory();
        newInv.setItemId(dim.getItemId());
        newInv.setItemType(dim.getItemType());
        newInv.setWarehouseId(dim.getWarehouseId());
        newInv.setLocationId(dim.getLocationId());
        newInv.setBatchNo(dim.getBatchNo());
        newInv.setSerialNo(dim.getSerialNo());
        newInv.setStockType(dim.getStockType());
        newInv.setQuantity(BigDecimal.ZERO);
        newInv.setReservedQty(BigDecimal.ZERO);
        newInv.setCreateTime(LocalDateTime.now());
        newInv.setUpdateTime(LocalDateTime.now());
        try {
            inventoryMapper.insert(newInv);
            return newInv;
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发插入冲突，重新加锁查询
            Inventory retry = inventoryMapper.selectForUpdate(
                    dim.getItemId(), dim.getItemType(), dim.getWarehouseId(), dim.getStockType(),
                    dim.getLocationId(), dim.getBatchNo(), dim.getSerialNo());
            if (retry == null) {
                throw new BizException("库存行创建失败");
            }
            return retry;
        }
    }

    /** 规范化可空维度字段，与数据库默认值一致 */
    private void normalize(Inventory dim) {
        if (dim.getLocationId() == null) dim.setLocationId(0L);
        if (dim.getBatchNo() == null) dim.setBatchNo("");
        if (dim.getSerialNo() == null) dim.setSerialNo("");
    }

    /** 构建按维度加锁的 key */
    private String buildLockKey(Inventory dim) {
        return "inventory:lock:" + dim.getItemType() + ":" + dim.getItemId()
                + ":" + dim.getWarehouseId() + ":" + dim.getStockType()
                + ":" + dim.getLocationId()
                + ":" + dim.getBatchNo()
                + ":" + dim.getSerialNo();
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
