-- ============================================================
-- 第二批关联修正 补全：单位表初始化 + unit_id 回填
-- 背景：schema_refactor.sql 只加了 unit_id 列，未初始化单位基础数据、未回填外键
-- ============================================================
SET NAMES utf8mb4;

-- 1. 初始化常用单位（幂等：不存在才插入）
INSERT INTO unit(code, name, status)
SELECT 'pcs', '个', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'pcs');
INSERT INTO unit(code, name, status)
SELECT 'kg', '公斤', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'kg');
INSERT INTO unit(code, name, status)
SELECT 'g', '克', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'g');
INSERT INTO unit(code, name, status)
SELECT 't', '吨', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 't');
INSERT INTO unit(code, name, status)
SELECT 'm', '米', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'm');
INSERT INTO unit(code, name, status)
SELECT 'cm', '厘米', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'cm');
INSERT INTO unit(code, name, status)
SELECT 'l', '升', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'l');
INSERT INTO unit(code, name, status)
SELECT 'tao', '套', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'tao');
INSERT INTO unit(code, name, status)
SELECT 'xiang', '箱', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'xiang');
INSERT INTO unit(code, name, status)
SELECT 'jian', '件', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM unit WHERE code = 'jian');

-- 2. 回填物料 unit_id（先按 code 忽略大小写匹配，再按 name 匹配）
UPDATE material m JOIN unit u ON LOWER(m.unit) = LOWER(u.code)
SET m.unit_id = u.id
WHERE m.unit_id IS NULL AND m.deleted = 0;

UPDATE material m JOIN unit u ON m.unit = u.name
SET m.unit_id = u.id
WHERE m.unit_id IS NULL AND m.deleted = 0;

-- 3. 回填产品 unit_id
UPDATE product p JOIN unit u ON LOWER(p.unit) = LOWER(u.code)
SET p.unit_id = u.id
WHERE p.unit_id IS NULL AND p.deleted = 0;

UPDATE product p JOIN unit u ON p.unit = u.name
SET p.unit_id = u.id
WHERE p.unit_id IS NULL AND p.deleted = 0;
