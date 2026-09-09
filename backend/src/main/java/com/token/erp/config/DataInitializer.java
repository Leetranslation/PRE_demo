package com.token.erp.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.entity.Permission;
import com.token.erp.entity.Role;
import com.token.erp.entity.RolePermission;
import com.token.erp.entity.User;
import com.token.erp.entity.UserRole;
import com.token.erp.mapper.PermissionMapper;
import com.token.erp.mapper.RoleMapper;
import com.token.erp.mapper.RolePermissionMapper;
import com.token.erp.mapper.UserMapper;
import com.token.erp.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 完整权限码清单：name 为 menu（菜单权限）或 button（操作权限），code 为 模块:资源[:操作]
     */
    private static final String[][] PERMISSIONS = {
            // 基础资料 - 菜单
            {"base:customer", "客户档案", "menu", "/base/customer"},
            {"base:supplier", "供应商档案", "menu", "/base/supplier"},
            {"base:material", "物料档案", "menu", "/base/material"},
            {"base:product", "产品档案", "menu", "/base/product"},
            {"base:warehouse", "仓库", "menu", "/base/warehouse"},
            {"base:location", "库位管理", "menu", "/base/location"},
            {"base:bom", "BOM管理", "menu", "/base/bom"},
            {"base:process", "工序管理", "menu", "/produceData/process"},
            {"base:processRoute", "工艺路线", "menu", "/produceData/processRoute"},
            {"base:workshop", "车间管理", "menu", "/produceData/workshop"},
            {"base:machine", "机台设备", "menu", "/produceData/machine"},
            {"base:workCalendar", "工作日历", "menu", "/produceData/workCalendar"},
            {"base:unit", "单位管理", "menu", "/produceData/unit"},
            {"base:unitConversion", "多单位转换率", "menu", "/produceData/unitConversion"},
            {"base:serialRule", "序列号规则", "menu", "/produceData/serialRule"},
            {"base:area", "库区管理", "menu", "/base/area"},
            {"production:reportWork", "报工管理", "menu", "/production/reportWork"},
            {"production:reportWork:create", "报工新增", "button", null},
            {"production:reportWork:audit", "审工", "button", null},
            {"production:order:start", "工单开始/完工/作废", "button", null},
            {"production:schedule", "生产排产计划", "menu", "/production/schedule"},
            {"production:schedule:create", "排产新增", "button", null},
            {"production:mps", "主生产计划", "menu", "/production/mps"},
            {"production:mps:create", "主计划新增/生成工单", "button", null},
            // 基础资料 - 操作
            {"base:customer:create", "客户新增", "button", null},
            {"base:customer:update", "客户编辑/停用", "button", null},
            {"base:supplier:create", "供应商新增", "button", null},
            {"base:supplier:update", "供应商编辑/停用", "button", null},
            {"base:material:create", "物料新增", "button", null},
            {"base:material:update", "物料编辑/停用", "button", null},
            {"base:product:create", "产品新增", "button", null},
            {"base:product:update", "产品编辑/停用", "button", null},
            {"base:warehouse:create", "仓库新增", "button", null},
            {"base:warehouse:update", "仓库编辑/停用", "button", null},
            {"base:location:create", "库位新增", "button", null},
            {"base:location:update", "库位编辑", "button", null},
            {"base:bom:create", "BOM新增", "button", null},
            {"base:bom:update", "BOM编辑", "button", null},
            {"base:process:create", "工序新增", "button", null},
            {"base:process:update", "工序编辑/停用/批量设置", "button", null},
            {"base:processRoute:create", "工艺路线新增", "button", null},
            {"base:processRoute:update", "工艺路线编辑/停用/删除", "button", null},
            {"base:workshop:create", "车间新增", "button", null},
            {"base:workshop:update", "车间编辑/停用", "button", null},
            {"base:machine:create", "机台新增", "button", null},
            {"base:machine:update", "机台编辑/停用", "button", null},
            {"base:workCalendar:update", "工作日历批量设置", "button", null},
            {"base:unit:create", "单位新增", "button", null},
            {"base:unit:update", "单位编辑/停用", "button", null},
            {"base:unitConversion:create", "换算率新增", "button", null},
            {"base:unitConversion:update", "换算率编辑/停用", "button", null},
            {"base:serialRule:create", "序列号规则新增", "button", null},
            {"base:serialRule:update", "序列号规则编辑/停用", "button", null},
            {"base:area:create", "库区新增", "button", null},
            {"base:area:update", "库区编辑/停用", "button", null},
            // 库存 - 菜单
            {"inventory", "库存查询/流水", "menu", "/inventory"},
            {"inventory:otherStock", "其他出入库", "menu", "/inventory/otherStock"},
            {"inventory:otherStock:create", "其他出入库新增", "button", null},
            {"inventory:reserve", "锁库/解锁", "button", null},
            // MRP - 菜单 + 操作
            {"mrp", "MRP计划", "menu", "/mrp/index"},
            {"mrp:calculate", "MRP计算", "button", null},
            {"mrp:confirm", "MRP确认生成单据", "button", null},
            // 销售 - 菜单 + 操作
            {"sales:order", "销售订单", "menu", "/sales/order"},
            {"sales:quotation", "报价单", "menu", "/sales/quotation"},
            {"sales:contract", "合同管理", "menu", "/sales/contract"},
            {"sales:invoice", "销售发票", "menu", "/sales/invoice"},
            {"sales:verification", "销售核销", "menu", "/sales/verification"},
            {"sales:outbound", "销售出库单", "menu", "/sales/outbound"},
            {"sales:return", "销售退货单", "menu", "/sales/return"},
            {"sales:afterSale", "售后管理", "menu", "/sales/afterSale"},
            {"sales:stocking", "销售备货单", "menu", "/sales/stocking"},
            {"sales:designCenter", "设计中心", "menu", "/sales/designCenter"},
            {"sales:packing", "装箱单", "menu", "/sales/packing"},
            {"sales:order:create", "销售订单新增", "button", null},
            {"sales:order:submit", "销售订单提交", "button", null},
            {"sales:order:approve", "销售订单审批/反审核", "button", null},
            {"sales:quotation:create", "报价单新增/转销售订单", "button", null},
            {"sales:quotation:approve", "报价单审批/反审核", "button", null},
            {"sales:contract:create", "合同新增/转销售订单", "button", null},
            {"sales:contract:approve", "合同审批/反审核", "button", null},
            {"sales:invoice:create", "销售发票新增", "button", null},
            {"sales:invoice:approve", "销售发票审批/反审核", "button", null},
            {"sales:verification:create", "销售核销新增", "button", null},
            {"sales:verification:approve", "销售核销审批/反审核", "button", null},
            {"sales:outbound:create", "销售出库", "button", null},
            {"sales:return:create", "销售退货", "button", null},
            {"sales:afterSale:create", "售后新增/状态更新", "button", null},
            {"sales:stocking:create", "备货单新增/审批", "button", null},
            {"sales:designCenter:create", "设计单新增/状态更新", "button", null},
            {"sales:packing:create", "装箱单新增", "button", null},
            // 采购 - 菜单 + 操作
            {"purchase:order", "采购订单", "menu", "/purchase/order"},
            {"purchase:requisition", "采购申请单", "menu", "/purchase/requisition"},
            {"purchase:inquiry", "采购询价单", "menu", "/purchase/inquiry"},
            {"purchase:invoice", "采购发票", "menu", "/purchase/invoice"},
            {"purchase:verification", "采购核销", "menu", "/purchase/verification"},
            {"purchase:return", "采购退货单", "menu", "/purchase/return"},
            {"purchase:inbound", "采购入库单", "menu", "/purchase/inbound"},
            {"purchase:order:create", "采购订单新增", "button", null},
            {"purchase:order:update", "采购订单编辑", "button", null},
            {"purchase:order:submit", "采购订单提交", "button", null},
            {"purchase:order:approve", "采购订单审批/反审核", "button", null},
            {"purchase:requisition:create", "采购申请新增", "button", null},
            {"purchase:requisition:approve", "采购申请审批/反审核", "button", null},
            {"purchase:inquiry:create", "询价单新增", "button", null},
            {"purchase:inquiry:approve", "询价单审批/反审核", "button", null},
            {"purchase:invoice:create", "采购发票新增", "button", null},
            {"purchase:invoice:approve", "采购发票审批/反审核", "button", null},
            {"purchase:verification:create", "采购核销新增", "button", null},
            {"purchase:verification:approve", "采购核销审批/反审核", "button", null},
            {"purchase:return:create", "采购退货", "button", null},
            {"purchase:inbound:create", "采购入库", "button", null},
            // 生产 - 菜单 + 操作
            {"production:order", "生产工单", "menu", "/production/order"},
            {"production:issue", "领料单", "menu", "/production/issue"},
            {"production:return", "退料单", "menu", "/production/return"},
            {"production:inbound", "生产入库单", "menu", "/production/inbound"},
            {"production:order:create", "生产工单新增", "button", null},
            {"production:order:submit", "生产工单提交", "button", null},
            {"production:order:approve", "生产工单审批/反审核", "button", null},
            {"production:issue:create", "领料", "button", null},
            {"production:return:create", "退料", "button", null},
            {"production:inbound:create", "生产入库", "button", null},
            // 质量 - 菜单 + 操作
            {"quality", "质量检验", "menu", "/quality/index"},
            {"quality:create", "质检单新增", "button", null},
            {"quality:judge", "质检判定", "button", null},
            // 系统管理 - 菜单
            {"system:user", "用户管理", "menu", "/system/user"},
            {"system:role", "角色管理", "menu", "/system/role"},
            {"system:department", "部门管理", "menu", "/system/department"},
    };

    @Override
    @Transactional
    public void run(String... args) {
        // 1. 初始化权限码
        List<Permission> allPermissions = initPermissions();

        // 2. 初始化 ADMIN 角色
        Role adminRole = initAdminRole();

        // 3. 给 ADMIN 角色分配全部权限
        assignAllPermissions(adminRole.getId(), allPermissions);

        // 4. 初始化 admin 用户并关联 ADMIN 角色
        User admin = initAdminUser();
        bindAdminRole(admin.getId(), adminRole.getId());
    }

    private List<Permission> initPermissions() {
        List<Permission> result = new ArrayList<>();
        for (String[] p : PERMISSIONS) {
            String code = p[0];
            Permission existing = permissionMapper.selectOne(
                    new LambdaQueryWrapper<Permission>().eq(Permission::getCode, code));
            if (existing == null) {
                Permission perm = new Permission();
                perm.setCode(code);
                perm.setName(p[1]);
                perm.setType(p[2]);
                perm.setPath(p[3]);
                perm.setParentId(null);
                perm.setSort(0);
                permissionMapper.insert(perm);
                result.add(perm);
                log.info("初始化权限码: {}", code);
            } else {
                result.add(existing);
            }
        }
        return result;
    }

    private Role initAdminRole() {
        Role role = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, "ADMIN"));
        if (role == null) {
            role = new Role();
            role.setCode("ADMIN");
            role.setName("管理员");
            role.setDataScope("all");
            role.setStatus(1);
            role.setRemark("超级管理员角色");
            roleMapper.insert(role);
            log.info("初始化管理员角色 ADMIN");
        }
        return role;
    }

    private void assignAllPermissions(Long roleId, List<Permission> permissions) {
        for (Permission perm : permissions) {
            Long count = rolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, roleId)
                            .eq(RolePermission::getPermissionId, perm.getId()));
            if (count == 0) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(perm.getId());
                rolePermissionMapper.insert(rp);
            }
        }
        log.info("ADMIN 角色已分配 {} 项权限", permissions.size());
    }

    private User initAdminUser() {
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("超级管理员");
            admin.setStatus(1);
            admin.setRemark("初始化超级管理员");
            userMapper.insert(admin);
            log.info("已创建超级管理员 admin / admin123");
        } else if (admin.getPassword() != null && admin.getPassword().contains("placeholder")) {
            admin.setPassword(passwordEncoder.encode("admin123"));
            userMapper.updateById(admin);
            log.info("已重置超级管理员密码为 admin123");
        }
        return admin;
    }

    private void bindAdminRole(Long userId, Long roleId) {
        Long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, roleId));
        if (count == 0) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
            log.info("已关联 admin 用户到 ADMIN 角色");
        }
    }
}
