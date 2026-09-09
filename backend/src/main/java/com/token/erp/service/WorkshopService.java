package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Workshop;
import com.token.erp.mapper.WorkshopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WorkshopService {

    private final WorkshopMapper workshopMapper;
    private final CodeGenerator codeGenerator;

    public Page<Workshop> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Workshop> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Workshop::getName, keyword).or().like(Workshop::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Workshop::getStatus, status);
        }
        qw.orderByDesc(Workshop::getId);
        return workshopMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Workshop create(Workshop workshop) {
        if (!StringUtils.hasText(workshop.getName())) {
            throw new BizException("车间名称不能为空");
        }
        workshop.setId(null);
        workshop.setCode(codeGenerator.next("CJ"));
        workshop.setStatus(workshop.getStatus() == null ? 1 : workshop.getStatus());
        workshopMapper.insert(workshop);
        return workshop;
    }

    public Workshop update(Workshop workshop) {
        if (workshopMapper.selectById(workshop.getId()) == null) {
            throw new BizException(404, "车间不存在");
        }
        workshop.setCode(null);
        workshopMapper.updateById(workshop);
        return workshopMapper.selectById(workshop.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (workshopMapper.selectById(id) == null) {
            throw new BizException(404, "车间不存在");
        }
        Workshop upd = new Workshop();
        upd.setId(id);
        upd.setStatus(status);
        workshopMapper.updateById(upd);
    }
}
