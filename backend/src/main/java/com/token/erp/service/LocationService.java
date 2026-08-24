package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Location;
import com.token.erp.mapper.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationMapper locationMapper;

    public Page<Location> page(long page, long pageSize, Long warehouseId, String keyword) {
        LambdaQueryWrapper<Location> qw = new LambdaQueryWrapper<>();
        if (warehouseId != null) qw.eq(Location::getWarehouseId, warehouseId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Location::getName, keyword).or().like(Location::getCode, keyword));
        }
        qw.orderByDesc(Location::getId);
        return locationMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 按仓库查询库位（供下拉框） */
    public List<Location> listByWarehouse(Long warehouseId) {
        return locationMapper.selectList(
                new LambdaQueryWrapper<Location>()
                        .eq(Location::getWarehouseId, warehouseId)
                        .eq(Location::getStatus, 1)
                        .orderByAsc(Location::getId));
    }

    public Location create(Location location) {
        if (location.getWarehouseId() == null) throw new BizException("仓库不能为空");
        if (!StringUtils.hasText(location.getCode())) throw new BizException("库位编号不能为空");
        location.setId(null);
        location.setStatus(location.getStatus() == null ? 1 : location.getStatus());
        locationMapper.insert(location);
        return location;
    }

    public Location update(Location location) {
        if (locationMapper.selectById(location.getId()) == null) throw new BizException(404, "库位不存在");
        locationMapper.updateById(location);
        return locationMapper.selectById(location.getId());
    }
}
