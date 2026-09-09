package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.ProcessProgressVO;
import com.token.erp.dto.ReportWorkRequest;
import com.token.erp.entity.Process;
import com.token.erp.entity.ProcessRoute;
import com.token.erp.entity.ProcessRouteItem;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.entity.ReportWork;
import com.token.erp.mapper.ProcessMapper;
import com.token.erp.mapper.ProcessRouteItemMapper;
import com.token.erp.mapper.ProcessRouteMapper;
import com.token.erp.mapper.ProductionOrderMapper;
import com.token.erp.mapper.ReportWorkMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportWorkService {

    private final ReportWorkMapper reportWorkMapper;
    private final ProductionOrderMapper orderMapper;
    private final ProcessRouteMapper processRouteMapper;
    private final ProcessRouteItemMapper processRouteItemMapper;
    private final ProcessMapper processMapper;
    private final CodeGenerator codeGenerator;
    private final SecurityUtils securityUtils;

    public Page<ReportWork> page(long page, long pageSize, Long workOrderId, String auditStatus) {
        LambdaQueryWrapper<ReportWork> qw = new LambdaQueryWrapper<>();
        if (workOrderId != null) {
            qw.eq(ReportWork::getWorkOrderId, workOrderId);
        }
        if (StringUtils.hasText(auditStatus)) {
            qw.eq(ReportWork::getAuditStatus, auditStatus);
        }
        qw.orderByDesc(ReportWork::getId);
        return reportWorkMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public ReportWork create(ReportWorkRequest req) {
        if (req.getWorkOrderId() == null) throw new BizException("工单不能为空");
        if (req.getProcessId() == null) throw new BizException("工序不能为空");
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("报工数量必须大于0");
        }

        ProductionOrder order = orderMapper.selectById(req.getWorkOrderId());
        if (order == null) throw new BizException(404, "生产工单不存在");
        // 关联校验：工单必须已审批且已开始（执行中）
        if (!"approved".equals(order.getApproveStatus())) {
            throw new BizException("工单未审批，不能报工");
        }
        if (!"producing".equals(order.getBizStatus())) {
            throw new BizException("工单未开始（未执行中），不能报工");
        }

        ReportWork rw = new ReportWork();
        rw.setReportNo(codeGenerator.next("BW"));
        rw.setWorkOrderId(req.getWorkOrderId());
        rw.setProcessId(req.getProcessId());
        rw.setProcessRouteItemId(req.getProcessRouteItemId());
        rw.setMachineId(req.getMachineId() != null ? req.getMachineId() : order.getMachineId());
        rw.setWorkshopId(order.getWorkshopId());
        rw.setReporterId(securityUtils.currentUserId());
        rw.setReporterName(securityUtils.currentUsername());
        rw.setReportDate(LocalDateTime.now());
        rw.setQuantity(req.getQuantity());
        rw.setQualifiedQty(req.getQualifiedQty() != null ? req.getQualifiedQty() : req.getQuantity());
        rw.setDefectiveQty(req.getDefectiveQty() != null ? req.getDefectiveQty() : BigDecimal.ZERO);
        rw.setWorkHours(req.getWorkHours());
        rw.setStartTime(req.getStartTime());
        rw.setEndTime(req.getEndTime());
        rw.setAuditStatus("pending");
        rw.setRemark(req.getRemark());

        reportWorkMapper.insert(rw);
        return rw;
    }

    /** 审工：审核通过（联动工单完工数量） */
    @Transactional
    public void approve(Long id) {
        ReportWork rw = reportWorkMapper.selectById(id);
        if (rw == null) throw new BizException(404, "报工单不存在");
        if (!"pending".equals(rw.getAuditStatus()) && !"rejected".equals(rw.getAuditStatus())) {
            throw new BizException("仅待审或驳回状态可审核");
        }
        rw.setAuditStatus("approved");
        rw.setAuditorId(securityUtils.currentUserId());
        rw.setAuditorName(securityUtils.currentUsername());
        rw.setAuditTime(LocalDateTime.now());
        reportWorkMapper.updateById(rw);

        // 联动：只有最后一道工序的良品数才累加到工单完工数量（成品）
        ProductionOrder order = orderMapper.selectById(rw.getWorkOrderId());
        if (order != null && rw.getQualifiedQty() != null && isLastProcess(order, rw.getProcessId())) {
            BigDecimal completed = order.getCompletedQty().add(rw.getQualifiedQty());
            // 完工数量不超过工单计划数量
            if (completed.compareTo(order.getQuantity()) > 0) {
                completed = order.getQuantity();
            }
            order.setCompletedQty(completed);
            orderMapper.updateById(order);
        }
    }

    /** 审工：反审核（回退工单完工数量） */
    @Transactional
    public void unApprove(Long id) {
        ReportWork rw = reportWorkMapper.selectById(id);
        if (rw == null) throw new BizException(404, "报工单不存在");
        if (!"approved".equals(rw.getAuditStatus())) {
            throw new BizException("仅已审核状态可反审核");
        }
        rw.setAuditStatus("pending");
        rw.setAuditorId(null);
        rw.setAuditorName(null);
        rw.setAuditTime(null);
        reportWorkMapper.updateById(rw);

        // 回退：扣减工单完工数量（仅最后一道工序）
        ProductionOrder order = orderMapper.selectById(rw.getWorkOrderId());
        if (order != null && rw.getQualifiedQty() != null && isLastProcess(order, rw.getProcessId())) {
            BigDecimal completed = order.getCompletedQty().subtract(rw.getQualifiedQty());
            if (completed.compareTo(BigDecimal.ZERO) < 0) {
                completed = BigDecimal.ZERO;
            }
            order.setCompletedQty(completed);
            orderMapper.updateById(order);
        }
    }

    /** 判断工序是否是工单工艺路线的最后一道工序 */
    private boolean isLastProcess(ProductionOrder order, Long processId) {
        if (order.getProcessRouteId() == null) {
            // 无工艺路线时，无法判断，按「是最后工序」处理（单工序场景）
            return true;
        }
        ProcessRoute route = processRouteMapper.selectById(order.getProcessRouteId());
        if (route == null) return true;
        List<ProcessRouteItem> items = processRouteItemMapper.selectList(
                new LambdaQueryWrapper<ProcessRouteItem>()
                        .eq(ProcessRouteItem::getRouteId, route.getId())
                        .orderByDesc(ProcessRouteItem::getSeqNo));
        if (items == null || items.isEmpty()) return true;
        // 最后一道工序 = seqNo 最大的那道
        return items.get(0).getProcessId().equals(processId);
    }

    /** 审工：驳回 */
    @Transactional
    public void reject(Long id, String auditRemark) {
        ReportWork rw = reportWorkMapper.selectById(id);
        if (rw == null) throw new BizException(404, "报工单不存在");
        if (!"pending".equals(rw.getAuditStatus())) {
            throw new BizException("仅待审状态可驳回");
        }
        rw.setAuditStatus("rejected");
        rw.setAuditorId(securityUtils.currentUserId());
        rw.setAuditorName(securityUtils.currentUsername());
        rw.setAuditTime(LocalDateTime.now());
        rw.setAuditRemark(auditRemark);
        reportWorkMapper.updateById(rw);
    }

    /** 一键审核：批量审核待审的报工单 */
    @Transactional
    public void batchApprove(Long workOrderId) {
        LambdaQueryWrapper<ReportWork> qw = new LambdaQueryWrapper<>();
        qw.eq(ReportWork::getAuditStatus, "pending");
        if (workOrderId != null) {
            qw.eq(ReportWork::getWorkOrderId, workOrderId);
        }
        java.util.List<ReportWork> list = reportWorkMapper.selectList(qw);
        Long uid = securityUtils.currentUserId();
        String uname = securityUtils.currentUsername();
        for (ReportWork rw : list) {
            rw.setAuditStatus("approved");
            rw.setAuditorId(uid);
            rw.setAuditorName(uname);
            rw.setAuditTime(LocalDateTime.now());
            reportWorkMapper.updateById(rw);
        }
    }

    /** 按工单查工序进度 */
    public List<ProcessProgressVO> processProgress(Long workOrderId) {
        ProductionOrder order = orderMapper.selectById(workOrderId);
        if (order == null) throw new BizException(404, "生产工单不存在");

        List<ProcessProgressVO> result = new ArrayList<>();
        if (order.getProcessRouteId() == null) {
            return result;
        }
        ProcessRoute route = processRouteMapper.selectById(order.getProcessRouteId());
        if (route == null) return result;

        // 工艺路线明细（工序序列）
        List<ProcessRouteItem> items = processRouteItemMapper.selectList(
                new LambdaQueryWrapper<ProcessRouteItem>()
                        .eq(ProcessRouteItem::getRouteId, route.getId())
                        .orderByAsc(ProcessRouteItem::getSeqNo));

        // 工序名称映射
        List<Long> processIds = items.stream().map(ProcessRouteItem::getProcessId).collect(Collectors.toList());
        Map<Long, String> nameMap = processIds.isEmpty() ? Map.of() :
                processMapper.selectBatchIds(processIds).stream()
                        .collect(Collectors.toMap(Process::getId, Process::getName));

        // 该工单所有已审报工，按工序聚合良品数
        List<ReportWork> reports = reportWorkMapper.selectList(
                new LambdaQueryWrapper<ReportWork>()
                        .eq(ReportWork::getWorkOrderId, workOrderId)
                        .eq(ReportWork::getAuditStatus, "approved"));
        Map<Long, BigDecimal> qtyMap = reports.stream().collect(Collectors.groupingBy(
                ReportWork::getProcessId,
                Collectors.reducing(BigDecimal.ZERO, ReportWork::getQualifiedQty, BigDecimal::add)));
        Map<Long, Long> countMap = reports.stream().collect(Collectors.groupingBy(
                ReportWork::getProcessId, Collectors.counting()));

        for (ProcessRouteItem item : items) {
            ProcessProgressVO vo = new ProcessProgressVO();
            vo.setProcessId(item.getProcessId());
            vo.setProcessName(nameMap.getOrDefault(item.getProcessId(), "工序" + item.getProcessId()));
            vo.setSeqNo(item.getSeqNo());
            vo.setPlanQty(order.getQuantity());
            vo.setReportedQty(qtyMap.getOrDefault(item.getProcessId(), BigDecimal.ZERO));
            vo.setReportCount(countMap.getOrDefault(item.getProcessId(), 0L));
            vo.setIsKey(item.getIsKey());
            vo.setIsQc(item.getIsQc());
            result.add(vo);
        }
        return result;
    }
}
