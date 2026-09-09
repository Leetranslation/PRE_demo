package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.SerialRule;
import com.token.erp.mapper.SerialRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SerialRuleService {

    private final SerialRuleMapper serialRuleMapper;

    public Page<SerialRule> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<SerialRule> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SerialRule::getName, keyword).or().like(SerialRule::getCode, keyword));
        }
        if (status != null) {
            qw.eq(SerialRule::getStatus, status);
        }
        qw.orderByDesc(SerialRule::getId);
        return serialRuleMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public SerialRule create(SerialRule rule) {
        if (!StringUtils.hasText(rule.getName())) {
            throw new BizException("规则名称不能为空");
        }
        rule.setId(null);
        if (!StringUtils.hasText(rule.getCode())) {
            rule.setCode("SR" + System.currentTimeMillis());
        }
        rule.setStartNo(rule.getStartNo() == null ? 1 : rule.getStartNo());
        rule.setStatus(rule.getStatus() == null ? 1 : rule.getStatus());
        serialRuleMapper.insert(rule);
        return rule;
    }

    public SerialRule update(SerialRule rule) {
        if (serialRuleMapper.selectById(rule.getId()) == null) {
            throw new BizException(404, "规则不存在");
        }
        rule.setCode(null);
        serialRuleMapper.updateById(rule);
        return serialRuleMapper.selectById(rule.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (serialRuleMapper.selectById(id) == null) {
            throw new BizException(404, "规则不存在");
        }
        SerialRule upd = new SerialRule();
        upd.setId(id);
        upd.setStatus(status);
        serialRuleMapper.updateById(upd);
    }
}
