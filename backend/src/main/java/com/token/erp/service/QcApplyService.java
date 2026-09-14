package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.QcApplyRequest;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.QcApply;
import com.token.erp.entity.QualityInspection;
import com.token.erp.mapper.QcApplyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QcApplyService {

    private final QcApplyMapper qcApplyMapper;
    private final QualityService qualityService;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<QcApply> page(long page, long pageSize, String sourceType, String status) {
        LambdaQueryWrapper<QcApply> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sourceType)) qw.eq(QcApply::getSourceType, sourceType);
        if (StringUtils.hasText(status)) qw.eq(QcApply::getStatus, status);
        qw.orderByDesc(QcApply::getId);
        Page<QcApply> result = qcApplyMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            List<QcApply> records = result.getRecords();
            List<Long> pids = records.stream().filter(i -> "product".equals(i.getItemType()))
                    .map(QcApply::getItemId).collect(Collectors.toList());
            List<Long> mids = records.stream().filter(i -> !"product".equals(i.getItemType()))
                    .map(QcApply::getItemId).collect(Collectors.toList());
            Map<Long, String[]> pm = nameFillUtil.productMap(pids);
            Map<Long, String[]> mm = nameFillUtil.materialMap(mids);
            Map<Long, String> wm = nameFillUtil.warehouseNameMap(
                    records.stream().map(QcApply::getWarehouseId).collect(Collectors.toList()));
            records.forEach(i -> {
                String[] arr = "product".equals(i.getItemType()) ? pm.get(i.getItemId()) : mm.get(i.getItemId());
                if (arr != null) {
                    i.setItemCode(arr[0]);
                    i.setItemName(arr[1]);
                }
                i.setWarehouseName(wm.get(i.getWarehouseId()));
            });
        }
        return result;
    }

    @Transactional
    public QcApply create(QcApplyRequest request) {
        if (request.getItemId() == null) throw new BizException("物料/产品不能为空");
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("申请质检数量必须大于0");
        }
        if (!StringUtils.hasText(request.getSourceType())) throw new BizException("质检来源（四线）不能为空");

        QcApply apply = new QcApply();
        apply.setApplyNo(codeGenerator.next("QS"));
        apply.setSourceType(request.getSourceType());
        apply.setSourceId(request.getSourceId());
        apply.setSourceNo(request.getSourceNo());
        apply.setItemId(request.getItemId());
        apply.setItemType(StringUtils.hasText(request.getItemType()) ? request.getItemType() : "material");
        apply.setWarehouseId(request.getWarehouseId());
        apply.setLocationId(request.getLocationId());
        apply.setBatchNo(request.getBatchNo());
        apply.setSerialNo(request.getSerialNo());
        apply.setQuantity(request.getQuantity());
        apply.setApplyDate(request.getApplyDate() != null ? request.getApplyDate() : LocalDate.now());
        apply.setStatus("pending");
        apply.setRemark(request.getRemark());
        qcApplyMapper.insert(apply);
        return apply;
    }

    /** 申请单生成检验单（三段式：申请 → 检验），并把库存转待检 available → pending_qc */
    @Transactional
    public QualityInspection generateInspection(Long applyId) {
        QcApply apply = qcApplyMapper.selectById(applyId);
        if (apply == null) throw new BizException(404, "质检申请单不存在");
        if (!"pending".equals(apply.getStatus())) throw new BizException("该申请单已生成检验单或已检");

        // 转待检：优先匹配已在待检的库存（采购质检物料入库即待检），否则从可用库存搬运缺口
        transferToPendingQc(apply);

        QualityInspection inspection = new QualityInspection();
        inspection.setSourceType(apply.getSourceType());
        inspection.setSourceId(apply.getId());
        inspection.setSourceNo(apply.getApplyNo());
        inspection.setItemId(apply.getItemId());
        inspection.setItemType(apply.getItemType());
        inspection.setWarehouseId(apply.getWarehouseId());
        inspection.setLocationId(apply.getLocationId());
        inspection.setBatchNo(apply.getBatchNo());
        inspection.setSerialNo(apply.getSerialNo());
        inspection.setQuantity(apply.getQuantity());
        inspection.setRemark(apply.getRemark());
        QualityInspection created = qualityService.create(inspection);

        apply.setInspectionId(created.getId());
        apply.setStatus("inspecting");
        qcApplyMapper.updateById(apply);
        return created;
    }

    /**
     * 库存转待检：优先匹配已在待检（pending_qc）的库存——采购质检物料入库时直接进待检，报检无需再搬运；
     * 否则从可用库存（available）搬运缺口量。不做 try-catch 吞异常，缺货时抛出明确的业务异常。
     */
    private void transferToPendingQc(QcApply apply) {
        Inventory pendingDim = buildDim(apply, "pending_qc");

        // 1. 精确匹配（含库位/批次/序列号）该批次是否已在待检区
        BigDecimal pendingQty = inventoryService.stockQty(pendingDim);
        if (pendingQty.compareTo(apply.getQuantity()) >= 0) {
            // 已在待检，无需搬运（采购质检物料的正常路径）
            return;
        }

        // 2. 待检精确库存不足，查该物料在待检区的跨批次总量，区分「漏填批次」与「真缺货」
        BigDecimal pendingTotal = inventoryService.stockQtyTotal(
                apply.getItemId(), apply.getItemType(), apply.getWarehouseId(), "pending_qc");
        if (pendingTotal.compareTo(apply.getQuantity()) >= 0) {
            // 待检区有足够的带批次库存，但没按批次精确匹配上 → 提示指定正确批次，避免检错货
            throw new BizException("该物料在待检区存在带批次库存（共 " + pendingTotal
                    + "），请指定正确批次/库位后报检");
        }

        // 3. 待检区确实无货，从可用库存搬运缺口量
        BigDecimal shortage = apply.getQuantity().subtract(pendingQty);
        Inventory availableDim = buildDim(apply, "available");
        inventoryService.change(availableDim, "out", shortage,
                "qc_apply", apply.getId(), apply.getApplyNo());
        inventoryService.change(pendingDim, "in", shortage,
                "qc_apply", apply.getId(), apply.getApplyNo());
    }

    /** 构造库存维度（库位/批次/序列号与申请单保持一致） */
    private Inventory buildDim(QcApply apply, String stockType) {
        Inventory dim = new Inventory();
        dim.setItemId(apply.getItemId());
        dim.setItemType(apply.getItemType());
        dim.setWarehouseId(apply.getWarehouseId());
        dim.setLocationId(apply.getLocationId());
        dim.setBatchNo(apply.getBatchNo());
        dim.setSerialNo(apply.getSerialNo());
        dim.setStockType(stockType);
        return dim;
    }
}
