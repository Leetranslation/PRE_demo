package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PurchaseInquiryRequest;
import com.token.erp.dto.PurchaseOrderRequest;
import com.token.erp.entity.PurchaseInquiry;
import com.token.erp.entity.PurchaseInquiryItem;
import com.token.erp.entity.PurchaseOrder;
import com.token.erp.entity.PurchaseOrderItem;
import com.token.erp.mapper.PurchaseInquiryItemMapper;
import com.token.erp.mapper.PurchaseInquiryMapper;
import com.token.erp.mapper.PurchaseOrderMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseInquiryService {

    private final PurchaseInquiryMapper inquiryMapper;
    private final PurchaseInquiryItemMapper itemMapper;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final NameFillUtil nameFillUtil;

    public Page<PurchaseInquiry> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<PurchaseInquiry> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(PurchaseInquiry::getInquiryNo, keyword);
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(PurchaseInquiry::getApproveStatus, approveStatus);
        }
        qw.orderByDesc(PurchaseInquiry::getId);
        return inquiryMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public List<PurchaseInquiryItem> items(Long inquiryId) {
        List<PurchaseInquiryItem> list = itemMapper.selectList(
                new LambdaQueryWrapper<PurchaseInquiryItem>().eq(PurchaseInquiryItem::getInquiryId, inquiryId));
        if (!list.isEmpty()) {
            Map<Long, String[]> mm = nameFillUtil.materialMap(
                    list.stream().map(PurchaseInquiryItem::getMaterialId).collect(Collectors.toList()));
            Map<Long, String> sm = nameFillUtil.supplierNameMap(
                    list.stream().map(PurchaseInquiryItem::getSupplierId).collect(Collectors.toList()));
            list.forEach(i -> {
                String[] arr = mm.get(i.getMaterialId());
                if (arr != null) {
                    i.setMaterialCode(arr[0]);
                    i.setMaterialName(arr[1]);
                }
                i.setSupplierName(sm.get(i.getSupplierId()));
            });
        }
        return list;
    }

    @Transactional
    public PurchaseInquiry create(PurchaseInquiryRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("询价明细不能为空");

        PurchaseInquiry inquiry = new PurchaseInquiry();
        inquiry.setInquiryNo(codeGenerator.next("XJ"));
        inquiry.setInquiryDate(request.getInquiryDate() != null ? request.getInquiryDate() : LocalDate.now());
        inquiry.setInquirer(request.getInquirer());
        inquiry.setApproveStatus("pending");
        inquiry.setBizStatus("pending");
        inquiry.setRemark(request.getRemark());
        inquiryMapper.insert(inquiry);

        for (PurchaseInquiryRequest.Item item : request.getItems()) {
            if (item.getMaterialId() == null) throw new BizException("物料不能为空");
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("数量必须大于0");

            PurchaseInquiryItem ii = new PurchaseInquiryItem();
            ii.setInquiryId(inquiry.getId());
            ii.setMaterialId(item.getMaterialId());
            ii.setSpec(item.getSpec());
            ii.setUnit(item.getUnit());
            ii.setQuantity(qty);
            ii.setSupplierId(item.getSupplierId());
            ii.setPrice(item.getPrice());
            ii.setRemark(item.getRemark());
            itemMapper.insert(ii);
        }
        return inquiry;
    }

    @Transactional
    public void submit(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) throw new BizException(404, "询价单不存在");
        if (!"pending".equals(inquiry.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        inquiry.setApproveStatus("approving");
        inquiryMapper.updateById(inquiry);
    }

    @Transactional
    public void approve(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) throw new BizException(404, "询价单不存在");
        if (!"approving".equals(inquiry.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        checkSelfApprove(inquiry.getCreateBy());
        inquiry.setApproveStatus("approved");
        inquiryMapper.updateById(inquiry);
    }

    @Transactional
    public void unApprove(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) throw new BizException(404, "询价单不存在");
        if (!"approved".equals(inquiry.getApproveStatus())) throw new BizException("仅已审批可反审核");
        inquiry.setApproveStatus("pending");
        inquiryMapper.updateById(inquiry);
    }

    private void checkSelfApprove(String createBy) {
        if (!sysConfigService.getBoolean("allow_self_approve")) {
            String currentUser = securityUtils.currentUsername();
            if (createBy != null && createBy.equals(currentUser)) {
                throw new BizException("禁止自审：不能审批自己提交的单据");
            }
        }
    }

    /**
     * 比价生单：将已审批询价单转采购订单
     * @param inquiryId  询价单ID
     * @param supplierId 指定供应商（可空；为空时自动按供应商总价最低比价选择）
     */
    @Transactional
    public PurchaseOrder toPurchaseOrder(Long inquiryId, Long supplierId) {
        PurchaseInquiry inquiry = inquiryMapper.selectById(inquiryId);
        if (inquiry == null) throw new BizException(404, "询价单不存在");
        if (!"approved".equals(inquiry.getApproveStatus())) throw new BizException("仅已审批的询价单可生单");
        if (!"pending".equals(inquiry.getBizStatus())) throw new BizException("该询价单已比价生单");

        List<PurchaseInquiryItem> allItems = items(inquiryId);
        if (allItems.isEmpty()) throw new BizException("询价单无明细");

        Long targetSupplier = supplierId;
        if (targetSupplier == null) {
            targetSupplier = bestSupplier(allItems);
        }
        if (targetSupplier == null) throw new BizException("无可生单的供应商报价");

        List<PurchaseInquiryItem> selected = new ArrayList<>();
        for (PurchaseInquiryItem i : allItems) {
            if (targetSupplier.equals(i.getSupplierId())) selected.add(i);
        }
        if (selected.isEmpty()) throw new BizException("该询价单无该供应商的报价明细");

        PurchaseOrderRequest req = new PurchaseOrderRequest();
        req.setSupplierId(targetSupplier);
        req.setOrderDate(LocalDate.now());
        List<PurchaseOrderItem> orderItems = new ArrayList<>();
        for (PurchaseInquiryItem si : selected) {
            PurchaseOrderItem oi = new PurchaseOrderItem();
            oi.setMaterialId(si.getMaterialId());
            oi.setSpec(si.getSpec());
            oi.setUnit(si.getUnit());
            oi.setQuantity(si.getQuantity());
            oi.setPrice(si.getPrice());
            oi.setRemark(si.getRemark());
            orderItems.add(oi);
        }
        req.setItems(orderItems);

        PurchaseOrder order = purchaseOrderService.create(req);
        order.setSourceType("inquiry");
        order.setSourceId(inquiryId);
        purchaseOrderMapper.updateById(order);

        inquiry.setBizStatus("priced");
        inquiryMapper.updateById(inquiry);
        return order;
    }

    /** 自动比价：按供应商归集总价，选总价最低的供应商 */
    private Long bestSupplier(List<PurchaseInquiryItem> items) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (PurchaseInquiryItem i : items) {
            if (i.getSupplierId() == null || i.getPrice() == null) continue;
            BigDecimal qty = i.getQuantity() == null ? BigDecimal.ZERO : i.getQuantity();
            BigDecimal line = i.getPrice().multiply(qty);
            totals.merge(i.getSupplierId(), line, BigDecimal::add);
        }
        return totals.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
