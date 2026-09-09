-- ============================================================
-- Token 制造 ERP V2.0 生产排产计划 + 主生产计划
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 排产计划
CREATE TABLE `schedule_plan` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `schedule_no`    VARCHAR(32)  NOT NULL COMMENT '排产单号（PC+日期+流水）',
  `work_order_id`  BIGINT       NOT NULL COMMENT '生产工单ID',
  `machine_id`     BIGINT       DEFAULT NULL COMMENT '机台ID',
  `workshop_id`    BIGINT       DEFAULT NULL COMMENT '车间ID',
  `plan_start`     DATETIME     DEFAULT NULL COMMENT '计划开始时间',
  `plan_end`       DATETIME     DEFAULT NULL COMMENT '计划结束时间',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'planned' COMMENT '状态：planned已排产/started已开始/finished已完成/cancelled已取消',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_no` (`schedule_no`),
  KEY `idx_order` (`work_order_id`),
  KEY `idx_machine` (`machine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产排产计划';

-- 主生产计划
CREATE TABLE `mps_plan` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `mps_no`         VARCHAR(32)   NOT NULL COMMENT '主生产计划单号（MP+日期+流水）',
  `product_id`     BIGINT        NOT NULL COMMENT '产品ID',
  `quantity`       DECIMAL(18,6) NOT NULL COMMENT '计划数量',
  `plan_date`      DATE          NOT NULL COMMENT '计划生产日期',
  `workshop_id`    BIGINT        DEFAULT NULL COMMENT '车间ID',
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'planned' COMMENT '状态：planned未生成/confirmed已生成工单',
  `work_order_id`  BIGINT        DEFAULT NULL COMMENT '生成的工单ID',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mps_no` (`mps_no`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主生产计划';

SET FOREIGN_KEY_CHECKS = 1;
