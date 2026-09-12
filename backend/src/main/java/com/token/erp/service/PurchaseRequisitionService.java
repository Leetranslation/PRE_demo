package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PurchaseRequisitionRequest;
import com.token.erp.entity.PurchaseRequisition;
import com.token.erp.entity.PurchaseRequisitionItem;
import com.token.erp.mapper.PurchaseRequisitionItemMapper;
import com.token.erp.mapper.PurchaseRequisitionMapper;
import com.token.erp.security.SecurityUtils;
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
public class PurchaseRequisitionService {

    private final PurchaseRequisitionMapper requisitionMapper;
    private final PurchaseRequisitionItemMapper itemMapper;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;
    private final NameFillUtil nameFillUtil;

    public Page<PurchaseRequisition> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<PurchaseRequisition> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(PurchaseRequisition::getRequisitionNo, keyword);
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(PurchaseRequisition::getApproveStatus, approveStatus);
        }
        qw.orderByDesc(PurchaseRequisition::getId);
        return requisitionMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public List<PurchaseRequisitionItem> items(Long requisitionId) {
        List<PurchaseRequisitionItem> list = itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseRequisitionItem>().eq(PurchaseRequisitionItem::getRequisitionId, requisitionId));
        if (!list.isEmpty()) {
            Map<Long, String[]> mm = nameFillUtil.materialMap(
                    list.stream().map(PurchaseRequisitionItem::getMaterialId).collect(Collectors.toList()));
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
    public PurchaseRequisition create(PurchaseRequisitionRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("申请明细不能为空");

        PurchaseRequisition req = new PurchaseRequisition();
        req.setRequisitionNo(codeGenerator.next("SQ"));
        req.setDepartmentId(request.getDepartmentId());
        req.setRequisitionDate(request.getRequisitionDate() != null ? request.getRequisitionDate() : LocalDate.now());
        req.setApplicant(request.getApplicant());
        req.setApproveStatus("pending");
        req.setBizStatus("pending");
        req.setRemark(request.getRemark());
        requisitionMapper.insert(req);

        for (PurchaseRequisitionRequest.Item item : request.getItems()) {
            if (item.getMaterialId() == null) throw new BizException("物料不能为空");
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("需求数量必须大于0");

            PurchaseRequisitionItem ri = new PurchaseRequisitionItem();
            ri.setRequisitionId(req.getId());
            ri.setMaterialId(item.getMaterialId());
            ri.setSpec(item.getSpec());
            ri.setUnit(item.getUnit());
            ri.setQuantity(qty);
            ri.setRequiredDate(item.getRequiredDate());
            ri.setRemark(item.getRemark());
            itemMapper.insert(ri);
        }
        return req;
    }

    @Transactional
    public void submit(Long id) {
        PurchaseRequisition req = requisitionMapper.selectById(id);
        if (req == null) throw new BizException(404, "采购申请单不存在");
        if (!"pending".equals(req.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        req.setApproveStatus("approving");
        requisitionMapper.updateById(req);
    }

    @Transactional
    public void approve(Long id) {
        PurchaseRequisition req = requisitionMapper.selectById(id);
        if (req == null) throw new BizException(404, "采购申请单不存在");
        if (!"approving".equals(req.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        checkSelfApprove(req.getCreateBy());
        req.setApproveStatus("approved");
        requisitionMapper.updateById(req);
    }

    @Transactional
    public void unApprove(Long id) {
        PurchaseRequisition req = requisitionMapper.selectById(id);
        if (req == null) throw new BizException(404, "采购申请单不存在");
        if (!"approved".equals(req.getApproveStatus())) throw new BizException("仅已审批可反审核");
        if ("purchased".equals(req.getBizStatus())) throw new BizException("已转采购的申请单不能反审核");
        req.setApproveStatus("pending");
        requisitionMapper.updateById(req);
    }

    /** 标记已转采购（由采购订单生成时回写） */
    @Transactional
    public void markPurchased(Long id) {
        PurchaseRequisition req = requisitionMapper.selectById(id);
        if (req == null) return;
        req.setBizStatus("purchased");
        requisitionMapper.updateById(req);
    }

    private void checkSelfApprove(String createBy) {
        if (!sysConfigService.getBoolean("allow_self_approve")) {
            String currentUser = securityUtils.currentUsername();
            if (createBy != null && createBy.equals(currentUser)) {
                throw new BizException("禁止自审：不能审批自己提交的单据");
            }
        }
    }
}
