package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.CustomerPrice;
import com.token.erp.mapper.CustomerPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 客户价格资料
 */
@Service
@RequiredArgsConstructor
public class CustomerPriceService {

    private final CustomerPriceMapper customerPriceMapper;

    public Page<CustomerPrice> page(long page, long pageSize, Long customerId, String itemType, Integer status) {
        LambdaQueryWrapper<CustomerPrice> qw = new LambdaQueryWrapper<>();
        if (customerId != null) qw.eq(CustomerPrice::getCustomerId, customerId);
        if (itemType != null && !itemType.isEmpty()) qw.eq(CustomerPrice::getItemType, itemType);
        if (status != null) qw.eq(CustomerPrice::getStatus, status);
        qw.orderByDesc(CustomerPrice::getId);
        return customerPriceMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public CustomerPrice create(CustomerPrice cp) {
        validate(cp);
        cp.setId(null);
        if (cp.getPrice() == null) cp.setPrice(BigDecimal.ZERO);
        if (cp.getStatus() == null) cp.setStatus(1);
        try {
            customerPriceMapper.insert(cp);
        } catch (DuplicateKeyException e) {
            throw new BizException("该客户对同一产品/物料已存在价格记录，请编辑而非新增");
        }
        return cp;
    }

    @Transactional
    public CustomerPrice update(CustomerPrice cp) {
        if (cp.getId() == null) throw new BizException("ID不能为空");
        validate(cp);
        customerPriceMapper.updateById(cp);
        return cp;
    }

    @Transactional
    public void status(Long id, Integer status) {
        CustomerPrice cp = customerPriceMapper.selectById(id);
        if (cp == null) throw new BizException(404, "价格记录不存在");
        cp.setStatus(status);
        customerPriceMapper.updateById(cp);
    }

    @Transactional
    public void delete(Long id) {
        customerPriceMapper.deleteById(id);
    }

    private void validate(CustomerPrice cp) {
        if (cp.getCustomerId() == null) throw new BizException("客户不能为空");
        if (cp.getItemId() == null) throw new BizException("产品/物料不能为空");
        if (cp.getItemType() == null) cp.setItemType("product");
        if (cp.getPrice() != null && cp.getPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new BizException("价格不能为负数");
    }
}
