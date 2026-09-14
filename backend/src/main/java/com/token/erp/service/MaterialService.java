package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Material;
import com.token.erp.entity.Unit;
import com.token.erp.mapper.MaterialMapper;
import com.token.erp.mapper.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final UnitMapper unitMapper;
    private final CodeGenerator codeGenerator;

    public Page<Material> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Material::getName, keyword)
                    .or().like(Material::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Material::getStatus, status);
        }
        qw.orderByDesc(Material::getId);
        return materialMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Material create(Material material) {
        // 必填：编号(自动)、名称、分类、单位、状态
        if (!StringUtils.hasText(material.getName())) {
            throw new BizException("物料名称不能为空");
        }
        if (!StringUtils.hasText(material.getCategory())) {
            throw new BizException("物料分类不能为空");
        }
        if (material.getUnitId() == null) {
            throw new BizException("单位不能为空");
        }
        // 单位快照缺失时按 unitId 回填，保证 unit 列非空
        if (!StringUtils.hasText(material.getUnit())) {
            Unit u = unitMapper.selectById(material.getUnitId());
            if (u != null) material.setUnit(u.getName());
        }
        material.setId(null);
        material.setCode(codeGenerator.next("WL"));
        material.setStatus(material.getStatus() == null ? 1 : material.getStatus());
        material.setSupplyType(StringUtils.hasText(material.getSupplyType()) ? material.getSupplyType() : "purchase");
        material.setBatchManage(StringUtils.hasText(material.getBatchManage()) ? material.getBatchManage() : "none");
        materialMapper.insert(material);
        return material;
    }

    public Material update(Material material) {
        if (materialMapper.selectById(material.getId()) == null) {
            throw new BizException(404, "物料不存在");
        }
        material.setCode(null);
        materialMapper.updateById(material);
        return materialMapper.selectById(material.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (materialMapper.selectById(id) == null) {
            throw new BizException(404, "物料不存在");
        }
        Material upd = new Material();
        upd.setId(id);
        upd.setStatus(status);
        materialMapper.updateById(upd);
    }
}
