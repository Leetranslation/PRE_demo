package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.OutsourceOrderRequest;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 外协单服务：整单外协 / 工序外协
 * 状态流转：审批 pending→approving→approved；业务 pending→outsourcing→finished/voided
 * 回写工单 outsource_status（none/whole/process）
 */
@Service
@RequiredArgsConstructor
public class OutsourceOrderService {

    private final OutsourceOrderMapper outsourceMapper;
    private final OutsourceOrderItemMapper itemMapper;
    private final ProductionOrderMapper orderMapper;
    private final SupplierMapper supplierMapper;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final NameFillUtil nameFillUtil;

    public Page<OutsourceOrder> page(long page, long pageSize, Long orderId, String type, String approveStatus) {
        LambdaQueryWrapper<OutsourceOrder> qw = new LambdaQueryWrapper<>();
        if (orderId != null) qw.eq(OutsourceOrder::getOrderId, orderId);
        if (StringUtils.hasText(type)) qw.eq(OutsourceOrder::getType, type);
        if (StringUtils.hasText(approveStatus)) qw.eq(OutsourceOrder::getApproveStatus, approveStatus);
        qw.orderByDesc(OutsourceOrder::getId);
        Page<OutsourceOrder> result = outsourceMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            List<OutsourceOrder> records = result.getRecords();
            Map<Long, String> sm = nameFillUtil.supplierNameMap(
                    records.stream().map(OutsourceOrder::getSupplierId).collect(Collectors.toList()));
            Map<Long, String> om = nameFillUtil.productionOrderNoMap(
                    records.stream().map(OutsourceOrder::getOrderId).collect(Collectors.toList()));
            Map<Long, String> pm = nameFillUtil.processNameMap(
                    records.stream().map(OutsourceOrder::getProcessId).collect(Collectors.toList()));
            records.forEach(o -> {
                o.setSupplierName(sm.get(o.getSupplierId()));
                o.setSourceOrderNo(om.get(o.getOrderId()));
                o.setProcessName(pm.get(o.getProcessId()));
            });
        }
        return result;
    }

    public List<OutsourceOrderItem> items(Long outsourceId) {
        List<OutsourceOrderItem> list = itemMapper.selectList(
                new LambdaQueryWrapper<OutsourceOrderItem>().eq(OutsourceOrderItem::getOutsourceId, outsourceId));
        if (!list.isEmpty()) {
            List<Long> pids = list.stream().filter(i -> "product".equals(i.getItemType()))
                    .map(OutsourceOrderItem::getItemId).collect(Collectors.toList());
            List<Long> mids = list.stream().filter(i -> !"product".equals(i.getItemType()))
                    .map(OutsourceOrderItem::getItemId).collect(Collectors.toList());
            Map<Long, String[]> pm = nameFillUtil.productMap(pids);
            Map<Long, String[]> mm = nameFillUtil.materialMap(mids);
            list.forEach(i -> {
                String[] arr = "product".equals(i.getItemType()) ? pm.get(i.getItemId()) : mm.get(i.getItemId());
                if (arr != null) {
                    i.setItemCode(arr[0]);
                    i.setItemName(arr[1]);
                }
            });
        }
        return list;
    }

    @Transactional
    public OutsourceOrder create(OutsourceOrderRequest req) {
        if (req.getOrderId() == null) throw new BizException("生产工单不能为空");
        if (req.getSupplierId() == null) throw new BizException("外协供应商不能为空");
        ProductionOrder order = orderMapper.selectById(req.getOrderId());
        if (order == null) throw new BizException(404, "生产工单不存在");
        if (supplierMapper.selectById(req.getSupplierId()) == null) throw new BizException(404, "供应商不存在");

        String type = StringUtils.hasText(req.getType()) ? req.getType() : "whole";
        if (!"whole".equals(type) && !"process".equals(type))
            throw new BizException("类型必须为 whole整单外协 或 process工序外协");
        if ("process".equals(type) && req.getProcessId() == null)
            throw new BizException("工序外协必须指定工序");

        OutsourceOrder o = new OutsourceOrder();
        o.setOrderNo(codeGenerator.next("WX"));
        o.setType(type);
        o.setOrderId(req.getOrderId());
        o.setProcessId(req.getProcessId());
        o.setSupplierId(req.getSupplierId());
        o.setQuantity(req.getQuantity() != null ? req.getQuantity() : order.getQuantity());
        o.setDeliveryDate(req.getDeliveryDate());
        o.setApproveStatus("pending");
        o.setBizStatus("pending");
        o.setRemark(req.getRemark());
        outsourceMapper.insert(o);

        if (req.getItems() != null) {
            for (OutsourceOrderItem it : req.getItems()) {
                it.setId(null);
                it.setOutsourceId(o.getId());
                if (it.getItemType() == null) it.setItemType("material");
                itemMapper.insert(it);
            }
        }

        // 回写工单外协状态
        order.setOutsourceStatus(type);
        orderMapper.updateById(order);
        return o;
    }

    @Transactional
    public OutsourceOrder update(OutsourceOrderRequest req) {
        OutsourceOrder db = outsourceMapper.selectById(req.getId());
        if (db == null) throw new BizException(404, "外协单不存在");
        if (!"pending".equals(db.getApproveStatus())) throw new BizException("仅待提交状态可修改");
        db.setSupplierId(req.getSupplierId());
        db.setProcessId(req.getProcessId());
        db.setQuantity(req.getQuantity());
        db.setDeliveryDate(req.getDeliveryDate());
        db.setRemark(req.getRemark());
        outsourceMapper.updateById(db);
        itemMapper.delete(new LambdaQueryWrapper<OutsourceOrderItem>()
                .eq(OutsourceOrderItem::getOutsourceId, db.getId()));
        if (req.getItems() != null) {
            for (OutsourceOrderItem it : req.getItems()) {
                it.setId(null);
                it.setOutsourceId(db.getId());
                if (it.getItemType() == null) it.setItemType("material");
                itemMapper.insert(it);
            }
        }
        return db;
    }

    @Transactional
    public void submit(Long id) {
        OutsourceOrder o = outsourceMapper.selectById(id);
        if (o == null) throw new BizException(404, "外协单不存在");
        if (!"pending".equals(o.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        o.setApproveStatus("approving");
        outsourceMapper.updateById(o);
    }

    @Transactional
    public void approve(Long id) {
        OutsourceOrder o = outsourceMapper.selectById(id);
        if (o == null) throw new BizException(404, "外协单不存在");
        if (!"approving".equals(o.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        checkSelfApprove(o.getCreateBy());
        o.setApproveStatus("approved");
        outsourceMapper.updateById(o);
        notificationService.sendToUsername(o.getCreateBy(), "approve_result",
                "外协单已审批", "外协单 " + o.getOrderNo() + " 已审批通过",
                "outsource_order", o.getId());
    }

    @Transactional
    public void unApprove(Long id) {
        OutsourceOrder o = outsourceMapper.selectById(id);
        if (o == null) throw new BizException(404, "外协单不存在");
        if (!"approved".equals(o.getApproveStatus())) throw new BizException("仅已审批可反审核");
        if (!"pending".equals(o.getBizStatus())) throw new BizException("已开始外协的单不能反审核，需先处理下游");
        o.setApproveStatus("pending");
        outsourceMapper.updateById(o);
    }

    /** 开始外协：pending → outsourcing */
    @Transactional
    public void start(Long id) {
        OutsourceOrder o = outsourceMapper.selectById(id);
        if (o == null) throw new BizException(404, "外协单不存在");
        if (!"approved".equals(o.getApproveStatus())) throw new BizException("外协单未审批，不能开始");
        if (!"pending".equals(o.getBizStatus())) throw new BizException("仅未开始的外协单可开始");
        o.setBizStatus("outsourcing");
        outsourceMapper.updateById(o);
    }

    /** 外协完工：outsourcing → finished */
    @Transactional
    public void finish(Long id) {
        OutsourceOrder o = outsourceMapper.selectById(id);
        if (o == null) throw new BizException(404, "外协单不存在");
        if (!"outsourcing".equals(o.getBizStatus())) throw new BizException("仅外协中的外协单可完工");
        o.setBizStatus("finished");
        outsourceMapper.updateById(o);
    }

    /** 作废 */
    @Transactional
    public void voidOrder(Long id) {
        OutsourceOrder o = outsourceMapper.selectById(id);
        if (o == null) throw new BizException(404, "外协单不存在");
        if ("finished".equals(o.getBizStatus())) throw new BizException("已完工的外协单不能作废");
        o.setBizStatus("voided");
        outsourceMapper.updateById(o);
        // 作废后工单外协状态回退为 none
        ProductionOrder order = orderMapper.selectById(o.getOrderId());
        if (order != null) {
            order.setOutsourceStatus("none");
            orderMapper.updateById(order);
        }
    }

    private void checkSelfApprove(String createBy) {
        if (!sysConfigService.getBoolean("allow_self_approve")) {
            String currentUser = securityUtils.currentUsername();
            if (createBy != null && createBy.equals(currentUser)) {
                throw new BizException("禁止自审：不能审批自己提交的单据");
            }
        }
    }
}
