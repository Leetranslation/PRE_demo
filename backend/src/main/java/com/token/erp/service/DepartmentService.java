package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.common.BizException;
import com.token.erp.entity.Department;
import com.token.erp.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;

    public List<Department> list() {
        return departmentMapper.selectList(
                new LambdaQueryWrapper<Department>()
                        .orderByAsc(Department::getSort)
                        .orderByAsc(Department::getId));
    }

    public Department create(Department dept) {
        if (!StringUtils.hasText(dept.getCode())) throw new BizException("部门编码不能为空");
        if (!StringUtils.hasText(dept.getName())) throw new BizException("部门名称不能为空");
        dept.setId(null);
        dept.setParentId(dept.getParentId() == null ? 0L : dept.getParentId());
        dept.setSort(dept.getSort() == null ? 0 : dept.getSort());
        dept.setStatus(dept.getStatus() == null ? 1 : dept.getStatus());
        departmentMapper.insert(dept);
        return dept;
    }

    public Department update(Department dept) {
        if (departmentMapper.selectById(dept.getId()) == null) throw new BizException(404, "部门不存在");
        departmentMapper.updateById(dept);
        return departmentMapper.selectById(dept.getId());
    }
}
