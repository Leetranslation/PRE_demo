package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.ProcessRequest;
import com.token.erp.entity.Process;
import com.token.erp.mapper.ProcessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessMapper processMapper;
    private final CodeGenerator codeGenerator;

    public Page<Process> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Process> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Process::getName, keyword)
                    .or().like(Process::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Process::getStatus, status);
        }
        qw.orderByDesc(Process::getId);
        return processMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Process create(ProcessRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException("工序名称不能为空");
        }
        if (!StringUtils.hasText(req.getPriceType())) {
            throw new BizException("计价方式不能为空");
        }
        Process p = new Process();
        p.setId(null);
        p.setCode(codeGenerator.next("GX"));
        p.setName(req.getName());
        p.setCategory(req.getCategory());
        p.setStandardPrice(req.getStandardPrice());
        p.setPriceType(req.getPriceType());
        p.setDescription(req.getDescription());
        p.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        p.setRemark(req.getRemark());
        processMapper.insert(p);
        return p;
    }

    public Process update(ProcessRequest req) {
        Process existing = processMapper.selectById(req.getId());
        if (existing == null) {
            throw new BizException(404, "工序不存在");
        }
        if (StringUtils.hasText(req.getName())) {
            existing.setName(req.getName());
        }
        existing.setCategory(req.getCategory());
        existing.setStandardPrice(req.getStandardPrice());
        if (StringUtils.hasText(req.getPriceType())) {
            existing.setPriceType(req.getPriceType());
        }
        existing.setDescription(req.getDescription());
        existing.setRemark(req.getRemark());
        // 编号不可修改
        processMapper.updateById(existing);
        return processMapper.selectById(existing.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (processMapper.selectById(id) == null) {
            throw new BizException(404, "工序不存在");
        }
        Process upd = new Process();
        upd.setId(id);
        upd.setStatus(status);
        processMapper.updateById(upd);
    }

    /**
     * 批量设置标准单价
     */
    public void batchSetPrice(List<Long> ids, BigDecimal price) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BizException("请选择要设置的工序");
        }
        if (price == null) {
            throw new BizException("标准单价不能为空");
        }
        processMapper.update(null, new LambdaUpdateWrapper<Process>()
                .in(Process::getId, ids)
                .set(Process::getStandardPrice, price));
    }

    /**
     * 批量设置计价方式
     */
    public void batchSetPriceType(List<Long> ids, String priceType) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BizException("请选择要设置的工序");
        }
        if (!StringUtils.hasText(priceType)) {
            throw new BizException("计价方式不能为空");
        }
        processMapper.update(null, new LambdaUpdateWrapper<Process>()
                .in(Process::getId, ids)
                .set(Process::getPriceType, priceType));
    }
}
