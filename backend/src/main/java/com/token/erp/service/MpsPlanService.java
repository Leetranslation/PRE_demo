package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.MpsPlanRequest;
import com.token.erp.dto.ProductionOrderRequest;
import com.token.erp.entity.MpsPlan;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.mapper.MpsPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MpsPlanService {

    private final MpsPlanMapper mpsPlanMapper;
    private final ProductionOrderService productionOrderService;
    private final CodeGenerator codeGenerator;

    public Page<MpsPlan> page(long page, long pageSize, String status) {
        LambdaQueryWrapper<MpsPlan> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq(MpsPlan::getStatus, status);
        qw.orderByDesc(MpsPlan::getId);
        return mpsPlanMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public MpsPlan create(MpsPlanRequest req) {
        if (req.getProductId() == null) throw new BizException("产品不能为空");
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("计划数量必须大于0");
        }
        MpsPlan plan = new MpsPlan();
        plan.setMpsNo(codeGenerator.next("MP"));
        plan.setProductId(req.getProductId());
        plan.setQuantity(req.getQuantity());
        plan.setPlanDate(req.getPlanDate() != null ? req.getPlanDate() : java.time.LocalDate.now());
        plan.setWorkshopId(req.getWorkshopId());
        plan.setStatus("planned");
        plan.setRemark(req.getRemark());
        mpsPlanMapper.insert(plan);
        return plan;
    }

    /** 生成工单：主生产计划 → 生产工单 */
    @Transactional
    public ProductionOrder generateWorkOrder(Long id) {
        MpsPlan plan = mpsPlanMapper.selectById(id);
        if (plan == null) throw new BizException(404, "主生产计划不存在");
        if ("confirmed".equals(plan.getStatus())) throw new BizException("该计划已生成工单");

        ProductionOrderRequest req = new ProductionOrderRequest();
        req.setProductId(plan.getProductId());
        req.setQuantity(plan.getQuantity());
        req.setPlanDate(plan.getPlanDate());
        req.setWorkshopId(plan.getWorkshopId());
        req.setSourceType("mps");
        req.setSourceId(plan.getId());
        ProductionOrder order = productionOrderService.create(req);

        plan.setStatus("confirmed");
        plan.setWorkOrderId(order.getId());
        mpsPlanMapper.updateById(plan);
        return order;
    }
}
