-- ============================================================
-- 阶段6 财务：应收应付 + 收付款 + 核销对账
-- 编号前缀：YS应收 / YF应付 / SK收款 / FK付款
-- 金额精度：decimal(18,2)（继承 V1 冻结规则）
-- ============================================================
SET NAMES utf8mb4;

-- 1. 应收台账（销售发票审批通过后自动生成）
CREATE TABLE IF NOT EXISTS `receivable` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `receivable_no`      VARCHAR(32)   NOT NULL COMMENT '应收单号（YS+日期+流水）',
  `customer_id`        BIGINT        NOT NULL COMMENT '客户ID',
  `invoice_id`         BIGINT        NOT NULL COMMENT '销售发票ID',
  `invoice_no`         VARCHAR(32)   DEFAULT NULL COMMENT '发票号快照',
  `amount`             DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '应收金额',
  `written_off_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  `balance`            DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '未核销余额',
  `status`             VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending未结清/partial部分/settled已结清',
  `remark`             VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`            TINYINT       NOT NULL DEFAULT 0,
  `create_by`          VARCHAR(64)   DEFAULT NULL,
  `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`          VARCHAR(64)   DEFAULT NULL,
  `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receivable_no` (`receivable_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_invoice` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收台账';

-- 2. 应付台账（采购发票审批通过后自动生成）
CREATE TABLE IF NOT EXISTS `payable` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `payable_no`         VARCHAR(32)   NOT NULL COMMENT '应付单号（YF+日期+流水）',
  `supplier_id`        BIGINT        NOT NULL COMMENT '供应商ID',
  `invoice_id`         BIGINT        NOT NULL COMMENT '采购发票ID',
  `invoice_no`         VARCHAR(32)   DEFAULT NULL COMMENT '发票号快照',
  `amount`             DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '应付金额',
  `written_off_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  `balance`            DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '未核销余额',
  `status`             VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending未结清/partial部分/settled已结清',
  `remark`             VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`            TINYINT       NOT NULL DEFAULT 0,
  `create_by`          VARCHAR(64)   DEFAULT NULL,
  `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`          VARCHAR(64)   DEFAULT NULL,
  `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payable_no` (`payable_no`),
  KEY `idx_supplier` (`supplier_id`),
  KEY `idx_invoice` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应付台账';

-- 3. 收款单
CREATE TABLE IF NOT EXISTS `receipt` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `receipt_no`         VARCHAR(32)   NOT NULL COMMENT '收款单号（SK+日期+流水）',
  `customer_id`        BIGINT        NOT NULL COMMENT '客户ID',
  `amount`             DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '收款金额',
  `written_off_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  `balance`            DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '未核销余额',
  `receipt_date`       DATE          DEFAULT NULL COMMENT '收款日期',
  `account`            VARCHAR(64)   DEFAULT NULL COMMENT '收款账户',
  `status`             VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending未核销/partial部分/settled已核销完',
  `remark`             VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`            TINYINT       NOT NULL DEFAULT 0,
  `create_by`          VARCHAR(64)   DEFAULT NULL,
  `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`          VARCHAR(64)   DEFAULT NULL,
  `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipt_no` (`receipt_no`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款单';

-- 4. 付款单
CREATE TABLE IF NOT EXISTS `payment` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `payment_no`         VARCHAR(32)   NOT NULL COMMENT '付款单号（FK+日期+流水）',
  `supplier_id`        BIGINT        NOT NULL COMMENT '供应商ID',
  `amount`             DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '付款金额',
  `written_off_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已核销金额',
  `balance`            DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '未核销余额',
  `payment_date`       DATE          DEFAULT NULL COMMENT '付款日期',
  `account`            VARCHAR(64)   DEFAULT NULL COMMENT '付款账户',
  `status`             VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending未核销/partial部分/settled已核销完',
  `remark`             VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`            TINYINT       NOT NULL DEFAULT 0,
  `create_by`          VARCHAR(64)   DEFAULT NULL,
  `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`          VARCHAR(64)   DEFAULT NULL,
  `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款单';

-- 5. 收款核销明细（收款单 ↔ 应收台账）
CREATE TABLE IF NOT EXISTS `receipt_writeoff` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `receipt_id`    BIGINT        NOT NULL COMMENT '收款单ID',
  `receivable_id` BIGINT        NOT NULL COMMENT '应收台账ID',
  `amount`        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '核销金额',
  `writeoff_date` DATE          DEFAULT NULL COMMENT '核销日期',
  `remark`        VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_receipt` (`receipt_id`),
  KEY `idx_receivable` (`receivable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款核销明细';

-- 6. 付款核销明细（付款单 ↔ 应付台账）
CREATE TABLE IF NOT EXISTS `payment_writeoff` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `payment_id`   BIGINT        NOT NULL COMMENT '付款单ID',
  `payable_id`   BIGINT        NOT NULL COMMENT '应付台账ID',
  `amount`       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '核销金额',
  `writeoff_date` DATE         DEFAULT NULL COMMENT '核销日期',
  `remark`       VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment` (`payment_id`),
  KEY `idx_payable` (`payable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款核销明细';
