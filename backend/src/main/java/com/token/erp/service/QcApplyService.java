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

        // 转待检：available → pending_qc（若 available 充足；否则说明库存已在待检，跳过）
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

    /** 库存转待检：available → pending_qc（尽力转，转不了说明已在待检） */
    private void transferToPendingQc(QcApply apply) {
        try {
            Inventory availableDim = new Inventory();
            availableDim.setItemId(apply.getItemId());
            availableDim.setItemType(apply.getItemType());
            availableDim.setWarehouseId(apply.getWarehouseId());
            availableDim.setLocationId(apply.getLocationId());
            availableDim.setBatchNo(apply.getBatchNo());
            availableDim.setSerialNo(apply.getSerialNo());
            availableDim.setStockType("available");
            inventoryService.change(availableDim, "out", apply.getQuantity(),
                    "qc_apply", apply.getId(), apply.getApplyNo());

            Inventory pendingDim = new Inventory();
            pendingDim.setItemId(apply.getItemId());
            pendingDim.setItemType(apply.getItemType());
            pendingDim.setWarehouseId(apply.getWarehouseId());
            pendingDim.setLocationId(apply.getLocationId());
            pendingDim.setBatchNo(apply.getBatchNo());
            pendingDim.setSerialNo(apply.getSerialNo());
            pendingDim.setStockType("pending_qc");
            inventoryService.change(pendingDim, "in", apply.getQuantity(),
                    "qc_apply", apply.getId(), apply.getApplyNo());
        } catch (BizException e) {
            // available 库存不足：库存可能已在 pending_qc，无需转换
        }
    }
}
