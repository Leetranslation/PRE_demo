package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Receipt;
import com.token.erp.entity.ReceiptWriteoff;
import com.token.erp.entity.Receivable;
import com.token.erp.mapper.ReceiptMapper;
import com.token.erp.mapper.ReceiptWriteoffMapper;
import com.token.erp.mapper.ReceivableMapper;
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
public class ReceiptService {

    private final ReceiptMapper receiptMapper;
    private final ReceiptWriteoffMapper writeoffMapper;
    private final ReceivableMapper receivableMapper;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<Receipt> page(long page, long pageSize, Long customerId, String status) {
        LambdaQueryWrapper<Receipt> qw = new LambdaQueryWrapper<>();
        if (customerId != null) qw.eq(Receipt::getCustomerId, customerId);
        if (status != null && !status.isEmpty()) qw.eq(Receipt::getStatus, status);
        qw.orderByDesc(Receipt::getId);
        Page<Receipt> result = receiptMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> cm = nameFillUtil.customerNameMap(
                    result.getRecords().stream().map(Receipt::getCustomerId).collect(Collectors.toList()));
            result.getRecords().forEach(r -> r.setCustomerName(cm.get(r.getCustomerId())));
        }
        return result;
    }

    @Transactional
    public Receipt create(Receipt receipt) {
        if (receipt.getCustomerId() == null) throw new BizException("客户不能为空");
        if (receipt.getAmount() == null || receipt.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("收款金额必须大于0");
        }
        receipt.setId(null);
        receipt.setReceiptNo(codeGenerator.next("SK"));
        receipt.setWrittenOffAmount(BigDecimal.ZERO);
        receipt.setBalance(receipt.getAmount());
        receipt.setStatus("pending");
        receipt.setReceiptDate(receipt.getReceiptDate() != null ? receipt.getReceiptDate() : LocalDate.now());
        receiptMapper.insert(receipt);
        return receipt;
    }

    /**
     * 收款核销应收：items 每个含 receivableId + amount
     * 金额管控：核销金额 ≤ 应收余额 且 ≤ 收款单未核销余额
     */
    @Transactional
    public void writeoff(Long receiptId, List<ReceiptWriteoff> items) {
        Receipt receipt = receiptMapper.selectById(receiptId);
        if (receipt == null) throw new BizException(404, "收款单不存在");
        if (items == null || items.isEmpty()) throw new BizException("核销明细不能为空");

        BigDecimal receiptBalance = receipt.getBalance() == null ? BigDecimal.ZERO : receipt.getBalance();
        for (ReceiptWriteoff item : items) {
            if (item.getReceivableId() == null) throw new BizException("应收台账不能为空");
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("核销金额必须大于0");
            }
            Receivable r = receivableMapper.selectById(item.getReceivableId());
            if (r == null) throw new BizException(404, "应收台账不存在");

            BigDecimal rBalance = r.getBalance() == null ? BigDecimal.ZERO : r.getBalance();
            if (item.getAmount().compareTo(rBalance) > 0) {
                throw new BizException("核销金额超过应收余额，应收单 " + r.getReceivableNo() + " 当前余额：" + rBalance);
            }
            if (item.getAmount().compareTo(receiptBalance) > 0) {
                throw new BizException("核销金额超过收款单未核销余额，当前余额：" + receiptBalance);
            }

            // 扣应收余额、加应收已核销
            BigDecimal rWritten = r.getWrittenOffAmount() == null ? BigDecimal.ZERO : r.getWrittenOffAmount();
            r.setWrittenOffAmount(rWritten.add(item.getAmount()));
            r.setBalance(rBalance.subtract(item.getAmount()));
            r.setStatus(calcStatus(r.getBalance(), r.getAmount()));
            receivableMapper.updateById(r);

            // 累减收款单余额
            receiptBalance = receiptBalance.subtract(item.getAmount());

            // 插入核销明细
            item.setId(null);
            item.setReceiptId(receiptId);
            if (item.getWriteoffDate() == null) item.setWriteoffDate(LocalDate.now());
            item.setCreateTime(LocalDateTime.now());
            writeoffMapper.insert(item);
        }

        // 更新收款单
        BigDecimal written = items.stream().map(ReceiptWriteoff::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receiptWritten = receipt.getWrittenOffAmount() == null ? BigDecimal.ZERO : receipt.getWrittenOffAmount();
        receipt.setWrittenOffAmount(receiptWritten.add(written));
        receipt.setBalance(receiptBalance);
        receipt.setStatus(calcStatus(receipt.getBalance(), receipt.getAmount()));
        receiptMapper.updateById(receipt);
    }

    /** 收款核销明细 */
    public List<ReceiptWriteoff> writeoffItems(Long receiptId) {
        List<ReceiptWriteoff> list = writeoffMapper.selectList(
                new LambdaQueryWrapper<ReceiptWriteoff>()
                        .eq(ReceiptWriteoff::getReceiptId, receiptId)
                        .orderByAsc(ReceiptWriteoff::getId));
        if (!list.isEmpty()) {
            List<Receivable> rs = receivableMapper.selectList(new LambdaQueryWrapper<Receivable>()
                    .in(Receivable::getId, list.stream().map(ReceiptWriteoff::getReceivableId).collect(Collectors.toList())));
            Map<Long, Receivable> rm = rs.stream().collect(Collectors.toMap(Receivable::getId, x -> x, (a, b) -> a));
            Map<Long, String> cm = nameFillUtil.customerNameMap(
                    rs.stream().map(Receivable::getCustomerId).collect(Collectors.toList()));
            list.forEach(w -> {
                Receivable r = rm.get(w.getReceivableId());
                if (r != null) {
                    w.setReceivableNo(r.getReceivableNo());
                    w.setCustomerName(cm.get(r.getCustomerId()));
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
