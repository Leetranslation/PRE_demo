package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.EquipmentCheck;
import com.token.erp.mapper.EquipmentCheckMapper;
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
public class EquipmentCheckService {

    private final EquipmentCheckMapper checkMapper;
    private final NameFillUtil nameFillUtil;

    public Page<EquipmentCheck> page(long page, long pageSize, Long machineId, String result) {
        LambdaQueryWrapper<EquipmentCheck> qw = new LambdaQueryWrapper<>();
        if (machineId != null) qw.eq(EquipmentCheck::getMachineId, machineId);
        if (StringUtils.hasText(result)) qw.eq(EquipmentCheck::getResult, result);
        qw.orderByDesc(EquipmentCheck::getId);
        Page<EquipmentCheck> p = checkMapper.selectPage(new Page<>(page, pageSize), qw);
        fillNames(p.getRecords());
        return p;
    }

    @Transactional
    public EquipmentCheck create(EquipmentCheck check) {
        if (check.getMachineId() == null) throw new BizException("设备不能为空");
        check.setId(null);
        if (check.getCheckDate() == null) check.setCheckDate(LocalDate.now());
        if (!StringUtils.hasText(check.getResult())) check.setResult("normal");
        checkMapper.insert(check);
        return check;
    }

    @Transactional
    public EquipmentCheck update(EquipmentCheck check) {
        if (checkMapper.selectById(check.getId()) == null) throw new BizException(404, "点检记录不存在");
        checkMapper.updateById(check);
        return checkMapper.selectById(check.getId());
    }

    @Transactional
    public void delete(Long id) {
        checkMapper.deleteById(id);
    }

    private void fillNames(List<EquipmentCheck> list) {
        if (list == null || list.isEmpty()) return;
        Map<Long, String[]> mm = nameFillUtil.machineMap(
                list.stream().map(EquipmentCheck::getMachineId).collect(Collectors.toList()));
        list.forEach(c -> {
            String[] arr = mm.get(c.getMachineId());
            if (arr != null) {
                c.setMachineCode(arr[0]);
                c.setMachineName(arr[1]);
            }
        });
    }
}
