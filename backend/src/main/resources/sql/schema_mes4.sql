-- ============================================================
-- Token 制造 ERP V2.0 生产工单状态机升级（子状态字段 + 紧急程度 + 外协状态）
-- 在既有 production_order 表上新增子状态字段
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE `production_order`
    ADD COLUMN `finish_status`    VARCHAR(16) NOT NULL DEFAULT 'unfinished' COMMENT '完工状态：unfinished未完工/finished已完工' AFTER `biz_status`,
    ADD COLUMN `inbound_status`   VARCHAR(16) NOT NULL DEFAULT 'none' COMMENT '入库状态：none未入库/partial部分入库/all全部入库' AFTER `finish_status`,
    ADD COLUMN `issue_status`     VARCHAR(16) NOT NULL DEFAULT 'none' COMMENT '领料状态：none未领料/partial部分领料/all全部领料' AFTER `inbound_status`,
    ADD COLUMN `qc_status`        VARCHAR(16) NOT NULL DEFAULT 'not_needed' COMMENT '质检状态：not_needed无需质检/none未质检/pending待质检/ing质检中/qualified合格/unqualified不合格' AFTER `issue_status`,
    ADD COLUMN `machine_status`   VARCHAR(16) NOT NULL DEFAULT 'unlinked' COMMENT '机台状态：unlinked未关联/linked已关联' AFTER `qc_status`,
    ADD COLUMN `urgency`          VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT '紧急程度：normal普通/urgent紧急/very_urgent非常紧急' AFTER `machine_status`,
    ADD COLUMN `outsource_status` VARCHAR(16) NOT NULL DEFAULT 'none' COMMENT '外协状态：none无/process_outsource工序外协/whole_outsource整单外协' AFTER `urgency`;
