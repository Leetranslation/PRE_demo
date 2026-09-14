-- ============================================================
-- 设备管理（TPM）：设备台账扩展 + 点检 + 保养 + 维修
-- 说明：设备台账复用 machine 表（机台即设备），扩展设备管理字段
-- ============================================================
SET NAMES utf8mb4;

-- 1. 扩展 machine 为完整设备台账
ALTER TABLE `machine`
  ADD COLUMN `model` VARCHAR(64) DEFAULT NULL COMMENT '型号' AFTER `type`,
  ADD COLUMN `manufacturer` VARCHAR(128) DEFAULT NULL COMMENT '厂商' AFTER `model`,
  ADD COLUMN `purchase_date` DATE DEFAULT NULL COMMENT '购置日期' AFTER `manufacturer`,
  ADD COLUMN `purchase_price` DECIMAL(18,2) DEFAULT NULL COMMENT '购置价格' AFTER `purchase_date`,
  ADD COLUMN `warranty_until` DATE DEFAULT NULL COMMENT '保修期至' AFTER `purchase_price`,
  ADD COLUMN `equip_status` VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT '设备状态：normal正常/repairing维修中/stopped停用/scrapped报废' AFTER `warranty_until`,
  ADD COLUMN `maintenance_cycle` INT DEFAULT NULL COMMENT '保养周期（天）' AFTER `equip_status`,
  ADD COLUMN `last_maintenance_date` DATE DEFAULT NULL COMMENT '上次保养日期' AFTER `maintenance_cycle`;

-- 2. 设备点检记录
CREATE TABLE IF NOT EXISTS `equipment_check` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `machine_id`  BIGINT       NOT NULL COMMENT '设备ID',
  `check_date`  DATE         NOT NULL COMMENT '点检日期',
  `check_item`  VARCHAR(128) DEFAULT NULL COMMENT '点检项目',
  `result`      VARCHAR(16)  NOT NULL DEFAULT 'normal' COMMENT '结果：normal正常/abnormal异常',
  `checker`     VARCHAR(64)  DEFAULT NULL COMMENT '点检人',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_machine` (`machine_id`),
  KEY `idx_date` (`check_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备点检记录';

-- 3. 设备保养记录/计划
CREATE TABLE IF NOT EXISTS `equipment_maintenance` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `machine_id`    BIGINT       NOT NULL COMMENT '设备ID',
  `plan_date`     DATE         DEFAULT NULL COMMENT '计划保养日期',
  `actual_date`   DATE         DEFAULT NULL COMMENT '实际保养日期',
  `content`       VARCHAR(255) DEFAULT NULL COMMENT '保养内容',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'planned' COMMENT 'planned计划/done完成',
  `maintainer`    VARCHAR(64)  DEFAULT NULL COMMENT '保养人',
  `remark`        VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_machine` (`machine_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备保养记录';

-- 4. 设备维修工单
CREATE TABLE IF NOT EXISTS `equipment_repair` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `repair_no`      VARCHAR(32)   NOT NULL COMMENT '维修单号（JX+日期+流水）',
  `machine_id`     BIGINT        NOT NULL COMMENT '设备ID',
  `fault_desc`     VARCHAR(255)  DEFAULT NULL COMMENT '故障描述',
  `report_date`    DATE          DEFAULT NULL COMMENT '报修日期',
  `repairer`       VARCHAR(64)   DEFAULT NULL COMMENT '维修人',
  `repair_status`  VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending待维修/repairing维修中/done已完成',
  `finish_date`    DATE          DEFAULT NULL COMMENT '完成日期',
  `downtime_hours` DECIMAL(10,2) DEFAULT NULL COMMENT '停机时长（小时）',
  `cost`           DECIMAL(18,2) DEFAULT NULL COMMENT '维修费用',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)   DEFAULT NULL,
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)   DEFAULT NULL,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repair_no` (`repair_no`),
  KEY `idx_machine` (`machine_id`),
  KEY `idx_status` (`repair_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维修工单';
