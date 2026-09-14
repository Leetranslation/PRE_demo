package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Payable;
import com.token.erp.entity.PurchaseInvoice;
import com.token.erp.mapper.PayableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayableService {

    private final PayableMapper payableMapper;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<Payable> page(long page, long pageSize, Long supplierId, String status) {
        LambdaQueryWrapper<Payable> qw = new LambdaQueryWrapper<>();
        if (supplierId != null) qw.eq(Payable::getSupplierId, supplierId);
        if (status != null && !status.isEmpty()) qw.eq(Payable::getStatus, status);
        qw.orderByDesc(Payable::getId);
        Page<Payable> result = payableMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> sm = nameFillUtil.supplierNameMap(
                    result.getRecords().stream().map(Payable::getSupplierId).collect(Collectors.toList()));
            result.getRecords().forEach(p -> p.setSupplierName(sm.get(p.getSupplierId())));
        }
        return result;
    }

    /** 采购发票审批通过时生成应付台账 */
    @Transactional
    public Payable generateFromInvoice(PurchaseInvoice invoice) {
        Payable p = new Payable();
        p.setPayableNo(codeGenerator.next("YF"));
        p.setSupplierId(invoice.getSupplierId());
        p.setInvoiceId(invoice.getId());
        p.setInvoiceNo(invoice.getInvoiceNo());
        p.setAmount(invoice.getAmount());
        p.setWrittenOffAmount(BigDecimal.ZERO);
        p.setBalance(invoice.getAmount());
        p.setStatus("pending");
        payableMapper.insert(p);
        return p;
    }

    /** 发票反审核时移除未核销的应付；已核销则禁止反审核 */
    @Transactional
    public void removeByInvoice(Long invoiceId) {
        List<Payable> list = payableMapper.selectList(
                new LambdaQueryWrapper<Payable>().eq(Payable::getInvoiceId, invoiceId));
        for (Payable p : list) {
            BigDecimal written = p.getWrittenOffAmount() == null ? BigDecimal.ZERO : p.getWrittenOffAmount();
            if (written.compareTo(BigDecimal.ZERO) > 0) {
                throw new BizException("发票已有资金核销记录（应付单 " + p.getPayableNo() + "），不能反审核");
            }
            payableMapper.deleteById(p.getId());
        }
    }
}
