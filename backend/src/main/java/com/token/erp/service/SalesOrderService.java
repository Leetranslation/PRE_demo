package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.SalesOrderRequest;
import com.token.erp.entity.SalesOrder;
import com.token.erp.entity.SalesOrderItem;
import com.token.erp.mapper.SalesOrderItemMapper;
import com.token.erp.mapper.SalesOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper itemMapper;
    private final CodeGenerator codeGenerator;

    public Page<SalesOrder> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<SalesOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(SalesOrder::getOrderNo, keyword);
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(SalesOrder::getApproveStatus, approveStatus);
        }
        qw.orderByDesc(SalesOrder::getId);
        return orderMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public List<SalesOrderItem> items(Long orderId) {
        return itemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, orderId));
    }

    @Transactional
    public SalesOrder create(SalesOrderRequest request) {
        if (request.getCustomerId() == null) throw new BizException("客户不能为空");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("销售明细不能为空");

        SalesOrder order = new SalesOrder();
        order.setOrderNo(codeGenerator.next("SO"));
        order.setCustomerId(request.getCustomerId());
        order.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now());
        order.setSalesType(StringUtils.hasText(request.getSalesType()) ? request.getSalesType() : "normal");
        order.setContact(request.getContact());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setSalesman(request.getSalesman());
        order.setDepartmentId(request.getDepartmentId());
        order.setApproveStatus("pending");
        order.setBizStatus("pending_outbound");
        order.setRemark(request.getRemark());
        order.setTotalAmount(BigDecimal.ZERO);
        orderMapper.insert(order);

        BigDecimal total = saveItems(order.getId(), request.getItems());
        order.setTotalAmount(total);
        orderMapper.updateById(order);
        return order;
    }

    @Transactional
    public void submit(Long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "销售订单不存在");
        if (!"pending".equals(order.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        order.setApproveStatus("approving");
        orderMapper.updateById(order);
    }

    @Transactional
    public void approve(Long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "销售订单不存在");
        if (!"approving".equals(order.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        order.setApproveStatus("approved");
        orderMapper.updateById(order);
    }

    @Transactional
    public void unApprove(Long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "销售订单不存在");
        if (!"approved".equals(order.getApproveStatus())) throw new BizException("仅已审批可反审核");
        if (!"pending_outbound".equals(order.getBizStatus())) {
            throw new BizException("已产生出库的订单不能反审核，需先处理下游业务");
        }
        order.setApproveStatus("pending");
        orderMapper.updateById(order);
    }

    /** 保存明细并计算金额（差额法倒挤税额） */
    private BigDecimal saveItems(Long orderId, List<SalesOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItem item : items) {
            if (item.getProductId() == null) throw new BizException("产品不能为空");
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            BigDecimal taxRate = item.getTaxRate() == null ? new BigDecimal("13") : item.getTaxRate();

            BigDecimal totalAmount = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal divisor = BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
            BigDecimal amountExTax = totalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = totalAmount.subtract(amountExTax);

            item.setId(null);
            item.setOrderId(orderId);
            item.setQuantity(qty);
            item.setPrice(price);
            item.setTaxRate(taxRate);
            item.setAmountExTax(amountExTax);
            item.setTaxAmount(taxAmount);
            item.setTotalAmount(totalAmount);
            item.setOutboundQty(item.getOutboundQty() == null ? BigDecimal.ZERO : item.getOutboundQty());
            itemMapper.insert(item);

            total = total.add(totalAmount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
