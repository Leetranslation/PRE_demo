package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Supplier;
import com.token.erp.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final CodeGenerator codeGenerator;

    public Page<Supplier> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Supplier::getName, keyword)
                    .or().like(Supplier::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Supplier::getStatus, status);
        }
        qw.orderByDesc(Supplier::getId);
        return supplierMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Supplier create(Supplier supplier) {
        if (!StringUtils.hasText(supplier.getName())) {
            throw new BizException("供应商名称不能为空");
        }
        if (!StringUtils.hasText(supplier.getCategory())) {
            throw new BizException("供应商分类不能为空");
        }
        supplier.setId(null);
        supplier.setCode(codeGenerator.next("GY"));
        supplier.setStatus(supplier.getStatus() == null ? 1 : supplier.getStatus());
        supplierMapper.insert(supplier);
        return supplier;
    }

    public Supplier update(Supplier supplier) {
        Supplier db = supplierMapper.selectById(supplier.getId());
        if (db == null) {
            throw new BizException(404, "供应商不存在");
        }
        supplier.setCode(null);
        supplierMapper.updateById(supplier);
        return supplierMapper.selectById(supplier.getId());
    }

    public void changeStatus(Long id, Integer status) {
        Supplier db = supplierMapper.selectById(id);
        if (db == null) {
            throw new BizException(404, "供应商不存在");
        }
        Supplier upd = new Supplier();
        upd.setId(id);
        upd.setStatus(status);
        supplierMapper.updateById(upd);
    }
}
