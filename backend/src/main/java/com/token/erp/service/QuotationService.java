package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.QuotationRequest;
import com.token.erp.dto.SalesOrderRequest;
import com.token.erp.entity.Quotation;
import com.token.erp.entity.QuotationItem;
import com.token.erp.entity.SalesOrder;
import com.token.erp.entity.SalesOrderItem;
import com.token.erp.mapper.QuotationItemMapper;
import com.token.erp.mapper.QuotationMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationMapper quotationMapper;
    private final QuotationItemMapper itemMapper;
    private final SalesOrderService salesOrderService;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;
    private final NameFillUtil nameFillUtil;

    public Page<Quotation> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<Quotation> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Quotation::getQuotationNo, keyword);
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(Quotation::getApproveStatus, approveStatus);
        }
        qw.orderByDesc(Quotation::getId);
        Page<Quotation> result = quotationMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            List<Long> qids = result.getRecords().stream().map(Quotation::getId).collect(Collectors.toList());
            List<QuotationItem> allItems = itemMapper.selectList(
                    new LambdaQueryWrapper<QuotationItem>()
                            .in(QuotationItem::getQuotationId, qids)
                            .orderByAsc(QuotationItem::getId));
            Map<Long, String[]> pm = nameFillUtil.productMap(
                    allItems.stream().map(QuotationItem::getProductId).distinct().collect(Collectors.toList()));
            Map<Long, List<QuotationItem>> grouped = allItems.stream()
                    .collect(Collectors.groupingBy(QuotationItem::getQuotationId));
            result.getRecords().forEach(q -> {
                List<QuotationItem> its = grouped.get(q.getId());
                if (its == null || its.isEmpty()) return;
                String[] arr = pm.get(its.get(0).getProductId());
                long kinds = its.stream().map(QuotationItem::getProductId).distinct().count();
                if (arr != null) {
                    q.setProductCode(arr[0] + (kinds > 1 ? " 等" + kinds + "种" : ""));
                    q.setProductName(arr[1]);
                }
            });
        }
        return result;
    }

    public List<QuotationItem> items(Long quotationId) {
        return itemMapper.selectList(
                new LambdaQueryWrapper<QuotationItem>().eq(QuotationItem::getQuotationId, quotationId));
    }

    @Transactional
    public Quotation create(QuotationRequest request) {
        if (request.getCustomerId() == null) throw new BizException("客户不能为空");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("报价明细不能为空");

        Quotation quotation = new Quotation();
        quotation.setQuotationNo(codeGenerator.next("BJ"));
        quotation.setCustomerId(request.getCustomerId());
        quotation.setQuoteDate(request.getQuoteDate() != null ? request.getQuoteDate() : LocalDate.now());
        quotation.setValidUntil(request.getValidUntil());
        quotation.setSalesman(request.getSalesman());
        quotation.setApproveStatus("pending");
        quotation.setBizStatus("pending");
        quotation.setTotalAmount(BigDecimal.ZERO);
        quotation.setRemark(request.getRemark());
        quotationMapper.insert(quotation);

        BigDecimal total = saveItems(quotation.getId(), request.getItems());
        quotation.setTotalAmount(total);
        quotationMapper.updateById(quotation);
        return quotation;
    }

    @Transactional
    public void submit(Long id) {
        Quotation q = quotationMapper.selectById(id);
        if (q == null) throw new BizException(404, "报价单不存在");
        if (!"pending".equals(q.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        q.setApproveStatus("approving");
        quotationMapper.updateById(q);
    }

    @Transactional
    public void approve(Long id) {
        Quotation q = quotationMapper.selectById(id);
        if (q == null) throw new BizException(404, "报价单不存在");
        if (!"approving".equals(q.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        checkSelfApprove(q.getCreateBy());
        q.setApproveStatus("approved");
        quotationMapper.updateById(q);
    }

    @Transactional
    public void unApprove(Long id) {
        Quotation q = quotationMapper.selectById(id);
        if (q == null) throw new BizException(404, "报价单不存在");
        if (!"approved".equals(q.getApproveStatus())) throw new BizException("仅已审批可反审核");
        if ("converted".equals(q.getBizStatus())) throw new BizException("已转销售订单的报价单不能反审核");
        q.setApproveStatus("pending");
        quotationMapper.updateById(q);
    }

    /** 报价单转销售订单 */
    @Transactional
    public SalesOrder convertToSalesOrder(Long id) {
        Quotation q = quotationMapper.selectById(id);
        if (q == null) throw new BizException(404, "报价单不存在");
        if (!"approved".equals(q.getApproveStatus())) throw new BizException("仅已审批的报价单可转销售订单");
        if ("converted".equals(q.getBizStatus())) throw new BizException("该报价单已转销售订单");

        List<QuotationItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<QuotationItem>().eq(QuotationItem::getQuotationId, id));

        SalesOrderRequest req = new SalesOrderRequest();
        req.setCustomerId(q.getCustomerId());
        req.setSalesman(q.getSalesman());
        List<SalesOrderItem> soItems = new ArrayList<>();
        for (QuotationItem it : items) {
            SalesOrderItem soItem = new SalesOrderItem();
            soItem.setProductId(it.getProductId());
            soItem.setSpec(it.getSpec());
            soItem.setUnit(it.getUnit());
            soItem.setQuantity(it.getQuantity());
            soItem.setPrice(it.getPrice());
            soItem.setTaxRate(it.getTaxRate());
            soItem.setRemark(it.getRemark());
            soItems.add(soItem);
        }
        req.setItems(soItems);

        SalesOrder order = salesOrderService.create(req);

        q.setBizStatus("converted");
        quotationMapper.updateById(q);
        return order;
    }

    private void checkSelfApprove(String createBy) {
        if (!sysConfigService.getBoolean("allow_self_approve")) {
            String currentUser = securityUtils.currentUsername();
            if (createBy != null && createBy.equals(currentUser)) {
                throw new BizException("禁止自审：不能审批自己提交的单据");
            }
        }
    }

    private BigDecimal saveItems(Long quotationId, List<QuotationRequest.Item> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (QuotationRequest.Item item : items) {
            if (item.getProductId() == null) throw new BizException("产品不能为空");
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            BigDecimal taxRate = item.getTaxRate() == null ? new BigDecimal("13") : item.getTaxRate();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("数量必须大于0");
            if (price.compareTo(BigDecimal.ZERO) < 0) throw new BizException("单价不能为负数");
            if (taxRate.compareTo(BigDecimal.ZERO) < 0) throw new BizException("税率不能为负数");

            BigDecimal totalAmount = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal divisor = BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
            BigDecimal amountExTax = totalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = totalAmount.subtract(amountExTax);

            QuotationItem qi = new QuotationItem();
            qi.setQuotationId(quotationId);
            qi.setProductId(item.getProductId());
            qi.setSpec(item.getSpec());
            qi.setUnit(item.getUnit());
            qi.setQuantity(qty);
            qi.setPrice(price);
            qi.setTaxRate(taxRate);
            qi.setAmountExTax(amountExTax);
            qi.setTaxAmount(taxAmount);
            qi.setTotalAmount(totalAmount);
            qi.setRemark(item.getRemark());
            itemMapper.insert(qi);

            total = total.add(totalAmount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
