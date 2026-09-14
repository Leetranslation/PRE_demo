-- ============================================================
-- 阶段7 成本核算：物料标准成本 + 工单成本字段
-- ============================================================
SET NAMES utf8mb4;

ALTER TABLE `material`
  ADD COLUMN `standard_cost` DECIMAL(18,6) DEFAULT NULL COMMENT '标准成本单价（成本核算用）' AFTER `min_stock`;

ALTER TABLE `product`
  ADD COLUMN `standard_cost` DECIMAL(18,6) DEFAULT NULL COMMENT '标准成本单价（成本核算用）' AFTER `max_stock`;

ALTER TABLE `production_order`
  ADD COLUMN `material_cost` DECIMAL(18,2) DEFAULT NULL COMMENT '材料成本' AFTER `completed_qty`,
  ADD COLUMN `labor_cost` DECIMAL(18,2) DEFAULT NULL COMMENT '人工成本' AFTER `material_cost`,
  ADD COLUMN `overhead_cost` DECIMAL(18,2) DEFAULT NULL COMMENT '制造费用' AFTER `labor_cost`,
  ADD COLUMN `total_cost` DECIMAL(18,2) DEFAULT NULL COMMENT '总成本' AFTER `overhead_cost`,
  ADD COLUMN `unit_cost` DECIMAL(18,6) DEFAULT NULL COMMENT '单位成本' AFTER `total_cost`;
