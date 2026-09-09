-- ============================================================
-- Token 制造 ERP V2 第一档断环：半成品加工单（复用生产工单）
-- 生产工单加 order_type 区分：product 成品 / semi 半成品
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `production_order`
  ADD COLUMN `order_type` VARCHAR(16) NOT NULL DEFAULT 'product' COMMENT '工单类型：product成品/semi半成品' AFTER `order_no`;

SET FOREIGN_KEY_CHECKS = 1;
