package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.SchedulePlanRequest;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.entity.SchedulePlan;
import com.token.erp.mapper.ProductionOrderMapper;
import com.token.erp.mapper.SchedulePlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulePlanService {

    private final SchedulePlanMapper schedulePlanMapper;
    private final ProductionOrderMapper orderMapper;
    private final CodeGenerator codeGenerator;

    public Page<SchedulePlan> page(long page, long pageSize, Long workOrderId, Long machineId, String status) {
        LambdaQueryWrapper<SchedulePlan> qw = new LambdaQueryWrapper<>();
        if (workOrderId != null) qw.eq(SchedulePlan::getWorkOrderId, workOrderId);
        if (machineId != null) qw.eq(SchedulePlan::getMachineId, machineId);
        if (status != null && !status.isEmpty()) qw.eq(SchedulePlan::getStatus, status);
        qw.orderByDesc(SchedulePlan::getId);
        return schedulePlanMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public SchedulePlan create(SchedulePlanRequest req) {
        if (req.getWorkOrderId() == null) throw new BizException("工单不能为空");
        if (req.getPlanStart() == null || req.getPlanEnd() == null) throw new BizException("计划起止时间不能为空");
        if (req.getPlanEnd().isBefore(req.getPlanStart())) throw new BizException("计划结束时间必须晚于开始时间");

        ProductionOrder order = orderMapper.selectById(req.getWorkOrderId());
        if (order == null) throw new BizException(404, "生产工单不存在");
        // 关联校验：工单必须已审批
        if (!"approved".equals(order.getApproveStatus())) {
            throw new BizException("工单未审批，不能排产");
        }

        // 机台时间冲突检测（同一机台时间不能重叠）
        Long machineId = req.getMachineId() != null ? req.getMachineId() : order.getMachineId();
        if (machineId != null) {
            List<SchedulePlan> conflicts = schedulePlanMapper.selectList(
                    new LambdaQueryWrapper<SchedulePlan>()
                            .eq(SchedulePlan::getMachineId, machineId)
                            .in(SchedulePlan::getStatus, "planned", "started")
                            .lt(SchedulePlan::getPlanStart, req.getPlanEnd())
                            .gt(SchedulePlan::getPlanEnd, req.getPlanStart()));
            if (conflicts != null && !conflicts.isEmpty()) {
                throw new BizException("该机台在所选时间段已有排产任务（排产单号 " + conflicts.get(0).getScheduleNo() + "），请调整时间或更换机台");
            }
        }

        SchedulePlan sp = new SchedulePlan();
        sp.setScheduleNo(codeGenerator.next("PC"));
        sp.setWorkOrderId(req.getWorkOrderId());
        sp.setMachineId(req.getMachineId() != null ? req.getMachineId() : order.getMachineId());
        sp.setWorkshopId(req.getWorkshopId() != null ? req.getWorkshopId() : order.getWorkshopId());
        sp.setPlanStart(req.getPlanStart());
        sp.setPlanEnd(req.getPlanEnd());
        sp.setStatus("planned");
        sp.setRemark(req.getRemark());
        schedulePlanMapper.insert(sp);
        return sp;
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        SchedulePlan sp = schedulePlanMapper.selectById(id);
        if (sp == null) throw new BizException(404, "排产计划不存在");
        sp.setStatus(status);
        schedulePlanMapper.updateById(sp);
    }
}
