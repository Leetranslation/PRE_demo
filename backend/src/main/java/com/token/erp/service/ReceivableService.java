package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Receivable;
import com.token.erp.entity.SalesInvoice;
import com.token.erp.mapper.ReceivableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableMapper receivableMapper;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<Receivable> page(long page, long pageSize, Long customerId, String status) {
        LambdaQueryWrapper<Receivable> qw = new LambdaQueryWrapper<>();
        if (customerId != null) qw.eq(Receivable::getCustomerId, customerId);
        if (status != null && !status.isEmpty()) qw.eq(Receivable::getStatus, status);
        qw.orderByDesc(Receivable::getId);
        Page<Receivable> result = receivableMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> cm = nameFillUtil.customerNameMap(
                    result.getRecords().stream().map(Receivable::getCustomerId).collect(Collectors.toList()));
            result.getRecords().forEach(r -> r.setCustomerName(cm.get(r.getCustomerId())));
        }
        return result;
    }

    /** 销售发票审批通过时生成应收台账 */
    @Transactional
    public Receivable generateFromInvoice(SalesInvoice invoice) {
        Receivable r = new Receivable();
        r.setReceivableNo(codeGenerator.next("YS"));
        r.setCustomerId(invoice.getCustomerId());
        r.setInvoiceId(invoice.getId());
        r.setInvoiceNo(invoice.getInvoiceNo());
        r.setAmount(invoice.getAmount());
        r.setWrittenOffAmount(BigDecimal.ZERO);
        r.setBalance(invoice.getAmount());
        r.setStatus("pending");
        receivableMapper.insert(r);
        return r;
    }

    /** 发票反审核时移除未核销的应收；已核销则禁止反审核 */
    @Transactional
    public void removeByInvoice(Long invoiceId) {
        List<Receivable> list = receivableMapper.selectList(
                new LambdaQueryWrapper<Receivable>().eq(Receivable::getInvoiceId, invoiceId));
        for (Receivable r : list) {
            BigDecimal written = r.getWrittenOffAmount() == null ? BigDecimal.ZERO : r.getWrittenOffAmount();
            if (written.compareTo(BigDecimal.ZERO) > 0) {
                throw new BizException("发票已有资金核销记录（应收单 " + r.getReceivableNo() + "），不能反审核");
            }
            receivableMapper.deleteById(r.getId());
        }
    }
}
