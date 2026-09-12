-- ============================================================
-- Token 制造 ERP V2 阶段5：外协管理
-- 外协单（整单/工序）+ 领料单/入库单加外协类型
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 外协单主表
CREATE TABLE `outsource_order` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`       VARCHAR(32)   NOT NULL COMMENT '外协单号（WX+日期+流水）',
  `type`           VARCHAR(16)   NOT NULL DEFAULT 'whole' COMMENT '类型：whole整单外协/process工序外协',
  `order_id`       BIGINT        NOT NULL COMMENT '生产工单ID',
  `process_id`     BIGINT        DEFAULT NULL COMMENT '工序ID（工序外协时）',
  `supplier_id`    BIGINT        NOT NULL COMMENT '外协供应商ID',
  `quantity`       DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '外协数量',
  `delivery_date`  DATE          DEFAULT NULL COMMENT '交货日期',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending/approving/approved',
  `biz_status`     VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '业务状态：pending未开始/outsourcing外协中/finished已完工/voided已作废',
  `remark`         VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`order_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协单';

-- 外协单明细
CREATE TABLE `outsource_order_item` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `outsource_id` BIGINT        NOT NULL COMMENT '外协单ID',
  `item_id`      BIGINT        NOT NULL COMMENT '物料/产品ID',
  `item_type`    VARCHAR(16)   NOT NULL DEFAULT 'material' COMMENT '类型：material物料/product产品',
  `quantity`     DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '数量',
  `unit`         VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `remark`       VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_outsource` (`outsource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协单明细';

-- 领料单加外协类型（复用现有领料单）
ALTER TABLE `material_issue`
  ADD COLUMN `issue_type` VARCHAR(32) NOT NULL DEFAULT 'production' COMMENT '领料类型：production生产/outsource_whole整单外协/outsource_process工序外协' AFTER `issue_no`,
  ADD COLUMN `outsource_id` BIGINT DEFAULT NULL COMMENT '外协单ID（外协领料时）' AFTER `order_id`;

-- 入库单加外协类型（复用现有入库单）
ALTER TABLE `production_inbound`
  ADD COLUMN `inbound_type` VARCHAR(32) NOT NULL DEFAULT 'production' COMMENT '入库类型：production生产/outsource_whole整单外协/outsource_process工序外协' AFTER `inbound_no`,
  ADD COLUMN `outsource_id` BIGINT DEFAULT NULL COMMENT '外协单ID（外协入库时）' AFTER `order_id`;

SET FOREIGN_KEY_CHECKS = 1;
