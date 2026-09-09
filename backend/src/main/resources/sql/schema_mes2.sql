-- ============================================================
-- Token 制造 ERP V2.0 MES 基础数据（第二批：车间/机台/工作日历/单位/序列号规则/库区）
-- 依赖 schema.sql、schema_mes.sql 先执行
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 车间
-- ------------------------------------------------------------
CREATE TABLE `workshop` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)  NOT NULL COMMENT '车间编码（CJ+日期+流水）',
  `name`        VARCHAR(64)  NOT NULL COMMENT '车间名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '车间描述',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车间';

-- ------------------------------------------------------------
-- 机台设备
-- ------------------------------------------------------------
CREATE TABLE `machine` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`         VARCHAR(32)  NOT NULL COMMENT '机台编号（JT+日期+流水）',
  `name`         VARCHAR(64)  NOT NULL COMMENT '机台名称',
  `workshop_id`  BIGINT       DEFAULT NULL COMMENT '所属车间ID',
  `type`         VARCHAR(32)  DEFAULT NULL COMMENT '机台类型（字典）',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_workshop` (`workshop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机台设备';

-- ------------------------------------------------------------
-- 工作日历
-- ------------------------------------------------------------
CREATE TABLE `work_calendar` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_date`  DATE         NOT NULL COMMENT '日期',
  `is_workday` TINYINT      NOT NULL DEFAULT 1 COMMENT '是否工作日：1是 0否',
  `remark`     VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date` (`work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作日历';

-- ------------------------------------------------------------
-- 单位
-- ------------------------------------------------------------
CREATE TABLE `unit` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)  NOT NULL COMMENT '单位编码',
  `name`        VARCHAR(64)  NOT NULL COMMENT '单位名称',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单位';

-- 多单位转换率
CREATE TABLE `unit_conversion` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `unit_id`       BIGINT        NOT NULL COMMENT '源单位ID',
  `target_unit_id` BIGINT       NOT NULL COMMENT '目标单位ID',
  `rate`          DECIMAL(18,6) NOT NULL COMMENT '换算率（1源单位 = rate目标单位）',
  `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`        VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`     VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_unit` (`unit_id`),
  KEY `idx_target` (`target_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='多单位转换率';

-- ------------------------------------------------------------
-- 序列号规则
-- ------------------------------------------------------------
CREATE TABLE `serial_rule` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)  NOT NULL COMMENT '规则编码',
  `name`        VARCHAR(64)  NOT NULL COMMENT '规则名称',
  `prefix`      VARCHAR(32)  DEFAULT NULL COMMENT '序列号前缀',
  `start_no`    BIGINT       NOT NULL DEFAULT 1 COMMENT '起始序号',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='序列号规则';

-- ------------------------------------------------------------
-- 库区
-- ------------------------------------------------------------
CREATE TABLE `area` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`         VARCHAR(32)  NOT NULL COMMENT '库区编码',
  `name`         VARCHAR(64)  NOT NULL COMMENT '库区名称',
  `warehouse_id` BIGINT       NOT NULL COMMENT '所属仓库ID',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库区';

SET FOREIGN_KEY_CHECKS = 1;
