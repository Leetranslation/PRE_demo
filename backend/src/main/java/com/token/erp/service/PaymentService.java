package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Payable;
import com.token.erp.entity.Payment;
import com.token.erp.entity.PaymentWriteoff;
import com.token.erp.mapper.PayableMapper;
import com.token.erp.mapper.PaymentMapper;
import com.token.erp.mapper.PaymentWriteoffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentWriteoffMapper writeoffMapper;
    private final PayableMapper payableMapper;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<Payment> page(long page, long pageSize, Long supplierId, String status) {
        LambdaQueryWrapper<Payment> qw = new LambdaQueryWrapper<>();
        if (supplierId != null) qw.eq(Payment::getSupplierId, supplierId);
        if (status != null && !status.isEmpty()) qw.eq(Payment::getStatus, status);
        qw.orderByDesc(Payment::getId);
        Page<Payment> result = paymentMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> sm = nameFillUtil.supplierNameMap(
                    result.getRecords().stream().map(Payment::getSupplierId).collect(Collectors.toList()));
            result.getRecords().forEach(p -> p.setSupplierName(sm.get(p.getSupplierId())));
        }
        return result;
    }

    @Transactional
    public Payment create(Payment payment) {
        if (payment.getSupplierId() == null) throw new BizException("供应商不能为空");
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("付款金额必须大于0");
        }
        payment.setId(null);
        payment.setPaymentNo(codeGenerator.next("FK"));
        payment.setWrittenOffAmount(BigDecimal.ZERO);
        payment.setBalance(payment.getAmount());
        payment.setStatus("pending");
        payment.setPaymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDate.now());
        paymentMapper.insert(payment);
        return payment;
    }

    /**
     * 付款核销应付：items 每个含 payableId + amount
     * 金额管控：核销金额 ≤ 应付余额 且 ≤ 付款单未核销余额
     */
    @Transactional
    public void writeoff(Long paymentId, List<PaymentWriteoff> items) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) throw new BizException(404, "付款单不存在");
        if (items == null || items.isEmpty()) throw new BizException("核销明细不能为空");

        BigDecimal paymentBalance = payment.getBalance() == null ? BigDecimal.ZERO : payment.getBalance();
        for (PaymentWriteoff item : items) {
            if (item.getPayableId() == null) throw new BizException("应付台账不能为空");
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("核销金额必须大于0");
            }
            Payable p = payableMapper.selectById(item.getPayableId());
            if (p == null) throw new BizException(404, "应付台账不存在");

            BigDecimal pBalance = p.getBalance() == null ? BigDecimal.ZERO : p.getBalance();
            if (item.getAmount().compareTo(pBalance) > 0) {
                throw new BizException("核销金额超过应付余额，应付单 " + p.getPayableNo() + " 当前余额：" + pBalance);
            }
            if (item.getAmount().compareTo(paymentBalance) > 0) {
                throw new BizException("核销金额超过付款单未核销余额，当前余额：" + paymentBalance);
            }

            // 扣应付余额、加应付已核销
            BigDecimal pWritten = p.getWrittenOffAmount() == null ? BigDecimal.ZERO : p.getWrittenOffAmount();
            p.setWrittenOffAmount(pWritten.add(item.getAmount()));
            p.setBalance(pBalance.subtract(item.getAmount()));
            p.setStatus(calcStatus(p.getBalance(), p.getAmount()));
            payableMapper.updateById(p);

            paymentBalance = paymentBalance.subtract(item.getAmount());

            item.setId(null);
            item.setPaymentId(paymentId);
            if (item.getWriteoffDate() == null) item.setWriteoffDate(LocalDate.now());
            item.setCreateTime(LocalDateTime.now());
            writeoffMapper.insert(item);
        }

        BigDecimal written = items.stream().map(PaymentWriteoff::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paymentWritten = payment.getWrittenOffAmount() == null ? BigDecimal.ZERO : payment.getWrittenOffAmount();
        payment.setWrittenOffAmount(paymentWritten.add(written));
        payment.setBalance(paymentBalance);
        payment.setStatus(calcStatus(payment.getBalance(), payment.getAmount()));
        paymentMapper.updateById(payment);
    }

    /** 付款核销明细 */
    public List<PaymentWriteoff> writeoffItems(Long paymentId) {
        List<PaymentWriteoff> list = writeoffMapper.selectList(
                new LambdaQueryWrapper<PaymentWriteoff>()
                        .eq(PaymentWriteoff::getPaymentId, paymentId)
                        .orderByAsc(PaymentWriteoff::getId));
        if (!list.isEmpty()) {
            List<Payable> ps = payableMapper.selectList(new LambdaQueryWrapper<Payable>()
                    .in(Payable::getId, list.stream().map(PaymentWriteoff::getPayableId).collect(Collectors.toList())));
            Map<Long, Payable> pm = ps.stream().collect(Collectors.toMap(Payable::getId, x -> x, (a, b) -> a));
            Map<Long, String> sm = nameFillUtil.supplierNameMap(
                    ps.stream().map(Payable::getSupplierId).collect(Collectors.toList()));
            list.forEach(w -> {
                Payable p = pm.get(w.getPayableId());
                if (p != null) {
                    w.setPayableNo(p.getPayableNo());
                    w.setSupplierName(sm.get(p.getSupplierId()));
                }
            });
        }
        return list;
    }

    private String calcStatus(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) return "settled";
        if (amount != null && balance.compareTo(amount) >= 0) return "pending";
        return "partial";
    }
}
