-- ============================================================
-- 第二批关联修正：外键规范化（增量迁移）
-- #1 BOM工序 → process_id；#2 单位 → unit_id；#3 库位接库区 area_id；#4 物料引用序列号规则 serial_rule_id
-- ============================================================
SET NAMES utf8mb4;

ALTER TABLE `bom_item`
  ADD COLUMN `process_id` BIGINT DEFAULT NULL COMMENT '工序ID（外键，process 字段保留为冗余快照）' AFTER `process`;

ALTER TABLE `material`
  ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '单位ID（外键，unit 字段保留为冗余快照）' AFTER `unit`,
  ADD COLUMN `serial_rule_id` BIGINT DEFAULT NULL COMMENT '序列号规则ID（启用序列号时引用）' AFTER `status`;

ALTER TABLE `product`
  ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '单位ID（外键，unit 字段保留为冗余快照）' AFTER `unit`;

ALTER TABLE `location`
  ADD COLUMN `area_id` BIGINT DEFAULT NULL COMMENT '库区ID（可空，层级：仓库→库区→库位）' AFTER `warehouse_id`;
