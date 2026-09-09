package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PurchaseInvoiceRequest;
import com.token.erp.entity.PurchaseInvoice;
import com.token.erp.entity.PurchaseOrder;
import com.token.erp.mapper.PurchaseInvoiceMapper;
import com.token.erp.mapper.PurchaseOrderMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PurchaseInvoiceService {

    private final PurchaseInvoiceMapper invoiceMapper;
    private final PurchaseOrderMapper orderMapper;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;

    public Page<PurchaseInvoice> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<PurchaseInvoice> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(PurchaseInvoice::getInvoiceNo, keyword);
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(PurchaseInvoice::getApproveStatus, approveStatus);
        }
        qw.orderByDesc(PurchaseInvoice::getId);
        return invoiceMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public PurchaseInvoice create(PurchaseInvoiceRequest request) {
        if (request.getOrderId() == null) throw new BizException("采购订单不能为空");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("发票金额必须大于0");
        }
        PurchaseOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "采购订单不存在");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("仅已审批的采购订单可开票");

        // 校验：发票金额 ≤ 订单未开票金额（含已创建未审批的发票）
        BigDecimal invoiced = order.getInvoicedAmount() == null ? BigDecimal.ZERO : order.getInvoicedAmount();
        BigDecimal pendingAmount = sumPendingAmount(request.getOrderId());
        BigDecimal available = order.getTotalAmount().subtract(invoiced).subtract(pendingAmount);
        if (request.getAmount().compareTo(available) > 0) {
            throw new BizException("发票金额超过未开票金额，当前可开票：" + available);
        }

        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setInvoiceNo(codeGenerator.next("FP"));
        invoice.setOrderId(request.getOrderId());
        invoice.setSupplierId(order.getSupplierId());
        invoice.setInvoiceDate(request.getInvoiceDate() != null ? request.getInvoiceDate() : LocalDate.now());
        invoice.setAmount(request.getAmount());
        invoice.setApproveStatus("pending");
        invoice.setBizStatus("pending");
        invoice.setVerifiedAmount(BigDecimal.ZERO);
        invoice.setRemark(request.getRemark());
        invoiceMapper.insert(invoice);
        return invoice;
    }

    @Transactional
    public void submit(Long id) {
        PurchaseInvoice inv = invoiceMapper.selectById(id);
        if (inv == null) throw new BizException(404, "发票不存在");
        if (!"pending".equals(inv.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        inv.setApproveStatus("approving");
        invoiceMapper.updateById(inv);
    }

    @Transactional
    public void approve(Long id) {
        PurchaseInvoice inv = invoiceMapper.selectById(id);
        if (inv == null) throw new BizException(404, "发票不存在");
        if (!"approving".equals(inv.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        checkSelfApprove(inv.getCreateBy());
        inv.setApproveStatus("approved");
        invoiceMapper.updateById(inv);

        // 回写订单已开票金额
        PurchaseOrder order = orderMapper.selectById(inv.getOrderId());
        if (order != null) {
            BigDecimal invoiced = order.getInvoicedAmount() == null ? BigDecimal.ZERO : order.getInvoicedAmount();
            order.setInvoicedAmount(invoiced.add(inv.getAmount()));
            orderMapper.updateById(order);
        }
    }

    @Transactional
    public void unApprove(Long id) {
        PurchaseInvoice inv = invoiceMapper.selectById(id);
        if (inv == null) throw new BizException(404, "发票不存在");
        if (!"approved".equals(inv.getApproveStatus())) throw new BizException("仅已审批可反审核");
        if (!"pending".equals(inv.getBizStatus())) throw new BizException("已核销的发票不能反审核，需先处理核销");
        inv.setApproveStatus("pending");
        invoiceMapper.updateById(inv);

        // 回退订单已开票金额
        PurchaseOrder order = orderMapper.selectById(inv.getOrderId());
        if (order != null) {
            BigDecimal invoiced = order.getInvoicedAmount() == null ? BigDecimal.ZERO : order.getInvoicedAmount();
            BigDecimal remain = invoiced.subtract(inv.getAmount());
            if (remain.compareTo(BigDecimal.ZERO) < 0) remain = BigDecimal.ZERO;
            order.setInvoicedAmount(remain);
            orderMapper.updateById(order);
        }
    }

    private void checkSelfApprove(String createBy) {
        if (!sysConfigService.getBoolean("allow_self_approve")) {
            String currentUser = securityUtils.currentUsername();
            if (createBy != null && createBy.equals(currentUser)) {
                throw new BizException("禁止自审：不能审批自己提交的单据");
            }
        }
    }

    /** 该订单所有「待提交/审批中」的发票金额合计（未审批回写的） */
    private BigDecimal sumPendingAmount(Long orderId) {
        java.util.List<PurchaseInvoice> list = invoiceMapper.selectList(
                new LambdaQueryWrapper<PurchaseInvoice>()
                        .eq(PurchaseInvoice::getOrderId, orderId)
                        .in(PurchaseInvoice::getApproveStatus, "pending", "approving"));
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseInvoice inv : list) {
            if (inv.getAmount() != null) total = total.add(inv.getAmount());
        }
        return total;
    }
}
