-- ============================================================
-- Token 制造 ERP V2 第一档断环：不良品项档案 + 报工不良品字段
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 不良品项档案
CREATE TABLE `defect_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)   NOT NULL COMMENT '编码（BL+日期+流水）',
  `name`        VARCHAR(128)  NOT NULL COMMENT '不良品项名称',
  `category`    VARCHAR(64)   DEFAULT NULL COMMENT '不良分类',
  `status`      INT           NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='不良品项';

-- 报工单加不良品项关联字段
ALTER TABLE `report_work`
  ADD COLUMN `defect_item_id` BIGINT DEFAULT NULL COMMENT '不良品项ID' AFTER `defective_qty`,
  ADD COLUMN `defect_reason` VARCHAR(255) DEFAULT NULL COMMENT '不良原因' AFTER `defect_item_id`;

SET FOREIGN_KEY_CHECKS = 1;
