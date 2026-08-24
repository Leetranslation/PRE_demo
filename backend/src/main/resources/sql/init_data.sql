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
('warehouse_type', '不良品仓', 'defective', 3, 1);
