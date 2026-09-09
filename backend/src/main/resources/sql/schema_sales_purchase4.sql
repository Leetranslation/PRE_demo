-- ============================================================
-- Token 制造 ERP V2.0 阶段3 第4~6批：核销 + 退货 + 售后/备货/设计中心/装箱单
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 销售核销
CREATE TABLE `sales_verification` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `verification_no` VARCHAR(32)  NOT NULL COMMENT '核销单号（HX+日期+流水）',
  `invoice_id`     BIGINT        NOT NULL COMMENT '销售发票ID',
  `order_id`       BIGINT        NOT NULL COMMENT '销售订单ID',
  `amount`         DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '核销金额（收款）',
  `verify_date`    DATE          NOT NULL COMMENT '核销日期',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)   DEFAULT NULL,
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)   DEFAULT NULL,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`verification_no`),
  KEY `idx_invoice` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售核销';

-- 采购核销
CREATE TABLE `purchase_verification` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `verification_no` VARCHAR(32)  NOT NULL COMMENT '核销单号（HX+日期+流水）',
  `invoice_id`     BIGINT        NOT NULL COMMENT '采购发票ID',
  `order_id`       BIGINT        NOT NULL COMMENT '采购订单ID',
  `amount`         DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '核销金额（付款）',
  `verify_date`    DATE          NOT NULL COMMENT '核销日期',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)   DEFAULT NULL,
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)   DEFAULT NULL,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`verification_no`),
  KEY `idx_invoice` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购核销';

-- 销售退货主表
CREATE TABLE `sales_return` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `return_no`      VARCHAR(32)   NOT NULL COMMENT '退货单号（TH+日期+流水）',
  `order_id`       BIGINT        NOT NULL COMMENT '销售订单ID',
  `customer_id`    BIGINT        NOT NULL COMMENT '客户ID',
  `warehouse_id`   BIGINT        DEFAULT NULL COMMENT '入库仓库ID',
  `return_date`    DATE          NOT NULL COMMENT '退货日期',
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)   DEFAULT NULL,
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)   DEFAULT NULL,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`return_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售退货';

-- 销售退货明细
CREATE TABLE `sales_return_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `return_id`   BIGINT        NOT NULL COMMENT '退货单ID',
  `product_id`  BIGINT        NOT NULL COMMENT '产品ID',
  `quantity`    DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '退货数量',
  `unit`        VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_return` (`return_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售退货明细';

-- 采购退货主表
CREATE TABLE `purchase_return` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `return_no`      VARCHAR(32)   NOT NULL COMMENT '退货单号（TH+日期+流水）',
  `order_id`       BIGINT        NOT NULL COMMENT '采购订单ID',
  `supplier_id`    BIGINT        NOT NULL COMMENT '供应商ID',
  `warehouse_id`   BIGINT        DEFAULT NULL COMMENT '出库仓库ID',
  `return_date`    DATE          NOT NULL COMMENT '退货日期',
  `status`         VARCHAR(16)   NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)   DEFAULT NULL,
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)   DEFAULT NULL,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`return_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货';

-- 采购退货明细
CREATE TABLE `purchase_return_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `return_id`   BIGINT        NOT NULL COMMENT '退货单ID',
  `material_id` BIGINT        NOT NULL COMMENT '物料ID',
  `quantity`    DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '退货数量',
  `unit`        VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_return` (`return_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货明细';

-- 售后管理
CREATE TABLE `after_sale` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `after_sale_no`  VARCHAR(32)  NOT NULL COMMENT '售后单号（SH+日期+流水）',
  `order_id`       BIGINT       NOT NULL COMMENT '销售订单ID',
  `customer_id`    BIGINT       NOT NULL COMMENT '客户ID',
  `type`           VARCHAR(32)  DEFAULT NULL COMMENT '售后类型：退货/维修/换货',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '状态：pending待处理/processing处理中/finished已完结',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)  DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)  DEFAULT NULL,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`after_sale_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后管理';

-- 销售备货单
CREATE TABLE `sales_stocking` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stocking_no`    VARCHAR(32)  NOT NULL COMMENT '备货单号（BH+日期+流水）',
  `order_id`       BIGINT       DEFAULT NULL COMMENT '销售订单ID（可选）',
  `approve_status` VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '审批状态',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)  DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)  DEFAULT NULL,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`stocking_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售备货单';

-- 销售备货单明细
CREATE TABLE `sales_stocking_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stocking_id` BIGINT        NOT NULL COMMENT '备货单ID',
  `product_id`  BIGINT        NOT NULL COMMENT '产品ID',
  `quantity`    DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '数量',
  `unit`        VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_stocking` (`stocking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售备货单明细';

-- 设计中心
CREATE TABLE `design_center` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `design_no`      VARCHAR(32)  NOT NULL COMMENT '设计单号（SJ+日期+流水）',
  `order_id`       BIGINT       DEFAULT NULL COMMENT '销售订单ID（可选）',
  `product_id`     BIGINT       NOT NULL COMMENT '设计的产品ID',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'designing' COMMENT '状态：designing设计中/finished已完成',
  `approve_status` VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '审批状态',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)  DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)  DEFAULT NULL,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`design_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计中心';

-- 装箱单
CREATE TABLE `packing` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `packing_no`     VARCHAR(32)  NOT NULL COMMENT '装箱单号（ZX+日期+流水）',
  `outbound_id`    BIGINT       DEFAULT NULL COMMENT '销售出库单ID',
  `approve_status` VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '审批状态',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  `create_by`      VARCHAR(64)  DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      VARCHAR(64)  DEFAULT NULL,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`packing_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='装箱单';

-- 装箱单明细
CREATE TABLE `packing_item` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `packing_id` BIGINT        NOT NULL COMMENT '装箱单ID',
  `box_no`     VARCHAR(64)   DEFAULT NULL COMMENT '箱号',
  `product_id` BIGINT        NOT NULL COMMENT '产品ID',
  `quantity`   DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '数量',
  `remark`     VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_packing` (`packing_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='装箱单明细';

SET FOREIGN_KEY_CHECKS = 1;
