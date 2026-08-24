package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Customer;
import com.token.erp.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final CodeGenerator codeGenerator;

    public Page<Customer> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Customer::getName, keyword)
                    .or().like(Customer::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Customer::getStatus, status);
        }
        qw.orderByDesc(Customer::getId);
        return customerMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Customer create(Customer customer) {
        if (!StringUtils.hasText(customer.getName())) {
            throw new BizException("客户名称不能为空");
        }
        if (!StringUtils.hasText(customer.getCategory())) {
            throw new BizException("客户分类不能为空");
        }
        customer.setId(null);
        customer.setCode(codeGenerator.next("KH"));
        customer.setStatus(customer.getStatus() == null ? 1 : customer.getStatus());
        customerMapper.insert(customer);
        return customer;
    }

    public Customer update(Customer customer) {
        Customer db = customerMapper.selectById(customer.getId());
        if (db == null) {
            throw new BizException(404, "客户不存在");
        }
        customer.setCode(null); // 编号不可修改
        customerMapper.updateById(customer);
        return customerMapper.selectById(customer.getId());
    }

    public void changeStatus(Long id, Integer status) {
        Customer db = customerMapper.selectById(id);
        if (db == null) {
            throw new BizException(404, "客户不存在");
        }
        Customer upd = new Customer();
        upd.setId(id);
        upd.setStatus(status);
        customerMapper.updateById(upd);
    }
}
