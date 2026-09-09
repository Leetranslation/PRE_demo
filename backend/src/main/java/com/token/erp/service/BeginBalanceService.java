package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.BeginBalance;
import com.token.erp.mapper.BeginBalanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 期初余额：客户应收 / 供应商应付期初
 */
@Service
@RequiredArgsConstructor
public class BeginBalanceService {

    private final BeginBalanceMapper beginBalanceMapper;

    public Page<BeginBalance> page(long page, long pageSize, String targetType, String keyword) {
        LambdaQueryWrapper<BeginBalance> qw = new LambdaQueryWrapper<>();
        if (targetType != null && !targetType.isEmpty()) qw.eq(BeginBalance::getTargetType, targetType);
        if (keyword != null && !keyword.isEmpty()) qw.like(BeginBalance::getTargetName, keyword);
        qw.orderByDesc(BeginBalance::getId);
        return beginBalanceMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** upsert：同一对象（客户/供应商）只有一条期初 */
    @Transactional
    public BeginBalance save(BeginBalance bb) {
        if (bb.getTargetType() == null || bb.getTargetId() == null)
            throw new BizException("对象类型和对象不能为空");
        if (bb.getAmount() == null) bb.setAmount(BigDecimal.ZERO);
        BeginBalance existing = beginBalanceMapper.selectOne(
                new LambdaQueryWrapper<BeginBalance>()
                        .eq(BeginBalance::getTargetType, bb.getTargetType())
                        .eq(BeginBalance::getTargetId, bb.getTargetId()));
        if (existing != null) {
            existing.setAmount(bb.getAmount());
            existing.setTargetName(bb.getTargetName());
            existing.setRemark(bb.getRemark());
            beginBalanceMapper.updateById(existing);
            return existing;
        }
        beginBalanceMapper.insert(bb);
        return bb;
    }

    @Transactional
    public void delete(Long id) {
        beginBalanceMapper.deleteById(id);
    }
}
