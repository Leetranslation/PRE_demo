package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Machine;
import com.token.erp.mapper.MachineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineMapper machineMapper;
    private final CodeGenerator codeGenerator;

    public Page<Machine> page(long page, long pageSize, String keyword, Integer status, Long workshopId, String equipStatus) {
        LambdaQueryWrapper<Machine> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Machine::getName, keyword).or().like(Machine::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Machine::getStatus, status);
        }
        if (workshopId != null) {
            qw.eq(Machine::getWorkshopId, workshopId);
        }
        if (StringUtils.hasText(equipStatus)) {
            qw.eq(Machine::getEquipStatus, equipStatus);
        }
        qw.orderByDesc(Machine::getId);
        return machineMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Machine create(Machine machine) {
        if (!StringUtils.hasText(machine.getName())) {
            throw new BizException("机台名称不能为空");
        }
        machine.setId(null);
        machine.setCode(codeGenerator.next("JT"));
        machine.setStatus(machine.getStatus() == null ? 1 : machine.getStatus());
        machine.setEquipStatus(StringUtils.hasText(machine.getEquipStatus()) ? machine.getEquipStatus() : "normal");
        machineMapper.insert(machine);
        return machine;
    }

    public Machine update(Machine machine) {
        if (machineMapper.selectById(machine.getId()) == null) {
            throw new BizException(404, "机台不存在");
        }
        machine.setCode(null);
        machineMapper.updateById(machine);
        return machineMapper.selectById(machine.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (machineMapper.selectById(id) == null) {
            throw new BizException(404, "机台不存在");
        }
        Machine upd = new Machine();
        upd.setId(id);
        upd.setStatus(status);
        machineMapper.updateById(upd);
    }
}
