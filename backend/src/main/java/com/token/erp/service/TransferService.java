package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.TransferRequest;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.Transfer;
import com.token.erp.entity.TransferItem;
import com.token.erp.mapper.TransferItemMapper;
import com.token.erp.mapper.TransferMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存调拨：源仓库出库 + 目标仓库入库（同物调仓）
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferMapper transferMapper;
    private final TransferItemMapper transferItemMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final SecurityUtils securityUtils;
    private final NameFillUtil nameFillUtil;

    public Page<Transfer> page(long page, long pageSize, Long sourceWarehouseId, Long targetWarehouseId) {
        LambdaQueryWrapper<Transfer> qw = new LambdaQueryWrapper<>();
        if (sourceWarehouseId != null) qw.eq(Transfer::getSourceWarehouseId, sourceWarehouseId);
        if (targetWarehouseId != null) qw.eq(Transfer::getTargetWarehouseId, targetWarehouseId);
        qw.orderByDesc(Transfer::getId);
        Page<Transfer> result = transferMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> wm = nameFillUtil.warehouseNameMap(
                    result.getRecords().stream().flatMap(t -> java.util.stream.Stream.of(t.getSourceWarehouseId(), t.getTargetWarehouseId()))
                            .collect(Collectors.toList()));
            result.getRecords().forEach(t -> {
                t.setSourceWarehouseName(wm.get(t.getSourceWarehouseId()));
                t.setTargetWarehouseName(wm.get(t.getTargetWarehouseId()));
            });
        }
        return result;
    }

    public List<TransferItem> items(Long transferId) {
        List<TransferItem> list = transferItemMapper.selectList(
                new LambdaQueryWrapper<TransferItem>().eq(TransferItem::getTransferId, transferId));
        if (!list.isEmpty()) {
            List<Long> pids = list.stream().filter(i -> "product".equals(i.getItemType()))
                    .map(TransferItem::getItemId).collect(Collectors.toList());
            List<Long> mids = list.stream().filter(i -> !"product".equals(i.getItemType()))
                    .map(TransferItem::getItemId).collect(Collectors.toList());
            Map<Long, String[]> pm = nameFillUtil.productMap(pids);
            Map<Long, String[]> mm = nameFillUtil.materialMap(mids);
            list.forEach(i -> {
                String[] arr = "product".equals(i.getItemType()) ? pm.get(i.getItemId()) : mm.get(i.getItemId());
                if (arr != null) {
                    i.setItemCode(arr[0]);
                    i.setItemName(arr[1]);
                }
            });
        }
        return list;
    }

    /** 创建调拨单并执行库存变动（源出 + 目标入） */
    @Transactional
    public Transfer create(TransferRequest req) {
        if (req.getSourceWarehouseId() == null || req.getTargetWarehouseId() == null)
            throw new BizException("源仓库和目标仓库不能为空");
        if (req.getSourceWarehouseId().equals(req.getTargetWarehouseId()))
            throw new BizException("源仓库和目标仓库不能相同");
        if (req.getItems() == null || req.getItems().isEmpty()) throw new BizException("调拨明细不能为空");

        Transfer t = new Transfer();
        t.setTransferNo(codeGenerator.next("DB"));
        t.setSourceWarehouseId(req.getSourceWarehouseId());
        t.setTargetWarehouseId(req.getTargetWarehouseId());
        t.setTransferDate(req.getTransferDate() != null ? req.getTransferDate() : LocalDate.now());
        t.setStatus("completed");
        t.setRemark(req.getRemark());
        t.setCreateBy(securityUtils.currentUsername());
        t.setCreateTime(LocalDateTime.now());
        transferMapper.insert(t);

        for (TransferRequest.Item item : req.getItems()) {
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                throw new BizException("调拨数量必须大于0");
            Inventory src = new Inventory();
            src.setItemId(item.getItemId());
            src.setItemType(item.getItemType());
            src.setWarehouseId(req.getSourceWarehouseId());
            src.setStockType("available");
            inventoryService.change(src, "out", item.getQuantity(), "transfer", t.getId(), t.getTransferNo());

            Inventory dst = new Inventory();
            dst.setItemId(item.getItemId());
            dst.setItemType(item.getItemType());
            dst.setWarehouseId(req.getTargetWarehouseId());
            dst.setStockType("available");
            inventoryService.change(dst, "in", item.getQuantity(), "transfer", t.getId(), t.getTransferNo());

            TransferItem ti = new TransferItem();
            ti.setTransferId(t.getId());
            ti.setItemId(item.getItemId());
            ti.setItemType(item.getItemType());
            ti.setQuantity(item.getQuantity());
            ti.setUnit(item.getUnit());
            transferItemMapper.insert(ti);
        }
        return t;
    }
}
