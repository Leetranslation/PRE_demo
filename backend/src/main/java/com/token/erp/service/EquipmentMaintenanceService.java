package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.EquipmentMaintenance;
import com.token.erp.entity.Machine;
import com.token.erp.mapper.EquipmentMaintenanceMapper;
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
public class EquipmentMaintenanceService {

    private final EquipmentMaintenanceMapper maintenanceMapper;
    private final MachineMapper machineMapper;
    private final NameFillUtil nameFillUtil;

    public Page<EquipmentMaintenance> page(long page, long pageSize, Long machineId, String status) {
        LambdaQueryWrapper<EquipmentMaintenance> qw = new LambdaQueryWrapper<>();
        if (machineId != null) qw.eq(EquipmentMaintenance::getMachineId, machineId);
        if (StringUtils.hasText(status)) qw.eq(EquipmentMaintenance::getStatus, status);
        qw.orderByDesc(EquipmentMaintenance::getId);
        Page<EquipmentMaintenance> p = maintenanceMapper.selectPage(new Page<>(page, pageSize), qw);
        fillNames(p.getRecords());
        return p;
    }

    @Transactional
    public EquipmentMaintenance create(EquipmentMaintenance m) {
        if (m.getMachineId() == null) throw new BizException("设备不能为空");
        m.setId(null);
        if (!StringUtils.hasText(m.getStatus())) m.setStatus("planned");
        maintenanceMapper.insert(m);
        return m;
    }

    @Transactional
    public EquipmentMaintenance update(EquipmentMaintenance m) {
        if (maintenanceMapper.selectById(m.getId()) == null) throw new BizException(404, "保养记录不存在");
        maintenanceMapper.updateById(m);
        return maintenanceMapper.selectById(m.getId());
    }

    /** 完成保养：回写设备上次保养日期 */
    @Transactional
    public void complete(Long id) {
        EquipmentMaintenance m = maintenanceMapper.selectById(id);
        if (m == null) throw new BizException(404, "保养记录不存在");
        if ("done".equals(m.getStatus())) throw new BizException("该保养已完成");
        LocalDate today = LocalDate.now();
        m.setStatus("done");
        m.setActualDate(today);
        maintenanceMapper.updateById(m);

        Machine machine = machineMapper.selectById(m.getMachineId());
        if (machine != null) {
            machine.setLastMaintenanceDate(today);
            machineMapper.updateById(machine);
        }
    }

    @Transactional
    public void delete(Long id) {
        maintenanceMapper.deleteById(id);
    }

    private void fillNames(List<EquipmentMaintenance> list) {
        if (list == null || list.isEmpty()) return;
        Map<Long, String[]> mm = nameFillUtil.machineMap(
                list.stream().map(EquipmentMaintenance::getMachineId).collect(Collectors.toList()));
        list.forEach(c -> {
            String[] arr = mm.get(c.getMachineId());
            if (arr != null) {
                c.setMachineCode(arr[0]);
                c.setMachineName(arr[1]);
            }
        });
    }
}
