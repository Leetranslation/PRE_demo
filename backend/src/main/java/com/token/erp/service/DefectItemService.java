package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.DefectItem;
import com.token.erp.mapper.DefectItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DefectItemService {

    private final DefectItemMapper defectItemMapper;
    private final CodeGenerator codeGenerator;

    public Page<DefectItem> page(long page, long pageSize, String keyword, String category, Integer status) {
        LambdaQueryWrapper<DefectItem> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(DefectItem::getName, keyword).or().like(DefectItem::getCode, keyword));
        }
        if (StringUtils.hasText(category)) qw.eq(DefectItem::getCategory, category);
        if (status != null) qw.eq(DefectItem::getStatus, status);
        qw.orderByDesc(DefectItem::getId);
        return defectItemMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public DefectItem create(DefectItem di) {
        if (!StringUtils.hasText(di.getName())) throw new BizException("不良品项名称不能为空");
        di.setId(null);
        di.setCode(codeGenerator.next("BL"));
        if (di.getStatus() == null) di.setStatus(1);
        defectItemMapper.insert(di);
        return di;
    }

    @Transactional
    public DefectItem update(DefectItem di) {
        if (di.getId() == null) throw new BizException("ID不能为空");
        defectItemMapper.updateById(di);
        return di;
    }

    @Transactional
    public void status(Long id, Integer status) {
        DefectItem di = defectItemMapper.selectById(id);
        if (di == null) throw new BizException(404, "不良品项不存在");
        di.setStatus(status);
        defectItemMapper.updateById(di);
    }

    @Transactional
    public void delete(Long id) {
        defectItemMapper.deleteById(id);
    }
}
