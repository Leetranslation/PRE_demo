package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.SalesStockingRequest;
import com.token.erp.entity.SalesStocking;
import com.token.erp.entity.SalesStockingItem;
import com.token.erp.mapper.SalesStockingItemMapper;
import com.token.erp.mapper.SalesStockingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesStockingService {

    private final SalesStockingMapper stockingMapper;
    private final SalesStockingItemMapper itemMapper;
    private final CodeGenerator codeGenerator;

    public Page<SalesStocking> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<SalesStocking> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) qw.like(SalesStocking::getStockingNo, keyword);
        if (StringUtils.hasText(approveStatus)) qw.eq(SalesStocking::getApproveStatus, approveStatus);
        qw.orderByDesc(SalesStocking::getId);
        return stockingMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public List<SalesStockingItem> items(Long stockingId) {
        return itemMapper.selectList(
                new LambdaQueryWrapper<SalesStockingItem>().eq(SalesStockingItem::getStockingId, stockingId));
    }

    @Transactional
    public SalesStocking create(SalesStockingRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("备货明细不能为空");
        SalesStocking stocking = new SalesStocking();
        stocking.setStockingNo(codeGenerator.next("BH"));
        stocking.setOrderId(request.getOrderId());
        stocking.setApproveStatus("pending");
        stocking.setRemark(request.getRemark());
        stockingMapper.insert(stocking);

        for (SalesStockingRequest.Item item : request.getItems()) {
            if (item.getProductId() == null) throw new BizException("产品不能为空");
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("数量必须大于0");
            }
            SalesStockingItem si = new SalesStockingItem();
            si.setStockingId(stocking.getId());
            si.setProductId(item.getProductId());
            si.setQuantity(item.getQuantity());
            si.setUnit(item.getUnit());
            si.setRemark(item.getRemark());
            itemMapper.insert(si);
        }
        return stocking;
    }

    @Transactional
    public void submit(Long id) {
        SalesStocking s = stockingMapper.selectById(id);
        if (s == null) throw new BizException(404, "备货单不存在");
        if (!"pending".equals(s.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        s.setApproveStatus("approving");
        stockingMapper.updateById(s);
    }

    @Transactional
    public void approve(Long id) {
        SalesStocking s = stockingMapper.selectById(id);
        if (s == null) throw new BizException(404, "备货单不存在");
        if (!"approving".equals(s.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        s.setApproveStatus("approved");
        stockingMapper.updateById(s);
    }
}
