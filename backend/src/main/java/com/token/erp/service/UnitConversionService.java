package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.UnitConversion;
import com.token.erp.mapper.UnitConversionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private final UnitConversionMapper unitConversionMapper;

    public Page<UnitConversion> page(long page, long pageSize, Integer status) {
        LambdaQueryWrapper<UnitConversion> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(UnitConversion::getStatus, status);
        }
        qw.orderByDesc(UnitConversion::getId);
        return unitConversionMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public UnitConversion create(UnitConversion conversion) {
        if (conversion.getUnitId() == null || conversion.getTargetUnitId() == null) {
            throw new BizException("源单位和目标单位不能为空");
        }
        if (conversion.getRate() == null || conversion.getRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("换算率必须大于0");
        }
        conversion.setId(null);
        conversion.setStatus(conversion.getStatus() == null ? 1 : conversion.getStatus());
        unitConversionMapper.insert(conversion);
        return conversion;
    }

    public UnitConversion update(UnitConversion conversion) {
        if (unitConversionMapper.selectById(conversion.getId()) == null) {
            throw new BizException(404, "换算率不存在");
        }
        unitConversionMapper.updateById(conversion);
        return unitConversionMapper.selectById(conversion.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (unitConversionMapper.selectById(id) == null) {
            throw new BizException(404, "换算率不存在");
        }
        UnitConversion upd = new UnitConversion();
        upd.setId(id);
        upd.setStatus(status);
        unitConversionMapper.updateById(upd);
    }
}
