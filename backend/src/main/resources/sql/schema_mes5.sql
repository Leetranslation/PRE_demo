-- ============================================================
-- Token 制造 ERP V2.0 报工单表
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 报工单主表
CREATE TABLE `report_work` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_no`            VARCHAR(32)   NOT NULL COMMENT '报工单号（BW+日期+流水）',
  `work_order_id`        BIGINT        NOT NULL COMMENT '生产工单ID',
  `process_id`           BIGINT        NOT NULL COMMENT '工序ID',
  `process_route_item_id` BIGINT       DEFAULT NULL COMMENT '工艺路线明细ID',
  `machine_id`           BIGINT        DEFAULT NULL COMMENT '机台ID',
  `workshop_id`          BIGINT        DEFAULT NULL COMMENT '车间ID',
  `reporter_id`          BIGINT        DEFAULT NULL COMMENT '报工人（用户ID）',
  `reporter_name`        VARCHAR(64)   DEFAULT NULL COMMENT '报工人姓名',
  `report_date`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报工时间',
  `quantity`             DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '报工数量',
  `qualified_qty`        DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '良品数量',
  `defective_qty`        DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '不良品数量',
  `work_hours`           DECIMAL(18,6) DEFAULT NULL COMMENT '工时（小时）',
  `start_time`           DATETIME      DEFAULT NULL COMMENT '工序开始时间',
  `end_time`             DATETIME      DEFAULT NULL COMMENT '工序结束时间',
  `audit_status`         VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审工状态：pending待审/approved已审/rejected驳回',
  `auditor_id`           BIGINT        DEFAULT NULL COMMENT '审核人',
  `auditor_name`         VARCHAR(64)   DEFAULT NULL COMMENT '审核人姓名',
  `audit_time`           DATETIME      DEFAULT NULL COMMENT '审核时间',
  `audit_remark`         VARCHAR(255)  DEFAULT NULL COMMENT '审核意见',
  `remark`               VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`              TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`            VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`            VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_no` (`report_no`),
  KEY `idx_order` (`work_order_id`),
  KEY `idx_process` (`process_id`),
  KEY `idx_audit` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工单';

SET FOREIGN_KEY_CHECKS = 1;
