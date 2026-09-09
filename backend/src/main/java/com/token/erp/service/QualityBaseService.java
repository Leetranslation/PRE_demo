package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.InspectionMethod;
import com.token.erp.entity.InspectionScheme;
import com.token.erp.mapper.InspectionMethodMapper;
import com.token.erp.mapper.InspectionSchemeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 质检基础：检验方法 + 检验方案
 */
@Service
@RequiredArgsConstructor
public class QualityBaseService {

    private final InspectionMethodMapper methodMapper;
    private final InspectionSchemeMapper schemeMapper;
    private final CodeGenerator codeGenerator;

    // ===== 检验方法 =====
    public Page<InspectionMethod> methodPage(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<InspectionMethod> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(InspectionMethod::getName, keyword).or().like(InspectionMethod::getCode, keyword));
        }
        if (status != null) qw.eq(InspectionMethod::getStatus, status);
        qw.orderByDesc(InspectionMethod::getId);
        return methodMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public InspectionMethod methodCreate(InspectionMethod m) {
        if (!StringUtils.hasText(m.getName())) throw new BizException("检验方法名称不能为空");
        m.setId(null);
        m.setCode(codeGenerator.next("JF"));
        if (m.getStatus() == null) m.setStatus(1);
        methodMapper.insert(m);
        return m;
    }

    @Transactional
    public InspectionMethod methodUpdate(InspectionMethod m) {
        if (m.getId() == null) throw new BizException("ID不能为空");
        methodMapper.updateById(m);
        return m;
    }

    @Transactional
    public void methodStatus(Long id, Integer status) {
        InspectionMethod m = methodMapper.selectById(id);
        if (m == null) throw new BizException(404, "检验方法不存在");
        m.setStatus(status);
        methodMapper.updateById(m);
    }

    // ===== 检验方案 =====
    public Page<InspectionScheme> schemePage(long page, long pageSize, String keyword, String itemType, Integer status) {
        LambdaQueryWrapper<InspectionScheme> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(InspectionScheme::getName, keyword).or().like(InspectionScheme::getCode, keyword));
        }
        if (StringUtils.hasText(itemType)) qw.eq(InspectionScheme::getItemType, itemType);
        if (status != null) qw.eq(InspectionScheme::getStatus, status);
        qw.orderByDesc(InspectionScheme::getId);
        return schemeMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public InspectionScheme schemeCreate(InspectionScheme s) {
        if (!StringUtils.hasText(s.getName())) throw new BizException("检验方案名称不能为空");
        s.setId(null);
        s.setCode(codeGenerator.next("FA"));
        if (s.getItemType() == null) s.setItemType("material");
        if (s.getStatus() == null) s.setStatus(1);
        schemeMapper.insert(s);
        return s;
    }

    @Transactional
    public InspectionScheme schemeUpdate(InspectionScheme s) {
        if (s.getId() == null) throw new BizException("ID不能为空");
        schemeMapper.updateById(s);
        return s;
    }

    @Transactional
    public void schemeStatus(Long id, Integer status) {
        InspectionScheme s = schemeMapper.selectById(id);
        if (s == null) throw new BizException(404, "检验方案不存在");
        s.setStatus(status);
        schemeMapper.updateById(s);
    }
}
