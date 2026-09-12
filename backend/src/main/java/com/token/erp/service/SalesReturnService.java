package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.SalesReturnRequest;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.SalesOrder;
import com.token.erp.entity.SalesOrderItem;
import com.token.erp.entity.SalesReturn;
import com.token.erp.entity.SalesReturnItem;
import com.token.erp.mapper.SalesOrderItemMapper;
import com.token.erp.mapper.SalesOrderMapper;
import com.token.erp.mapper.SalesReturnItemMapper;
import com.token.erp.mapper.SalesReturnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 销售退货服务：客户退货，库存【入库】(in)
 */
@Service
@RequiredArgsConstructor
public class SalesReturnService {

    private final SalesReturnMapper returnMapper;
    private final SalesReturnItemMapper returnItemMapper;
    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final IdempotencyService idempotencyService;
    private final NameFillUtil nameFillUtil;

    public Page<SalesReturn> page(long page, long pageSize, Long orderId) {
        LambdaQueryWrapper<SalesReturn> qw = new LambdaQueryWrapper<>();
        if (orderId != null) qw.eq(SalesReturn::getOrderId, orderId);
        qw.orderByDesc(SalesReturn::getId);
        Page<SalesReturn> result = returnMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> om = nameFillUtil.salesOrderNoMap(
                    result.getRecords().stream().map(SalesReturn::getOrderId).collect(Collectors.toList()));
            result.getRecords().forEach(r -> r.setSourceOrderNo(om.get(r.getOrderId())));
        }
        return result;
    }

    public List<SalesReturnItem> items(Long returnId) {
        List<SalesReturnItem> list = returnItemMapper.selectList(
                new LambdaQueryWrapper<SalesReturnItem>().eq(SalesReturnItem::getReturnId, returnId));
        if (!list.isEmpty()) {
            Map<Long, String[]> pm = nameFillUtil.productMap(
                    list.stream().map(SalesReturnItem::getProductId).collect(Collectors.toList()));
            list.forEach(i -> {
                String[] arr = pm.get(i.getProductId());
                if (arr != null) {
                    i.setProductCode(arr[0]);
                    i.setProductName(arr[1]);
                }
            });
        }
        return list;
    }

    @Transactional
    public SalesReturn create(SalesReturnRequest request) {
        // 幂等控制：防止重复提交
        idempotencyService.checkAndOccupy(request.getRequestId(), "sales_return");
        SalesOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BizException(404, "销售订单不存在");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("退货明细不能为空");

        SalesReturn sr = new SalesReturn();
        sr.setReturnNo(codeGenerator.next("TH"));
        sr.setOrderId(request.getOrderId());
        sr.setCustomerId(order.getCustomerId());
        sr.setWarehouseId(request.getWarehouseId());
        sr.setReturnDate(LocalDate.now());
        sr.setStatus("completed");
        sr.setRemark(request.getRemark());
        returnMapper.insert(sr);

        for (SalesReturnRequest.Item item : request.getItems()) {
            if (item.getProductId() == null) throw new BizException("产品不能为空");
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("退货数量必须大于0");
            }

            // 校验退货量 ≤ 已出库量（未出库的不能退，防止凭空造库存）
            SalesOrderItem orderItem = orderItemMapper.selectOne(
                    new LambdaQueryWrapper<SalesOrderItem>()
                            .eq(SalesOrderItem::getOrderId, request.getOrderId())
                            .eq(SalesOrderItem::getProductId, item.getProductId()));
            if (orderItem == null) throw new BizException("订单中不存在该产品");
            BigDecimal outboundQty = orderItem.getOutboundQty() == null ? BigDecimal.ZERO : orderItem.getOutboundQty();
            BigDecimal returnedQty = sumReturnedQty(request.getOrderId(), item.getProductId());
            BigDecimal availableToReturn = outboundQty.subtract(returnedQty);
            if (item.getQuantity().compareTo(availableToReturn) > 0) {
                throw new BizException("退货数量超过已出库数量，当前可退：" + availableToReturn);
            }

            // 销售退货 = 库存入库（in）
            Inventory dim = new Inventory();
            dim.setItemId(item.getProductId());
            dim.setItemType("product");
            dim.setWarehouseId(request.getWarehouseId());
            dim.setStockType("available");
            inventoryService.change(dim, "in", item.getQuantity(), "sales_return", sr.getId(), sr.getReturnNo());

            SalesReturnItem ri = new SalesReturnItem();
            ri.setReturnId(sr.getId());
            ri.setProductId(item.getProductId());
            ri.setQuantity(item.getQuantity());
            ri.setUnit(item.getUnit());
            ri.setRemark(item.getRemark());
            returnItemMapper.insert(ri);
        }
        return sr;
    }

    /** 该订单该产品已退货数量合计 */
    private BigDecimal sumReturnedQty(Long orderId, Long productId) {
        List<SalesReturn> returns = returnMapper.selectList(
                new LambdaQueryWrapper<SalesReturn>().eq(SalesReturn::getOrderId, orderId));
        BigDecimal total = BigDecimal.ZERO;
        for (SalesReturn r : returns) {
            List<SalesReturnItem> items = returnItemMapper.selectList(
                    new LambdaQueryWrapper<SalesReturnItem>()
                            .eq(SalesReturnItem::getReturnId, r.getId())
                            .eq(SalesReturnItem::getProductId, productId));
            for (SalesReturnItem ri : items) {
                total = total.add(ri.getQuantity() == null ? BigDecimal.ZERO : ri.getQuantity());
            }
        }
        return total;
    }
}
