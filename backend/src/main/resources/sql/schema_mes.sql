-- ============================================================
-- Token 制造 ERP V2.0 MES 基础数据建表脚本（工序/工艺路线/车间/机台/工作日历等）
-- 依赖 schema.sql 先执行
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 十、MES 基础数据
-- ------------------------------------------------------------

-- 工序
CREATE TABLE `process` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`           VARCHAR(32)   NOT NULL COMMENT '工序编码（GX+日期+流水）',
  `name`           VARCHAR(128)  NOT NULL COMMENT '工序名称',
  `category`       VARCHAR(32)   DEFAULT NULL COMMENT '工序类别（自由字符串：加工/检验/外协等）',
  `standard_price` DECIMAL(18,6) DEFAULT NULL COMMENT '标准单价（工价）',
  `price_type`     VARCHAR(16)   NOT NULL DEFAULT 'piece' COMMENT '计价方式：piece计件/time计时/fixed固定价',
  `description`    VARCHAR(500)  DEFAULT NULL COMMENT '工序说明',
  `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工序';

-- 工艺路线主表
CREATE TABLE `process_route` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)  NOT NULL COMMENT '工艺路线编号（GY+日期+流水）',
  `name`        VARCHAR(128) NOT NULL COMMENT '工艺路线名称',
  `product_id`  BIGINT       NOT NULL COMMENT '产品ID',
  `version`     VARCHAR(32)  DEFAULT NULL COMMENT '版本号',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺路线主表';

-- 工艺路线明细（产品→工序序列）
CREATE TABLE `process_route_item` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_id`       BIGINT        NOT NULL COMMENT '工艺路线ID',
  `process_id`     BIGINT        NOT NULL COMMENT '工序ID',
  `seq_no`         INT           NOT NULL DEFAULT 1 COMMENT '工序顺序号',
  `standard_hours` DECIMAL(18,6) DEFAULT NULL COMMENT '标准工时（小时）',
  `is_key`         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否关键工序：1是 0否',
  `is_qc`          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否检验工序：1是 0否',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_route` (`route_id`),
  KEY `idx_process` (`process_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺路线明细';

SET FOREIGN_KEY_CHECKS = 1;
