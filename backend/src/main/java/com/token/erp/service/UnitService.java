package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Unit;
import com.token.erp.mapper.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitMapper unitMapper;

    public Page<Unit> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Unit> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Unit::getName, keyword).or().like(Unit::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Unit::getStatus, status);
        }
        qw.orderByDesc(Unit::getId);
        return unitMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Unit create(Unit unit) {
        if (!StringUtils.hasText(unit.getName())) {
            throw new BizException("单位名称不能为空");
        }
        if (!StringUtils.hasText(unit.getCode())) {
            throw new BizException("单位编码不能为空");
        }
        unit.setId(null);
        unit.setStatus(unit.getStatus() == null ? 1 : unit.getStatus());
        unitMapper.insert(unit);
        return unit;
    }

    public Unit update(Unit unit) {
        if (unitMapper.selectById(unit.getId()) == null) {
            throw new BizException(404, "单位不存在");
        }
        unit.setCode(null);
        unitMapper.updateById(unit);
        return unitMapper.selectById(unit.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (unitMapper.selectById(id) == null) {
            throw new BizException(404, "单位不存在");
        }
        Unit upd = new Unit();
        upd.setId(id);
        upd.setStatus(status);
        unitMapper.updateById(upd);
    }
}
