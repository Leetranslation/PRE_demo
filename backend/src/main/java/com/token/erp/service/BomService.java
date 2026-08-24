package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.BomRequest;
import com.token.erp.entity.Bom;
import com.token.erp.entity.BomItem;
import com.token.erp.entity.BomVersion;
import com.token.erp.mapper.BomItemMapper;
import com.token.erp.mapper.BomMapper;
import com.token.erp.mapper.BomVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BomService {

    private final BomMapper bomMapper;
    private final BomItemMapper bomItemMapper;
    private final BomVersionMapper bomVersionMapper;
    private final CodeGenerator codeGenerator;

    public Page<Bom> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Bom> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Bom::getName, keyword)
                    .or().like(Bom::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Bom::getStatus, status);
        }
        qw.orderByDesc(Bom::getId);
        return bomMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 查询 BOM 明细（含版本） */
    public List<BomItem> items(Long bomId) {
        return bomItemMapper.selectList(
                new LambdaQueryWrapper<BomItem>()
                        .eq(BomItem::getBomId, bomId)
                        .orderByAsc(BomItem::getSort));
    }

    public List<BomVersion> versions(Long bomId) {
        return bomVersionMapper.selectList(
                new LambdaQueryWrapper<BomVersion>()
                        .eq(BomVersion::getBomId, bomId)
                        .orderByDesc(BomVersion::getId));
    }

    @Transactional
    public Bom create(BomRequest request) {
        if (request.getProductId() == null) {
            throw new BizException("产品不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("BOM 明细不能为空");
        }
        Bom bom = new Bom();
        bom.setCode(codeGenerator.next("BOM"));
        bom.setProductId(request.getProductId());
        bom.setName(request.getName());
        bom.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        bom.setRemark(request.getRemark());
        bomMapper.insert(bom);

        // 创建版本
        BomVersion version = new BomVersion();
        version.setBomId(bom.getId());
        version.setVersion(StringUtils.hasText(request.getVersion()) ? request.getVersion() : "V1.0");
        version.setEffectiveDate(LocalDate.now());
        version.setStatus(1);
        bomVersionMapper.insert(version);

        // 保存明细
        saveItems(bom.getId(), version.getId(), request.getItems());
        return bom;
    }

    @Transactional
    public Bom update(BomRequest request) {
        if (request.getId() == null || bomMapper.selectById(request.getId()) == null) {
            throw new BizException(404, "BOM 不存在");
        }
        Bom bom = new Bom();
        bom.setId(request.getId());
        bom.setName(request.getName());
        bom.setRemark(request.getRemark());
        bomMapper.updateById(bom);

        // 新版本（修改明细生成新版本，保留历史）
        BomVersion version = new BomVersion();
        version.setBomId(request.getId());
        version.setVersion(StringUtils.hasText(request.getVersion()) ? request.getVersion() : "V" + (System.currentTimeMillis() % 1000));
        version.setEffectiveDate(LocalDate.now());
        version.setStatus(1);
        bomVersionMapper.insert(version);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            saveItems(request.getId(), version.getId(), request.getItems());
        }
        return bomMapper.selectById(request.getId());
    }

    private void saveItems(Long bomId, Long versionId, List<BomItem> items) {
        int sort = 1;
        for (BomItem item : items) {
            item.setId(null);
            item.setBomId(bomId);
            item.setBomVersionId(versionId);
            if (item.getSort() == null) {
                item.setSort(sort);
            }
            sort++;
            bomItemMapper.insert(item);
        }
    }
}
