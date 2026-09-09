package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Area;
import com.token.erp.mapper.AreaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaMapper areaMapper;

    public Page<Area> page(long page, long pageSize, String keyword, Integer status, Long warehouseId) {
        LambdaQueryWrapper<Area> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Area::getName, keyword).or().like(Area::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Area::getStatus, status);
        }
        if (warehouseId != null) {
            qw.eq(Area::getWarehouseId, warehouseId);
        }
        qw.orderByDesc(Area::getId);
        return areaMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Area create(Area area) {
        if (!StringUtils.hasText(area.getName())) {
            throw new BizException("库区名称不能为空");
        }
        if (area.getWarehouseId() == null) {
            throw new BizException("所属仓库不能为空");
        }
        area.setId(null);
        if (!StringUtils.hasText(area.getCode())) {
            area.setCode("KQ" + System.currentTimeMillis());
        }
        area.setStatus(area.getStatus() == null ? 1 : area.getStatus());
        areaMapper.insert(area);
        return area;
    }

    public Area update(Area area) {
        if (areaMapper.selectById(area.getId()) == null) {
            throw new BizException(404, "库区不存在");
        }
        area.setCode(null);
        areaMapper.updateById(area);
        return areaMapper.selectById(area.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (areaMapper.selectById(id) == null) {
            throw new BizException(404, "库区不存在");
        }
        Area upd = new Area();
        upd.setId(id);
        upd.setStatus(status);
        areaMapper.updateById(upd);
    }
}
