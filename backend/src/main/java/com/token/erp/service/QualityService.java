package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.QcApply;
import com.token.erp.entity.QualityInspection;
import com.token.erp.entity.QualityResult;
import com.token.erp.mapper.QcApplyMapper;
import com.token.erp.mapper.QualityInspectionMapper;
import com.token.erp.mapper.QualityResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QualityService {

    private final QualityInspectionMapper inspectionMapper;
    private final QualityResultMapper resultMapper;
    private final QcApplyMapper qcApplyMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<QualityInspection> page(long page, long pageSize, String result) {
        LambdaQueryWrapper<QualityInspection> qw = new LambdaQueryWrapper<>();
        if (result != null && !result.isEmpty()) {
            qw.eq(QualityInspection::getResult, result);
        }
        qw.orderByDesc(QualityInspection::getId);
        Page<QualityInspection> pageResult = inspectionMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!pageResult.getRecords().isEmpty()) {
            List<QualityInspection> records = pageResult.getRecords();
            List<Long> pids = records.stream().filter(i -> "product".equals(i.getItemType()))
                    .map(QualityInspection::getItemId).collect(Collectors.toList());
            List<Long> mids = records.stream().filter(i -> !"product".equals(i.getItemType()))
                    .map(QualityInspection::getItemId).collect(Collectors.toList());
            Map<Long, String[]> pm = nameFillUtil.productMap(pids);
            Map<Long, String[]> mm = nameFillUtil.materialMap(mids);
            Map<Long, String> wm = nameFillUtil.warehouseNameMap(
                    records.stream().map(QualityInspection::getWarehouseId).collect(Collectors.toList()));
            records.forEach(i -> {
                String[] arr = "product".equals(i.getItemType()) ? pm.get(i.getItemId()) : mm.get(i.getItemId());
                if (arr != null) {
                    i.setItemCode(arr[0]);
                    i.setItemName(arr[1]);
                }
                i.setWarehouseName(wm.get(i.getWarehouseId()));
            });
        }
        return pageResult;
    }

    /** 创建质检单（针对待检库存的物料） */
    @Transactional
    public QualityInspection create(QualityInspection inspection) {
        inspection.setId(null);
        inspection.setInspectionNo(codeGenerator.next("QC"));
        inspection.setInspectionDate(inspection.getInspectionDate() != null
                ? inspection.getInspectionDate() : LocalDate.now());
        inspection.setResult(null);
        inspectionMapper.insert(inspection);
        return inspection;
    }

    /**
     * 质检判定
     * @param inspectionId 质检单ID
     * @param result 合格 qualified / 不合格 unqualified
     * @param handleType 不合格处理方式：return退货 / scrap报废（仅不合格时必填）
     * @param reason 不合格原因
     */
    @Transactional
    public void judge(Long inspectionId, String result, String handleType, String reason) {
        QualityInspection inspection = inspectionMapper.selectById(inspectionId);
        if (inspection == null) throw new BizException(404, "质检单不存在");
        if (inspection.getResult() != null) throw new BizException("该质检单已判定");

        inspection.setResult(result);
        inspectionMapper.updateById(inspection);

        // 待检库存维度
        Inventory pendingDim = new Inventory();
        pendingDim.setItemId(inspection.getItemId());
        pendingDim.setItemType(inspection.getItemType());
        pendingDim.setWarehouseId(inspection.getWarehouseId() != null ? inspection.getWarehouseId() : null);
        pendingDim.setStockType("pending_qc");

        if ("qualified".equals(result)) {
            // 合格：待检 → 可用（先出待检，再入可用）
            inventoryService.change(pendingDim, "out", inspection.getQuantity(),
                    "quality_qualified", inspection.getId(), inspection.getInspectionNo());
            Inventory availableDim = new Inventory();
            availableDim.setItemId(inspection.getItemId());
            availableDim.setItemType(inspection.getItemType());
            availableDim.setWarehouseId(inspection.getWarehouseId());
            availableDim.setStockType("available");
            inventoryService.change(availableDim, "in", inspection.getQuantity(),
                    "quality_qualified", inspection.getId(), inspection.getInspectionNo());
        } else if ("unqualified".equals(result)) {
            if (handleType == null || handleType.isEmpty()) {
                throw new BizException("不合格需指定处理方式（退货/报废）");
            }
            // 不合格：待检 → 不合格库存（退货/报废后续处理）
            inventoryService.change(pendingDim, "out", inspection.getQuantity(),
                    "quality_unqualified", inspection.getId(), inspection.getInspectionNo());
            Inventory unqualifiedDim = new Inventory();
            unqualifiedDim.setItemId(inspection.getItemId());
            unqualifiedDim.setItemType(inspection.getItemType());
            unqualifiedDim.setWarehouseId(inspection.getWarehouseId());
            unqualifiedDim.setStockType("unqualified");
            inventoryService.change(unqualifiedDim, "in", inspection.getQuantity(),
                    "quality_unqualified", inspection.getId(), inspection.getInspectionNo());

            // 记录处理结果
            QualityResult qr = new QualityResult();
            qr.setInspectionId(inspection.getId());
            qr.setHandleType(handleType);
            qr.setQuantity(inspection.getQuantity());
            qr.setReason(reason);
            qr.setResultStatus("unqualified");
            qr.setCreateTime(LocalDateTime.now());
            resultMapper.insert(qr);
        } else {
            throw new BizException("非法质检结果");
        }

        // 回写质检申请单状态（三段式闭环：判定后申请单已检）
        QcApply apply = qcApplyMapper.selectOne(
                new LambdaQueryWrapper<QcApply>().eq(QcApply::getInspectionId, inspection.getId()));
        if (apply != null) {
            apply.setStatus("finished");
            qcApplyMapper.updateById(apply);
        }
    }
}
