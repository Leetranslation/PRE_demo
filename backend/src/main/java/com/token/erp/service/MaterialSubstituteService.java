package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Material;
import com.token.erp.entity.MaterialSubstitute;
import com.token.erp.mapper.MaterialMapper;
import com.token.erp.mapper.MaterialSubstituteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 物料替代料关系
 */
@Service
@RequiredArgsConstructor
public class MaterialSubstituteService {

    private final MaterialSubstituteMapper substituteMapper;
    private final MaterialMapper materialMapper;

    public Page<MaterialSubstitute> page(long page, long pageSize, Long materialId, Integer status) {
        LambdaQueryWrapper<MaterialSubstitute> qw = new LambdaQueryWrapper<>();
        if (materialId != null) qw.eq(MaterialSubstitute::getMaterialId, materialId);
        if (status != null) qw.eq(MaterialSubstitute::getStatus, status);
        qw.orderByAsc(MaterialSubstitute::getPriority).orderByDesc(MaterialSubstitute::getId);
        return substituteMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public MaterialSubstitute create(MaterialSubstitute ms) {
        validate(ms);
        ms.setId(null);
        if (ms.getRatio() == null || ms.getRatio().compareTo(BigDecimal.ZERO) <= 0) ms.setRatio(BigDecimal.ONE);
        if (ms.getPriority() == null) ms.setPriority(0);
        if (ms.getStatus() == null) ms.setStatus(1);
        substituteMapper.insert(ms);
        return ms;
    }

    @Transactional
    public MaterialSubstitute update(MaterialSubstitute ms) {
        if (ms.getId() == null) throw new BizException("ID不能为空");
        validate(ms);
        substituteMapper.updateById(ms);
        return ms;
    }

    @Transactional
    public void status(Long id, Integer status) {
        MaterialSubstitute ms = substituteMapper.selectById(id);
        if (ms == null) throw new BizException(404, "替代料关系不存在");
        ms.setStatus(status);
        substituteMapper.updateById(ms);
    }

    @Transactional
    public void delete(Long id) {
        substituteMapper.deleteById(id);
    }

    private void validate(MaterialSubstitute ms) {
        if (ms.getMaterialId() == null || ms.getSubstituteMaterialId() == null)
            throw new BizException("原物料和替代物料不能为空");
        if (ms.getMaterialId().equals(ms.getSubstituteMaterialId()))
            throw new BizException("替代物料不能与原物料相同");
        Material m = materialMapper.selectById(ms.getMaterialId());
        if (m == null) throw new BizException("原物料不存在");
        Material s = materialMapper.selectById(ms.getSubstituteMaterialId());
        if (s == null) throw new BizException("替代物料不存在");
    }
}
