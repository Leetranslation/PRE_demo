-- ============================================================
-- ERP 测试库最小基础资料（阶段0 测试数据）
-- 铁律：USE token_erp_test 限定测试库，禁止误操作正式库
-- 幂等：按 code 唯一，重复执行不重复插入
-- ============================================================
USE token_erp_test;

-- 仓库
INSERT INTO warehouse (code, name, type, status, deleted)
SELECT 'TEST_WH01', '测试主仓库', 'normal', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM warehouse WHERE code='TEST_WH01');

-- 库位（依赖仓库）
INSERT INTO location (warehouse_id, code, name, status, deleted)
SELECT (SELECT id FROM warehouse WHERE code='TEST_WH01'), 'TEST_LOC01', 'A区-01', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM location WHERE code='TEST_LOC01');

-- 客户
INSERT INTO customer (code, name, category, status, deleted)
SELECT 'TEST_CUS001', '测试客户A', 'general', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM customer WHERE code='TEST_CUS001');

-- 供应商
INSERT INTO supplier (code, name, category, status, deleted)
SELECT 'TEST_SUP001', '测试供应商A', 'general', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE code='TEST_SUP001');

-- 物料（采购件）
INSERT INTO material (code, name, category, unit, spec, supply_type, inventory_manage, batch_manage, safety_stock, status, deleted)
SELECT 'TEST_MAT001', '测试原材料A', 'raw', 'pcs', '10x10', 'purchase', 1, 'none', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM material WHERE code='TEST_MAT001');

-- 产品（自制件，需 BOM）
INSERT INTO product (code, name, category, unit, spec, inventory_manage, batch_manage, status, deleted)
SELECT 'TEST_PRO001', '测试成品A', 'finished', 'pcs', '100x100', 1, 'none', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM product WHERE code='TEST_PRO001');
