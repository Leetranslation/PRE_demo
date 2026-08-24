package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Warehouse;
import com.token.erp.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public Page<Warehouse> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Warehouse> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Warehouse::getName, keyword)
                    .or().like(Warehouse::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Warehouse::getStatus, status);
        }
        qw.orderByDesc(Warehouse::getId);
        return warehouseMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Warehouse create(Warehouse warehouse) {
        if (!StringUtils.hasText(warehouse.getName())) {
            throw new BizException("仓库名称不能为空");
        }
        if (!StringUtils.hasText(warehouse.getCode())) {
            throw new BizException("仓库编号不能为空");
        }
        warehouse.setId(null);
        warehouse.setStatus(warehouse.getStatus() == null ? 1 : warehouse.getStatus());
        warehouseMapper.insert(warehouse);
        return warehouse;
    }

    public Warehouse update(Warehouse warehouse) {
        if (warehouseMapper.selectById(warehouse.getId()) == null) {
            throw new BizException(404, "仓库不存在");
        }
        warehouseMapper.updateById(warehouse);
        return warehouseMapper.selectById(warehouse.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (warehouseMapper.selectById(id) == null) {
            throw new BizException(404, "仓库不存在");
        }
        Warehouse upd = new Warehouse();
        upd.setId(id);
        upd.setStatus(status);
        warehouseMapper.updateById(upd);
    }
}
