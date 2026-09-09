package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.AfterSale;
import com.token.erp.entity.SalesOrder;
import com.token.erp.mapper.AfterSaleMapper;
import com.token.erp.mapper.SalesOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final SalesOrderMapper orderMapper;
    private final CodeGenerator codeGenerator;

    public Page<AfterSale> page(long page, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<AfterSale> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) qw.like(AfterSale::getAfterSaleNo, keyword);
        if (StringUtils.hasText(status)) qw.eq(AfterSale::getStatus, status);
        qw.orderByDesc(AfterSale::getId);
        return afterSaleMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public AfterSale create(AfterSale afterSale) {
        if (afterSale.getOrderId() == null) throw new BizException("销售订单不能为空");
        SalesOrder order = orderMapper.selectById(afterSale.getOrderId());
        if (order == null) throw new BizException(404, "销售订单不存在");
        afterSale.setId(null);
        afterSale.setAfterSaleNo(codeGenerator.next("SH"));
        afterSale.setCustomerId(order.getCustomerId());
        afterSale.setStatus("pending");
        afterSaleMapper.insert(afterSale);
        return afterSale;
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) throw new BizException(404, "售后单不存在");
        as.setStatus(status);
        afterSaleMapper.updateById(as);
    }
}
