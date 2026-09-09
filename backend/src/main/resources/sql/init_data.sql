-- ============================================================
-- Token 制造 ERP V1.0 初始化数据脚本
-- 依赖 schema.sql / schema_biz.sql / schema_inventory.sql 先执行
-- ============================================================

SET NAMES utf8mb4;

-- 超级管理员（用户名 admin，密码 admin123，BCrypt 哈希需后端生成，此处占位）
-- 说明：password 为 BCrypt 哈希，由后端初始化脚本写入，此处仅插入账号
INSERT INTO `user` (`username`, `password`, `real_name`, `status`, `remark`) VALUES
('admin', '$2a$10$placeholder_placeholder_placeholder_placeholder_', '超级管理员', 1, '初始化超级管理员');

-- 默认角色
INSERT INTO `role` (`code`, `name`, `data_scope`, `status`, `remark`) VALUES
('ADMIN', '管理员', 'all', 1, '超级管理员角色'),
('SALES', '销售', 'self', 1, '销售角色'),
('PURCHASE', '采购', 'self', 1, '采购角色'),
('PRODUCTION', '生产', 'self', 1, '生产角色'),
('WAREHOUSE', '仓库', 'self', 1, '仓库角色');

-- 超级管理员关联管理员角色
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- ============================================================
-- 权限码清单（模块:资源[:操作]，type 区分 menu/button）
-- 说明：后端 DataInitializer 会自动初始化，此 SQL 供全新部署时用；
--      若已存在同名 code 会因 uk_code 唯一索引冲突，请使用 INSERT IGNORE。
-- ============================================================
INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `path`, `sort`) VALUES
-- 基础资料 - 菜单
('base:customer', '客户档案', 'menu', '/base/customer', 1),
('base:supplier', '供应商档案', 'menu', '/base/supplier', 2),
('base:material', '物料档案', 'menu', '/base/material', 3),
('base:product', '产品档案', 'menu', '/base/product', 4),
('base:warehouse', '仓库', 'menu', '/base/warehouse', 5),
('base:location', '库位管理', 'menu', '/base/location', 6),
('base:bom', 'BOM管理', 'menu', '/base/bom', 7),
('base:process', '工序管理', 'menu', '/produceData/process', 8),
('base:processRoute', '工艺路线', 'menu', '/produceData/processRoute', 9),
('base:workshop', '车间管理', 'menu', '/produceData/workshop', 10),
('base:machine', '机台设备', 'menu', '/produceData/machine', 11),
('base:workCalendar', '工作日历', 'menu', '/produceData/workCalendar', 12),
('base:unit', '单位管理', 'menu', '/produceData/unit', 13),
('base:unitConversion', '多单位转换率', 'menu', '/produceData/unitConversion', 14),
('base:serialRule', '序列号规则', 'menu', '/produceData/serialRule', 15),
('base:area', '库区管理', 'menu', '/base/area', 16),
('production:reportWork', '报工管理', 'menu', '/production/reportWork', 17),
('production:reportWork:create', '报工新增', 'button', NULL, 0),
('production:reportWork:audit', '审工', 'button', NULL, 0),
('production:order:start', '工单开始/完工/作废', 'button', NULL, 0),
('production:schedule', '生产排产计划', 'menu', '/production/schedule', 18),
('production:schedule:create', '排产新增', 'button', NULL, 0),
('production:mps', '主生产计划', 'menu', '/production/mps', 19),
('production:mps:create', '主计划新增/生成工单', 'button', NULL, 0),
-- 基础资料 - 操作
('base:customer:create', '客户新增', 'button', NULL, 0),
('base:customer:update', '客户编辑/停用', 'button', NULL, 0),
('base:supplier:create', '供应商新增', 'button', NULL, 0),
('base:supplier:update', '供应商编辑/停用', 'button', NULL, 0),
('base:material:create', '物料新增', 'button', NULL, 0),
('base:material:update', '物料编辑/停用', 'button', NULL, 0),
('base:product:create', '产品新增', 'button', NULL, 0),
('base:product:update', '产品编辑/停用', 'button', NULL, 0),
('base:warehouse:create', '仓库新增', 'button', NULL, 0),
('base:warehouse:update', '仓库编辑/停用', 'button', NULL, 0),
('base:location:create', '库位新增', 'button', NULL, 0),
('base:location:update', '库位编辑', 'button', NULL, 0),
('base:bom:create', 'BOM新增', 'button', NULL, 0),
('base:bom:update', 'BOM编辑', 'button', NULL, 0),
('base:process:create', '工序新增', 'button', NULL, 0),
('base:process:update', '工序编辑/停用/批量设置', 'button', NULL, 0),
('base:processRoute:create', '工艺路线新增', 'button', NULL, 0),
('base:processRoute:update', '工艺路线编辑/停用/删除', 'button', NULL, 0),
('base:workshop:create', '车间新增', 'button', NULL, 0),
('base:workshop:update', '车间编辑/停用', 'button', NULL, 0),
('base:machine:create', '机台新增', 'button', NULL, 0),
('base:machine:update', '机台编辑/停用', 'button', NULL, 0),
('base:workCalendar:update', '工作日历批量设置', 'button', NULL, 0),
('base:unit:create', '单位新增', 'button', NULL, 0),
('base:unit:update', '单位编辑/停用', 'button', NULL, 0),
('base:unitConversion:create', '换算率新增', 'button', NULL, 0),
('base:unitConversion:update', '换算率编辑/停用', 'button', NULL, 0),
('base:serialRule:create', '序列号规则新增', 'button', NULL, 0),
('base:serialRule:update', '序列号规则编辑/停用', 'button', NULL, 0),
('base:area:create', '库区新增', 'button', NULL, 0),
('base:area:update', '库区编辑/停用', 'button', NULL, 0),
-- 库存 - 菜单
('inventory', '库存查询/流水', 'menu', '/inventory', 30),
('inventory:otherStock', '其他出入库', 'menu', '/inventory/otherStock', 31),
('inventory:otherStock:create', '其他出入库新增', 'button', NULL, 0),
('inventory:reserve', '锁库/解锁', 'button', NULL, 0),
-- MRP
('mrp', 'MRP计划', 'menu', '/mrp/index', 40),
('mrp:calculate', 'MRP计算', 'button', NULL, 0),
('mrp:confirm', 'MRP确认生成单据', 'button', NULL, 0),
-- 销售
('sales:order', '销售订单', 'menu', '/sales/order', 50),
('sales:quotation', '报价单', 'menu', '/sales/quotation', 51),
('sales:contract', '合同管理', 'menu', '/sales/contract', 52),
('sales:outbound', '销售出库单', 'menu', '/sales/outbound', 53),
('sales:return', '销售退货单', 'menu', '/sales/return', 54),
('sales:order:create', '销售订单新增', 'button', NULL, 0),
('sales:order:submit', '销售订单提交', 'button', NULL, 0),
('sales:order:approve', '销售订单审批/反审核', 'button', NULL, 0),
('sales:quotation:create', '报价单新增/转销售订单', 'button', NULL, 0),
('sales:quotation:approve', '报价单审批/反审核', 'button', NULL, 0),
('sales:contract:create', '合同新增/转销售订单', 'button', NULL, 0),
('sales:contract:approve', '合同审批/反审核', 'button', NULL, 0),
('sales:outbound:create', '销售出库', 'button', NULL, 0),
('sales:return:create', '销售退货', 'button', NULL, 0),
-- 采购
('purchase:order', '采购订单', 'menu', '/purchase/order', 60),
('purchase:requisition', '采购申请单', 'menu', '/purchase/requisition', 61),
('purchase:inquiry', '采购询价单', 'menu', '/purchase/inquiry', 62),
('purchase:inbound', '采购入库单', 'menu', '/purchase/inbound', 63),
('purchase:order:create', '采购订单新增', 'button', NULL, 0),
('purchase:order:update', '采购订单编辑', 'button', NULL, 0),
('purchase:order:submit', '采购订单提交', 'button', NULL, 0),
('purchase:order:approve', '采购订单审批/反审核', 'button', NULL, 0),
('purchase:requisition:create', '采购申请新增', 'button', NULL, 0),
('purchase:requisition:approve', '采购申请审批/反审核', 'button', NULL, 0),
('purchase:inquiry:create', '询价单新增', 'button', NULL, 0),
('purchase:inquiry:approve', '询价单审批/反审核', 'button', NULL, 0),
('purchase:inbound:create', '采购入库', 'button', NULL, 0),
-- 生产
('production:order', '生产工单', 'menu', '/production/order', 70),
('production:issue', '领料单', 'menu', '/production/issue', 71),
('production:return', '退料单', 'menu', '/production/return', 72),
('production:inbound', '生产入库单', 'menu', '/production/inbound', 73),
('production:order:create', '生产工单新增', 'button', NULL, 0),
('production:order:submit', '生产工单提交', 'button', NULL, 0),
('production:order:approve', '生产工单审批/反审核', 'button', NULL, 0),
('production:issue:create', '领料', 'button', NULL, 0),
('production:return:create', '退料', 'button', NULL, 0),
('production:inbound:create', '生产入库', 'button', NULL, 0),
-- 质量
('quality', '质量检验', 'menu', '/quality/index', 80),
('quality:create', '质检单新增', 'button', NULL, 0),
('quality:judge', '质检判定', 'button', NULL, 0),
-- 系统管理
('system:user', '用户管理', 'menu', '/system/user', 90),
('system:role', '角色管理', 'menu', '/system/role', 91),
('system:department', '部门管理', 'menu', '/system/department', 92);

-- ADMIN 角色关联全部权限（role_id=1 为默认管理员角色）
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `permission`;

-- 系统参数默认值
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`) VALUES
('default_currency', 'CNY', '默认币种'),
('default_tax_rate', '13', '默认税率(%)'),
('amount_scale', '2', '金额精度'),
('quantity_scale', '6', '数量精度'),
('price_scale', '6', '单价精度'),
('allow_self_approve', 'false', '是否允许制单人自审'),
('negative_stock_strategy', 'forbid', '库存负数策略：forbid禁止');

-- 数据字典：基础资料分类
INSERT INTO `dictionary` (`dict_type`, `dict_label`, `dict_value`, `sort`, `status`) VALUES
('customer_category', '普通客户', 'normal', 1, 1),
('customer_category', 'VIP客户', 'vip', 2, 1),
('supplier_category', '普通供应商', 'normal', 1, 1),
('supplier_category', '核心供应商', 'core', 2, 1),
('material_category', '原材料', 'raw', 1, 1),
('material_category', '半成品', 'semi', 2, 1),
('product_category', '成品', 'finished', 1, 1),
('unit', '个', 'pcs', 1, 1),
('unit', '公斤', 'kg', 2, 1),
('unit', '米', 'm', 3, 1),
('unit', '套', 'set', 4, 1),
('purchase_type', '备货采购', 'stock', 1, 1),
('purchase_type', '生产采购', 'production', 2, 1),
('purchase_type', 'MRP采购', 'mrp', 3, 1),
('production_type', '订单生产', 'order', 1, 1),
('production_type', '库存生产', 'stock', 2, 1),
('production_type', 'MRP生产', 'mrp', 3, 1),
('sales_type', '普通销售', 'normal', 1, 1),
('warehouse_type', '原材料仓', 'raw', 1, 1),
('warehouse_type', '成品仓', 'finished', 2, 1),
('warehouse_type', '不良品仓', 'defective', 3, 1),
('process_category', '加工', 'process', 1, 1),
('process_category', '检验', 'qc', 2, 1),
('process_category', '外协', 'outsourcing', 3, 1),
('process_price_type', '计件', 'piece', 1, 1),
('process_price_type', '计时', 'time', 2, 1),
('process_price_type', '固定价', 'fixed', 3, 1);
