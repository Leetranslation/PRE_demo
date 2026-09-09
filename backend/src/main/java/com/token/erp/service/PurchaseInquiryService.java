package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PurchaseInquiryRequest;
import com.token.erp.entity.PurchaseInquiry;
import com.token.erp.entity.PurchaseInquiryItem;
import com.token.erp.mapper.PurchaseInquiryItemMapper;
import com.token.erp.mapper.PurchaseInquiryMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseInquiryService {

    private final PurchaseInquiryMapper inquiryMapper;
    private final PurchaseInquiryItemMapper itemMapper;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;

    public Page<PurchaseInquiry> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<PurchaseInquiry> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(PurchaseInquiry::getInquiryNo, keyword);
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(PurchaseInquiry::getApproveStatus, approveStatus);
        }
        qw.orderByDesc(PurchaseInquiry::getId);
        return inquiryMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public List<PurchaseInquiryItem> items(Long inquiryId) {
        return itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseInquiryItem>().eq(PurchaseInquiryItem::getInquiryId, inquiryId));
    }

    @Transactional
    public PurchaseInquiry create(PurchaseInquiryRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("询价明细不能为空");

        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setInquiryNo(codeGenerator.next("XJ"));
        inquiry.setInquiryDate(request.getInquiryDate() != null ? request.getInquiryDate() : LocalDate.now());
        inquiry.setInquirer(request.getInquirer());
        inquiry.setApproveStatus("pending");
        inquiry.setBizStatus("pending");
        inquiry.setRemark(request.getRemark());
        inquiryMapper.insert(inquiry);

        for (PurchaseInquiryRequest.Item item : request.getItems()) {
            if (item.getMaterialId() == null) throw new BizException("物料不能为空");
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("数量必须大于0");

            PurchaseInquiryItem ii = new PurchaseInquiryItem();
            ii.setInquiryId(inquiry.getId());
            ii.setMaterialId(item.getMaterialId());
            ii.setSpec(item.getSpec());
            ii.setUnit(item.getUnit());
            ii.setQuantity(qty);
            ii.setSupplierId(item.getSupplierId());
            ii.setPrice(item.getPrice());
            ii.setRemark(item.getRemark());
            itemMapper.insert(ii);
        }
        return inquiry;
    }

    @Transactional
    public void submit(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) throw new BizException(404, "询价单不存在");
        if (!"pending".equals(inquiry.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        inquiry.setApproveStatus("approving");
        inquiryMapper.updateById(inquiry);
    }

    @Transactional
    public void approve(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) throw new BizException(404, "询价单不存在");
        if (!"approving".equals(inquiry.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        checkSelfApprove(inquiry.getCreateBy());
        inquiry.setApproveStatus("approved");
        inquiryMapper.updateById(inquiry);
    }

    @Transactional
    public void unApprove(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) throw new BizException(404, "询价单不存在");
        if (!"approved".equals(inquiry.getApproveStatus())) throw new BizException("仅已审批可反审核");
        inquiry.setApproveStatus("pending");
        inquiryMapper.updateById(inquiry);
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
