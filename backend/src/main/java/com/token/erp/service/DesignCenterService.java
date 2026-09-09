package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.DesignCenter;
import com.token.erp.mapper.DesignCenterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DesignCenterService {

    private final DesignCenterMapper designCenterMapper;
    private final CodeGenerator codeGenerator;

    public Page<DesignCenter> page(long page, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<DesignCenter> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) qw.like(DesignCenter::getDesignNo, keyword);
        if (StringUtils.hasText(status)) qw.eq(DesignCenter::getStatus, status);
        qw.orderByDesc(DesignCenter::getId);
        return designCenterMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public DesignCenter create(DesignCenter design) {
        if (design.getProductId() == null) throw new BizException("产品不能为空");
        design.setId(null);
        design.setDesignNo(codeGenerator.next("SJ"));
        design.setStatus("designing");
        design.setApproveStatus("pending");
        designCenterMapper.insert(design);
        return design;
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        DesignCenter d = designCenterMapper.selectById(id);
        if (d == null) throw new BizException(404, "设计单不存在");
        d.setStatus(status);
        designCenterMapper.updateById(d);
    }
}
