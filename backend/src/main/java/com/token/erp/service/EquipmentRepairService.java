package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.EquipmentRepair;
import com.token.erp.entity.Machine;
import com.token.erp.mapper.EquipmentRepairMapper;
import com.token.erp.mapper.MachineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentRepairService {

    private final EquipmentRepairMapper repairMapper;
    private final MachineMapper machineMapper;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<EquipmentRepair> page(long page, long pageSize, Long machineId, String repairStatus) {
        LambdaQueryWrapper<EquipmentRepair> qw = new LambdaQueryWrapper<>();
        if (machineId != null) qw.eq(EquipmentRepair::getMachineId, machineId);
        if (StringUtils.hasText(repairStatus)) qw.eq(EquipmentRepair::getRepairStatus, repairStatus);
        qw.orderByDesc(EquipmentRepair::getId);
        Page<EquipmentRepair> p = repairMapper.selectPage(new Page<>(page, pageSize), qw);
        fillNames(p.getRecords());
        return p;
    }

    /** 报修：生成维修单号，设备置为维修中 */
    @Transactional
    public EquipmentRepair create(EquipmentRepair r) {
        if (r.getMachineId() == null) throw new BizException("设备不能为空");
        r.setId(null);
        r.setRepairNo(codeGenerator.next("JX"));
        r.setReportDate(r.getReportDate() != null ? r.getReportDate() : LocalDate.now());
        if (!StringUtils.hasText(r.getRepairStatus())) r.setRepairStatus("pending");
        repairMapper.insert(r);
        updateMachineEquipStatus(r.getMachineId(), "repairing");
        return r;
    }

    @Transactional
    public EquipmentRepair update(EquipmentRepair r) {
        if (repairMapper.selectById(r.getId()) == null) throw new BizException(404, "维修工单不存在");
        repairMapper.updateById(r);
        return repairMapper.selectById(r.getId());
    }

    /** 状态流转：pending待维修 → repairing维修中 → done已完成（完成时设备恢复正常） */
    @Transactional
    public void changeStatus(Long id, String status) {
        EquipmentRepair r = repairMapper.selectById(id);
        if (r == null) throw new BizException(404, "维修工单不存在");
        r.setRepairStatus(status);
        if ("done".equals(status)) {
            r.setFinishDate(LocalDate.now());
            repairMapper.updateById(r);
            updateMachineEquipStatus(r.getMachineId(), "normal");
        } else {
            repairMapper.updateById(r);
            if ("repairing".equals(status)) updateMachineEquipStatus(r.getMachineId(), "repairing");
        }
    }

    @Transactional
    public void delete(Long id) {
        repairMapper.deleteById(id);
    }

    private void updateMachineEquipStatus(Long machineId, String equipStatus) {
        if (machineId == null) return;
        Machine machine = machineMapper.selectById(machineId);
        if (machine != null) {
            machine.setEquipStatus(equipStatus);
            machineMapper.updateById(machine);
        }
    }

    private void fillNames(List<EquipmentRepair> list) {
        if (list == null || list.isEmpty()) return;
        Map<Long, String[]> mm = nameFillUtil.machineMap(
                list.stream().map(EquipmentRepair::getMachineId).collect(Collectors.toList()));
        list.forEach(c -> {
            String[] arr = mm.get(c.getMachineId());
            if (arr != null) {
                c.setMachineCode(arr[0]);
                c.setMachineName(arr[1]);
            }
        });
    }
}
