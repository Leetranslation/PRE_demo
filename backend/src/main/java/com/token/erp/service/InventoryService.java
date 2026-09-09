package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.InventoryWarningVO;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.InventoryReservation;
import com.token.erp.entity.InventoryTransaction;
import com.token.erp.entity.Material;
import com.token.erp.entity.Product;
import com.token.erp.mapper.InventoryMapper;
import com.token.erp.mapper.InventoryReservationMapper;
import com.token.erp.mapper.InventoryTransactionMapper;
import com.token.erp.mapper.MaterialMapper;
import com.token.erp.mapper.ProductMapper;
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
import java.util.ArrayList;
import java.util.List;

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
    private final InventoryReservationMapper reservationMapper;
    private final MaterialMapper materialMapper;
    private final ProductMapper productMapper;
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
    public Page<InventoryTransaction> txPage(long page, long pageSize, Long itemId, String itemType, String bizType) {
        LambdaQueryWrapper<InventoryTransaction> qw = new LambdaQueryWrapper<>();
        if (itemId != null) qw.eq(InventoryTransaction::getItemId, itemId);
        if (itemType != null && !itemType.isEmpty()) qw.eq(InventoryTransaction::getItemType, itemType);
        if (bizType != null && !bizType.isEmpty()) qw.eq(InventoryTransaction::getBizType, bizType);
        qw.orderByDesc(InventoryTransaction::getId);
        return transactionMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 库存预警：查低于最低库存/安全库存的物料和产品 */
    public List<InventoryWarningVO> warningList() {
        List<InventoryWarningVO> result = new ArrayList<>();
        // 物料预警
        List<Material> materials = materialMapper.selectList(
                new LambdaQueryWrapper<Material>()
                        .eq(Material::getStatus, 1)
                        .eq(Material::getInventoryManage, 1));
        for (Material m : materials) {
            if (m.getMinStock() == null && m.getSafetyStock() == null) continue;
            BigDecimal available = availableQty(m.getId(), "material");
            InventoryWarningVO vo = buildWarning(m.getId(), "material", m.getCode(), m.getName(),
                    available, m.getMinStock(), m.getSafetyStock());
            if (vo != null) result.add(vo);
        }
        // 产品预警
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .eq(Product::getInventoryManage, 1));
        for (Product p : products) {
            if (p.getMinStock() == null && p.getSafetyStock() == null) continue;
            BigDecimal available = availableQty(p.getId(), "product");
            InventoryWarningVO vo = buildWarning(p.getId(), "product", p.getCode(), p.getName(),
                    available, p.getMinStock(), p.getSafetyStock());
            if (vo != null) result.add(vo);
        }
        return result;
    }

    private InventoryWarningVO buildWarning(Long itemId, String itemType, String code, String name,
                                            BigDecimal available, BigDecimal minStock, BigDecimal safetyStock) {
        String level = null;
        if (minStock != null && available.compareTo(minStock) < 0) {
            level = "critical";
        } else if (safetyStock != null && available.compareTo(safetyStock) < 0) {
            level = "warning";
        }
        if (level == null) return null;
        InventoryWarningVO vo = new InventoryWarningVO();
        vo.setItemId(itemId);
        vo.setItemType(itemType);
        vo.setCode(code);
        vo.setName(name);
        vo.setAvailableQty(available);
        vo.setMinStock(minStock);
        vo.setSafetyStock(safetyStock);
        vo.setWarningLevel(level);
        return vo;
    }

    /** 某物料/产品的可用库存合计（跨仓库/库位/批次） */
    public BigDecimal availableQty(Long itemId, String itemType) {
        List<Inventory> list = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getItemId, itemId)
                        .eq(Inventory::getItemType, itemType)
                        .eq(Inventory::getStockType, "available"));
        BigDecimal total = BigDecimal.ZERO;
        for (Inventory inv : list) {
            BigDecimal qty = inv.getQuantity() == null ? BigDecimal.ZERO : inv.getQuantity();
            BigDecimal reserved = inv.getReservedQty() == null ? BigDecimal.ZERO : inv.getReservedQty();
            total = total.add(qty.subtract(reserved));
        }
        return total;
    }

    /** 批号跟踪：按批号查流水 */
    public Page<InventoryTransaction> batchTrack(long page, long pageSize, String batchNo) {
        LambdaQueryWrapper<InventoryTransaction> qw = new LambdaQueryWrapper<>();
        if (batchNo != null && !batchNo.isEmpty()) qw.eq(InventoryTransaction::getBatchNo, batchNo);
        qw.orderByAsc(InventoryTransaction::getId);
        return transactionMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 序列号跟踪：按序列号查流水 */
    public Page<InventoryTransaction> serialTrack(long page, long pageSize, String serialNo) {
        LambdaQueryWrapper<InventoryTransaction> qw = new LambdaQueryWrapper<>();
        if (serialNo != null && !serialNo.isEmpty()) qw.eq(InventoryTransaction::getSerialNo, serialNo);
        qw.orderByAsc(InventoryTransaction::getId);
        return transactionMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 锁库：占用可用库存（增加 reservedQty），创建锁库记录 */
    @Transactional
    public InventoryReservation reserve(Inventory dim, BigDecimal quantity,
                                        String bizType, Long bizId, String bizNo) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("锁定数量必须大于0");
        }
        normalize(dim);
        String lockKey = buildLockKey(dim);
        if (!tryLock(lockKey)) throw new BizException(409, "库存操作繁忙，请稍后重试");
        try {
            Inventory inv = inventoryMapper.selectForUpdate(
                    dim.getItemId(), dim.getItemType(), dim.getWarehouseId(), dim.getStockType(),
                    dim.getLocationId(), dim.getBatchNo(), dim.getSerialNo());
            if (inv == null) inv = create(dim);
            BigDecimal reserved = inv.getReservedQty() == null ? BigDecimal.ZERO : inv.getReservedQty();
            BigDecimal available = inv.getQuantity().subtract(reserved);
            if (quantity.compareTo(available) > 0) {
                throw new BizException("可锁定数量不足，当前可锁定：" + available);
            }
            inv.setReservedQty(reserved.add(quantity));
            inventoryMapper.updateById(inv);

            InventoryReservation r = new InventoryReservation();
            r.setInventoryId(inv.getId());
            r.setBizType(bizType);
            r.setBizId(bizId != null ? bizId : 0L);
            r.setBizNo(bizNo);
            r.setQuantity(quantity);
            r.setStatus("locked");
            r.setCreateTime(LocalDateTime.now());
            reservationMapper.insert(r);
            return r;
        } finally {
            releaseLockAfterCommit(lockKey);
        }
    }

    /** 解锁：释放锁库（减少 reservedQty） */
    @Transactional
    public void release(Long reservationId) {
        InventoryReservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new BizException(404, "锁库记录不存在");
        if (!"locked".equals(r.getStatus())) throw new BizException("该锁库记录已释放");

        Inventory inv = inventoryMapper.selectById(r.getInventoryId());
        if (inv != null) {
            BigDecimal reserved = inv.getReservedQty() == null ? BigDecimal.ZERO : inv.getReservedQty();
            BigDecimal remain = reserved.subtract(r.getQuantity());
            if (remain.compareTo(BigDecimal.ZERO) < 0) remain = BigDecimal.ZERO;
            inv.setReservedQty(remain);
            inventoryMapper.updateById(inv);
        }
        r.setStatus("released");
        r.setReleaseTime(LocalDateTime.now());
        reservationMapper.updateById(r);
    }

    /** 锁库记录列表 */
    public Page<InventoryReservation> reservationPage(long page, long pageSize, String status) {
        LambdaQueryWrapper<InventoryReservation> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq(InventoryReservation::getStatus, status);
        qw.orderByDesc(InventoryReservation::getId);
        return reservationMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 事务提交后释放分布式锁 */
    private void releaseLockAfterCommit(String lockKey) {
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
