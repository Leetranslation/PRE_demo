package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PurchaseVerificationRequest;
import com.token.erp.entity.PurchaseInvoice;
import com.token.erp.entity.PurchaseVerification;
import com.token.erp.mapper.PurchaseInvoiceMapper;
import com.token.erp.mapper.PurchaseVerificationMapper;
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
public class PurchaseVerificationService {

    private final PurchaseVerificationMapper verificationMapper;
    private final PurchaseInvoiceMapper invoiceMapper;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;

    public Page<PurchaseVerification> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<PurchaseVerification> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) qw.like(PurchaseVerification::getVerificationNo, keyword);
        if (StringUtils.hasText(approveStatus)) qw.eq(PurchaseVerification::getApproveStatus, approveStatus);
        qw.orderByDesc(PurchaseVerification::getId);
        return verificationMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public PurchaseVerification create(PurchaseVerificationRequest request) {
        if (request.getInvoiceId() == null) throw new BizException("发票不能为空");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("核销金额必须大于0");
        }
        PurchaseInvoice invoice = invoiceMapper.selectById(request.getInvoiceId());
        if (invoice == null) throw new BizException(404, "发票不存在");
        if (!"approved".equals(invoice.getApproveStatus())) throw new BizException("仅已审批的发票可核销");

        BigDecimal verified = invoice.getVerifiedAmount() == null ? BigDecimal.ZERO : invoice.getVerifiedAmount();
        BigDecimal pendingAmount = sumPendingAmount(request.getInvoiceId());
        BigDecimal available = invoice.getAmount().subtract(verified).subtract(pendingAmount);
        if (request.getAmount().compareTo(available) > 0) {
            throw new BizException("核销金额超过未核销金额，当前可核销：" + available);
        }

        PurchaseVerification v = new PurchaseVerification();
        v.setVerificationNo(codeGenerator.next("HX"));
        v.setInvoiceId(request.getInvoiceId());
        v.setOrderId(invoice.getOrderId());
        v.setAmount(request.getAmount());
        v.setVerifyDate(request.getVerifyDate() != null ? request.getVerifyDate() : LocalDate.now());
        v.setApproveStatus("pending");
        v.setRemark(request.getRemark());
        verificationMapper.insert(v);
        return v;
    }

    @Transactional
    public void submit(Long id) {
        PurchaseVerification v = verificationMapper.selectById(id);
        if (v == null) throw new BizException(404, "核销单不存在");
        if (!"pending".equals(v.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        v.setApproveStatus("approving");
        verificationMapper.updateById(v);
    }

    @Transactional
    public void approve(Long id) {
        PurchaseVerification v = verificationMapper.selectById(id);
        if (v == null) throw new BizException(404, "核销单不存在");
        if (!"approving".equals(v.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        checkSelfApprove(v.getCreateBy());
        v.setApproveStatus("approved");
        verificationMapper.updateById(v);

        PurchaseInvoice invoice = invoiceMapper.selectById(v.getInvoiceId());
        if (invoice != null) {
            BigDecimal verified = invoice.getVerifiedAmount() == null ? BigDecimal.ZERO : invoice.getVerifiedAmount();
            invoice.setVerifiedAmount(verified.add(v.getAmount()));
            if (invoice.getVerifiedAmount().compareTo(invoice.getAmount()) >= 0) {
                invoice.setBizStatus("verified");
            } else {
                invoice.setBizStatus("partial");
            }
            invoiceMapper.updateById(invoice);
        }
    }

    @Transactional
    public void unApprove(Long id) {
        PurchaseVerification v = verificationMapper.selectById(id);
        if (v == null) throw new BizException(404, "核销单不存在");
        if (!"approved".equals(v.getApproveStatus())) throw new BizException("仅已审批可反审核");
        v.setApproveStatus("pending");
        verificationMapper.updateById(v);

        PurchaseInvoice invoice = invoiceMapper.selectById(v.getInvoiceId());
        if (invoice != null) {
            BigDecimal verified = invoice.getVerifiedAmount() == null ? BigDecimal.ZERO : invoice.getVerifiedAmount();
            BigDecimal remain = verified.subtract(v.getAmount());
            if (remain.compareTo(BigDecimal.ZERO) < 0) remain = BigDecimal.ZERO;
            invoice.setVerifiedAmount(remain);
            invoice.setBizStatus("pending");
            invoiceMapper.updateById(invoice);
        }
    }

    private BigDecimal sumPendingAmount(Long invoiceId) {
        List<PurchaseVerification> list = verificationMapper.selectList(
                new LambdaQueryWrapper<PurchaseVerification>()
                        .eq(PurchaseVerification::getInvoiceId, invoiceId)
                        .in(PurchaseVerification::getApproveStatus, "pending", "approving"));
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseVerification v : list) {
            if (v.getAmount() != null) total = total.add(v.getAmount());
        }
        return total;
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
