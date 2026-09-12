package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PurchaseOrderRequest;
import com.token.erp.entity.PurchaseOrder;
import com.token.erp.entity.PurchaseOrderItem;
import com.token.erp.mapper.PurchaseOrderItemMapper;
import com.token.erp.mapper.PurchaseOrderMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final NameFillUtil nameFillUtil;

    public Page<PurchaseOrder> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(PurchaseOrder::getOrderNo, keyword));
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(PurchaseOrder::getApproveStatus, approveStatus);
        }
        qw.orderByDesc(PurchaseOrder::getId);
        Page<PurchaseOrder> result = orderMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> sm = nameFillUtil.supplierNameMap(
                    result.getRecords().stream().map(PurchaseOrder::getSupplierId).collect(Collectors.toList()));
            result.getRecords().forEach(o -> o.setSupplierName(sm.get(o.getSupplierId())));

            // 物料摘要：每单首个物料「编号 名称」，多种类时加「等N种」
            List<Long> orderIds = result.getRecords().stream().map(PurchaseOrder::getId).collect(Collectors.toList());
            List<PurchaseOrderItem> allItems = itemMapper.selectList(
                    new LambdaQueryWrapper<PurchaseOrderItem>()
                            .in(PurchaseOrderItem::getOrderId, orderIds)
                            .orderByAsc(PurchaseOrderItem::getId));
            Map<Long, String[]> mm = nameFillUtil.materialMap(
                    allItems.stream().map(PurchaseOrderItem::getMaterialId).distinct().collect(Collectors.toList()));
            Map<Long, List<PurchaseOrderItem>> grouped = allItems.stream()
                    .collect(Collectors.groupingBy(PurchaseOrderItem::getOrderId));
            result.getRecords().forEach(o -> {
                List<PurchaseOrderItem> its = grouped.get(o.getId());
                if (its == null || its.isEmpty()) return;
                String[] arr = mm.get(its.get(0).getMaterialId());
                long kinds = its.stream().map(PurchaseOrderItem::getMaterialId).distinct().count();
                if (arr != null) {
                    o.setMaterialCode(arr[0] + (kinds > 1 ? " 等" + kinds + "种" : ""));
                    o.setMaterialName(arr[1]);
                }
            });
        }
        return result;
    }

    public List<PurchaseOrderItem> items(Long orderId) {
        List<PurchaseOrderItem> list = itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId));
        if (!list.isEmpty()) {
            Map<Long, String[]> mm = nameFillUtil.materialMap(
                    list.stream().map(PurchaseOrderItem::getMaterialId).collect(Collectors.toList()));
            list.forEach(it -> {
                String[] arr = mm.get(it.getMaterialId());
                if (arr != null) {
                    it.setMaterialCode(arr[0]);
                    it.setMaterialName(arr[1]);
                }
            });
        }
        return list;
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrderRequest request) {
        if (request.getSupplierId() == null) throw new BizException("供应商不能为空");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("采购明细不能为空");

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(codeGenerator.next("PO"));
        order.setSupplierId(request.getSupplierId());
        order.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now());
        order.setPurchaseType(StringUtils.hasText(request.getPurchaseType()) ? request.getPurchaseType() : "stock");
        order.setExpectedDate(request.getExpectedDate());
        order.setContact(request.getContact());
        order.setDepartmentId(request.getDepartmentId());
        order.setBuyer(request.getBuyer());
        order.setApproveStatus("pending");
        order.setBizStatus("pending_inbound");
        order.setRemark(request.getRemark());
        order.setTotalAmount(BigDecimal.ZERO);
        orderMapper.insert(order);

        BigDecimal total = saveItems(order.getId(), request.getItems());
        order.setTotalAmount(total);
        orderMapper.updateById(order);
        return order;
    }

    @Transactional
    public PurchaseOrder update(PurchaseOrderRequest request) {
        PurchaseOrder db = orderMapper.selectById(request.getId());
        if (db == null) throw new BizException(404, "采购订单不存在");
        if (!"pending".equals(db.getApproveStatus())) {
            throw new BizException("仅待提交状态的订单可修改");
        }
        db.setSupplierId(request.getSupplierId());
        db.setExpectedDate(request.getExpectedDate());
        db.setContact(request.getContact());
        db.setBuyer(request.getBuyer());
        db.setRemark(request.getRemark());
        orderMapper.updateById(db);

        // 删除旧明细，重建
        itemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, db.getId()));
        BigDecimal total = saveItems(db.getId(), request.getItems());
        db.setTotalAmount(total);
        orderMapper.updateById(db);
        return db;
    }

    /** 提交审批 */
    @Transactional
    public void submit(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "采购订单不存在");
        if (!"pending".equals(order.getApproveStatus())) {
            throw new BizException("仅待提交状态的订单可提交");
        }
        order.setApproveStatus("approving");
        orderMapper.updateById(order);
    }

    /** 审批通过 */
    @Transactional
    public void approve(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "采购订单不存在");
        if (!"approving".equals(order.getApproveStatus())) {
            throw new BizException("仅审批中的订单可审批");
        }
        // 禁止自审校验
        checkSelfApprove(order.getCreateBy());
        order.setApproveStatus("approved");
        orderMapper.updateById(order);
        // 通知制单人审批结果
        notificationService.sendToUsername(order.getCreateBy(), "approve_result",
                "采购订单已审批", "采购订单 " + order.getOrderNo() + " 已审批通过",
                "purchase_order", order.getId());
    }

    /** 禁止自审校验（配置 allow_self_approve=false 时生效） */
    private void checkSelfApprove(String createBy) {
        if (!sysConfigService.getBoolean("allow_self_approve")) {
            String currentUser = securityUtils.currentUsername();
            if (createBy != null && createBy.equals(currentUser)) {
                throw new BizException("禁止自审：不能审批自己提交的单据");
            }
        }
    }

    /** 反审核（打回待提交） */
    @Transactional
    public void unApprove(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(404, "采购订单不存在");
        if (!"approved".equals(order.getApproveStatus())) {
            throw new BizException("仅已审批的订单可反审核");
        }
        if (!"pending_inbound".equals(order.getBizStatus())) {
            throw new BizException("已产生入库的订单不能反审核，需先处理下游业务");
        }
        order.setApproveStatus("pending");
        orderMapper.updateById(order);
    }

    /** 保存明细并计算金额（差额法倒挤税额） */
    private BigDecimal saveItems(Long orderId, List<PurchaseOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            if (item.getMaterialId() == null) throw new BizException("物料不能为空");
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            BigDecimal taxRate = item.getTaxRate() == null ? new BigDecimal("13") : item.getTaxRate();
            // 数量必须大于0，单价、税率不能为负
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("数量必须大于0");
            if (price.compareTo(BigDecimal.ZERO) < 0) throw new BizException("单价不能为负数");
            if (taxRate.compareTo(BigDecimal.ZERO) < 0) throw new BizException("税率不能为负数");

            // 价税合计 = 数量 × 含税单价
            BigDecimal totalAmount = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            // 不含税金额 = 价税合计 / (1 + 税率)
            BigDecimal divisor = BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
            BigDecimal amountExTax = totalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
            // 税额 = 价税合计 - 不含税金额（差额法倒挤）
            BigDecimal taxAmount = totalAmount.subtract(amountExTax);

            item.setId(null);
            item.setOrderId(orderId);
            item.setQuantity(qty);
            item.setPrice(price);
            item.setTaxRate(taxRate);
            item.setAmountExTax(amountExTax);
            item.setTaxAmount(taxAmount);
            item.setTotalAmount(totalAmount);
            item.setInboundQty(item.getInboundQty() == null ? BigDecimal.ZERO : item.getInboundQty());
            itemMapper.insert(item);

            total = total.add(totalAmount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
