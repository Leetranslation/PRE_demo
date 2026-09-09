-- ============================================================
-- Token 制造 ERP V2.0 阶段3：销售/采购补全单据（第1批：报价单 + 采购申请单）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 报价单主表
CREATE TABLE `quotation` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `quotation_no`   VARCHAR(32)   NOT NULL COMMENT '报价单号（BJ+日期+流水）',
  `customer_id`    BIGINT        NOT NULL COMMENT '客户ID',
  `quote_date`     DATE          NOT NULL COMMENT '报价日期',
  `valid_until`    DATE          DEFAULT NULL COMMENT '有效期至',
  `salesman`       VARCHAR(64)   DEFAULT NULL COMMENT '业务员',
  `total_amount`   DECIMAL(18,2) DEFAULT 0 COMMENT '价税合计',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending/approving/approved',
  `biz_status`     VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '业务状态：pending未转单/converted已转单',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`quotation_no`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报价单';

-- 报价单明细
CREATE TABLE `quotation_item` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `quotation_id` BIGINT        NOT NULL COMMENT '报价单ID',
  `product_id`   BIGINT        NOT NULL COMMENT '产品ID',
  `spec`         VARCHAR(128)  DEFAULT NULL COMMENT '规格',
  `unit`         VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `quantity`     DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '数量',
  `price`        DECIMAL(18,6) DEFAULT 0 COMMENT '含税单价',
  `tax_rate`     DECIMAL(6,2)  DEFAULT 13 COMMENT '税率',
  `amount_ex_tax` DECIMAL(18,2) DEFAULT 0 COMMENT '不含税金额',
  `tax_amount`   DECIMAL(18,2) DEFAULT 0 COMMENT '税额',
  `total_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '价税合计',
  `remark`       VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_quotation` (`quotation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报价单明细';

-- 采购申请单主表
CREATE TABLE `purchase_requisition` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `requisition_no` VARCHAR(32)   NOT NULL COMMENT '申请单号（SQ+日期+流水）',
  `department_id`  BIGINT        DEFAULT NULL COMMENT '申请部门ID',
  `requisition_date` DATE        NOT NULL COMMENT '申请日期',
  `applicant`      VARCHAR(64)   DEFAULT NULL COMMENT '申请人',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending/approving/approved',
  `biz_status`     VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '业务状态：pending未采购/purchased已转采购',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`requisition_no`),
  KEY `idx_dept` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购申请单';

-- 采购申请单明细
CREATE TABLE `purchase_requisition_item` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `requisition_id` BIGINT        NOT NULL COMMENT '申请单ID',
  `material_id`    BIGINT        NOT NULL COMMENT '物料ID',
  `spec`           VARCHAR(128)  DEFAULT NULL COMMENT '规格',
  `unit`           VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `quantity`       DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '需求数量',
  `required_date`  DATE          DEFAULT NULL COMMENT '需求日期',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_requisition` (`requisition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购申请单明细';

SET FOREIGN_KEY_CHECKS = 1;
