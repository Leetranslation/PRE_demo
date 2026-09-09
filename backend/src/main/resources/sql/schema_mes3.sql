-- ============================================================
-- Token 制造 ERP V2.0 生产工单关联字段（车间/机台/工艺路线）
-- 在既有 production_order 表上新增字段，打通生产数据与生产工单的关联
-- ============================================================

SET NAMES utf8mb4;

-- 生产工单增加车间/机台/工艺路线关联字段
ALTER TABLE `production_order`
    ADD COLUMN `workshop_id`      BIGINT DEFAULT NULL COMMENT '车间ID' AFTER `department_id`,
    ADD COLUMN `machine_id`       BIGINT DEFAULT NULL COMMENT '机台ID' AFTER `workshop_id`,
    ADD COLUMN `process_route_id` BIGINT DEFAULT NULL COMMENT '工艺路线ID' AFTER `machine_id`;

-- 索引
ALTER TABLE `production_order`
    ADD KEY `idx_workshop` (`workshop_id`),
    ADD KEY `idx_machine` (`machine_id`),
    ADD KEY `idx_route` (`process_route_id`);
