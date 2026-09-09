-- ============================================================
-- Token 制造 ERP V2 第一档断环：质量四线补全（检验方法、检验方案）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 检验方法
CREATE TABLE `inspection_method` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)   NOT NULL COMMENT '编码（JF+日期+流水）',
  `name`        VARCHAR(128)  NOT NULL COMMENT '检验方法名称',
  `description` VARCHAR(255)  DEFAULT NULL COMMENT '方法描述',
  `status`      INT           NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检验方法';

-- 检验方案
CREATE TABLE `inspection_scheme` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`                 VARCHAR(32)   NOT NULL COMMENT '编码（FA+日期+流水）',
  `name`                 VARCHAR(128)  NOT NULL COMMENT '检验方案名称',
  `item_type`            VARCHAR(16)   NOT NULL DEFAULT 'material' COMMENT '适用类型：material物料/product产品',
  `inspection_method_id` BIGINT        DEFAULT NULL COMMENT '检验方法ID',
  `standard`             VARCHAR(500)  DEFAULT NULL COMMENT '检验标准',
  `status`               INT           NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
  `remark`               VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`              TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`            VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`            VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检验方案';

SET FOREIGN_KEY_CHECKS = 1;
