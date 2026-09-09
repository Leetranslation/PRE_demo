-- ============================================================
-- Token 制造 ERP V2 第一档断环：库存域（组装/拆卸单、期初余额）
-- 盘点/调拨/调整表已在 schema_inventory.sql 建好，此处不重复
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 组装/拆卸单主表（type 区分：assembly组装 / disassembly拆卸）
CREATE TABLE `assembly_order` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`    VARCHAR(32)   NOT NULL COMMENT '单号（ZZ组装/CX拆卸+日期+流水）',
  `type`        VARCHAR(16)   NOT NULL COMMENT '类型：assembly组装/disassembly拆卸',
  `product_id`  BIGINT        NOT NULL COMMENT '产品ID（成品）',
  `bom_id`      BIGINT        DEFAULT NULL COMMENT 'BOM ID（组装/拆卸按此展开子件）',
  `quantity`    DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '成品数量',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `biz_date`    DATE          NOT NULL COMMENT '业务日期',
  `status`      VARCHAR(16)   NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`order_no`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组装/拆卸单';

-- 组装/拆卸单明细（子件物料）
CREATE TABLE `assembly_order_item` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`   BIGINT        NOT NULL COMMENT '单ID',
  `material_id` BIGINT       NOT NULL COMMENT '子件物料ID',
  `quantity`   DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '子件数量（成品数量 × BOM用量）',
  `unit`       VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `remark`     VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组装/拆卸单明细';

-- 期初余额（客户应收/供应商应付期初，库存期初走 other_stock type=期初）
CREATE TABLE `begin_balance` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `target_type` VARCHAR(16)   NOT NULL COMMENT '对象类型：customer客户/supplier供应商',
  `target_id`   BIGINT        NOT NULL COMMENT '客户/供应商ID',
  `target_name` VARCHAR(128)  DEFAULT NULL COMMENT '对象名称（冗余）',
  `amount`      DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '期初应收/应付金额',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='期初余额';

SET FOREIGN_KEY_CHECKS = 1;
