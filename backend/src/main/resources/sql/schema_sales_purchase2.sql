-- ============================================================
-- Token 制造 ERP V2.0 阶段3 第2批：合同 + 询价单
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 合同主表
CREATE TABLE `contract` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_no`    VARCHAR(32)   NOT NULL COMMENT '合同号（HT+日期+流水）',
  `customer_id`    BIGINT        NOT NULL COMMENT '客户ID',
  `contract_date`  DATE          NOT NULL COMMENT '签订日期',
  `amount`         DECIMAL(18,2) DEFAULT 0 COMMENT '合同金额',
  `salesman`       VARCHAR(64)   DEFAULT NULL COMMENT '业务员',
  `payment_terms`  VARCHAR(255)  DEFAULT NULL COMMENT '付款方式/条款',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending/approving/approved',
  `biz_status`     VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '业务状态：pending未转单/converted已转单',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`contract_no`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同';

-- 合同明细
CREATE TABLE `contract_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_id` BIGINT        NOT NULL COMMENT '合同ID',
  `product_id`  BIGINT        NOT NULL COMMENT '产品ID',
  `spec`        VARCHAR(128)  DEFAULT NULL COMMENT '规格',
  `unit`        VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `quantity`    DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '数量',
  `price`       DECIMAL(18,6) DEFAULT 0 COMMENT '含税单价',
  `tax_rate`    DECIMAL(6,2)  DEFAULT 13 COMMENT '税率',
  `total_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '价税合计',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_contract` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同明细';

-- 询价单主表
CREATE TABLE `purchase_inquiry` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inquiry_no`     VARCHAR(32)   NOT NULL COMMENT '询价单号（XJ+日期+流水）',
  `inquiry_date`   DATE          NOT NULL COMMENT '询价日期',
  `inquirer`       VARCHAR(64)   DEFAULT NULL COMMENT '询价人',
  `approve_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending/approving/approved',
  `biz_status`     VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '业务状态：pending未比价/priced已比价',
  `remark`         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`inquiry_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购询价单';

-- 询价单明细（每个物料可报多个供应商价格）
CREATE TABLE `purchase_inquiry_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inquiry_id`  BIGINT        NOT NULL COMMENT '询价单ID',
  `material_id` BIGINT        NOT NULL COMMENT '物料ID',
  `spec`        VARCHAR(128)  DEFAULT NULL COMMENT '规格',
  `unit`        VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `quantity`    DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '数量',
  `supplier_id` BIGINT        DEFAULT NULL COMMENT '供应商ID（报价供应商）',
  `price`       DECIMAL(18,6) DEFAULT NULL COMMENT '供应商报价单价',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_inquiry` (`inquiry_id`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购询价单明细';

SET FOREIGN_KEY_CHECKS = 1;
