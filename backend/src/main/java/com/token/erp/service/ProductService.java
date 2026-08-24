package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Product;
import com.token.erp.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CodeGenerator codeGenerator;

    public Page<Product> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Product::getStatus, status);
        }
        qw.orderByDesc(Product::getId);
        return productMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Product create(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            throw new BizException("产品名称不能为空");
        }
        if (!StringUtils.hasText(product.getCategory())) {
            throw new BizException("产品分类不能为空");
        }
        if (!StringUtils.hasText(product.getUnit())) {
            throw new BizException("单位不能为空");
        }
        product.setId(null);
        product.setCode(codeGenerator.next("CP"));
        product.setStatus(product.getStatus() == null ? 1 : product.getStatus());
        product.setBatchManage(StringUtils.hasText(product.getBatchManage()) ? product.getBatchManage() : "none");
        productMapper.insert(product);
        return product;
    }

    public Product update(Product product) {
        if (productMapper.selectById(product.getId()) == null) {
            throw new BizException(404, "产品不存在");
        }
        product.setCode(null);
        productMapper.updateById(product);
        return productMapper.selectById(product.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (productMapper.selectById(id) == null) {
            throw new BizException(404, "产品不存在");
        }
        Product upd = new Product();
        upd.setId(id);
        upd.setStatus(status);
        productMapper.updateById(upd);
    }
}
