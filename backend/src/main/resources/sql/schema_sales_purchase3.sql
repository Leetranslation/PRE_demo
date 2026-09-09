-- ============================================================
-- Token 制造 ERP V2.0 阶段3 第3批：销售发票 + 采购发票（镜像）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 销售订单加「已开票金额」字段（用于发票回写）
ALTER TABLE `sales_order`
    ADD COLUMN `invoiced_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已开票金额' AFTER `total_amount`;

-- 采购订单加「已开票金额」字段
ALTER TABLE `purchase_order`
    ADD COLUMN `invoiced_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已开票金额' AFTER `total_amount`;

-- 销售发票主表
CREATE TABLE `sales_invoice` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `invoice_no`     VARCHAR(32)   NOT NULL COMMENT '发票号（FP+日期+流水）',
  `order_id`       BIGINT        NOT NULL COMMENT '销售订单ID',
  `customer_id`    BIGINT        NOT NULL COMMENT '客户ID',
  `invoice_date`   DATE          NOT NULL COMMENT '开票日期',
  `amount`         DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '开票金额',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending/approving/approved',
  `biz_status`     VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '业务状态：pending未核销/partial部分核销/verified已核销',
  `verified_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`invoice_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售发票';

-- 采购发票主表
CREATE TABLE `purchase_invoice` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `invoice_no`     VARCHAR(32)   NOT NULL COMMENT '发票号（FP+日期+流水）',
  `order_id`       BIGINT        NOT NULL COMMENT '采购订单ID',
  `supplier_id`    BIGINT        NOT NULL COMMENT '供应商ID',
  `invoice_date`   DATE          NOT NULL COMMENT '开票日期',
  `amount`         DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '发票金额',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending/approving/approved',
  `biz_status`     VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '业务状态：pending未核销/partial部分核销/verified已核销',
  `verified_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`invoice_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购发票';

SET FOREIGN_KEY_CHECKS = 1;
