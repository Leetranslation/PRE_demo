-- ============================================================
-- Token 制造 ERP V1.0 业务单据建表脚本（销售/采购/生产/MRP）
-- 依赖 schema.sql 先执行
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 四、销售管理
-- ------------------------------------------------------------

-- 销售订单
CREATE TABLE `sales_order` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`      VARCHAR(32)  NOT NULL COMMENT '销售单号（SO+日期+流水）',
  `customer_id`   BIGINT       NOT NULL COMMENT '客户ID',
  `order_date`    DATE         NOT NULL COMMENT '订单日期',
  `sales_type`    VARCHAR(32)  NOT NULL COMMENT '销售类型（字典）',
  `contact`       VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
  `delivery_date` DATE         DEFAULT NULL COMMENT '交货日期',
  `salesman`      VARCHAR(64)  DEFAULT NULL COMMENT '业务员',
  `department_id` BIGINT       DEFAULT NULL COMMENT '部门ID',
  `approve_status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '审批状态：pending待提交/approving审批中/approved已审批',
  `biz_status`    VARCHAR(16)  NOT NULL DEFAULT 'pending_outbound' COMMENT '业务状态：pending_outbound待出库/partial_outbound部分出库/completed已完成',
  `total_amount`  DECIMAL(18,2) DEFAULT 0 COMMENT '价税合计总额',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_status` (`approve_status`, `biz_status`),
  KEY `idx_date` (`order_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售订单';

-- 销售订单明细
CREATE TABLE `sales_order_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`     BIGINT       NOT NULL COMMENT '销售订单ID',
  `product_id`   BIGINT       NOT NULL COMMENT '产品ID',
  `spec`         VARCHAR(128) DEFAULT NULL COMMENT '规格',
  `unit`         VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '单位',
  `quantity`     DECIMAL(18,6) NOT NULL COMMENT '数量',
  `price`        DECIMAL(18,6) NOT NULL COMMENT '含税单价',
  `tax_rate`     DECIMAL(10,2) NOT NULL DEFAULT 13 COMMENT '税率(%)',
  `amount_ex_tax` DECIMAL(18,2) NOT NULL COMMENT '不含税金额',
  `tax_amount`   DECIMAL(18,2) NOT NULL COMMENT '税额',
  `total_amount` DECIMAL(18,2) NOT NULL COMMENT '价税合计',
  `delivery_date` DATE         DEFAULT NULL COMMENT '计划交货日期',
  `outbound_qty` DECIMAL(18,6) DEFAULT 0 COMMENT '已出库数量',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售订单明细';

-- 销售出库单
CREATE TABLE `sales_outbound` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `outbound_no`  VARCHAR(32)  NOT NULL COMMENT '出库单号（CK+日期+流水）',
  `order_id`     BIGINT       NOT NULL COMMENT '来源销售订单ID',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `outbound_date` DATE        NOT NULL COMMENT '出库日期',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbound_no` (`outbound_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售出库单';

-- 销售出库明细
CREATE TABLE `sales_outbound_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `outbound_id`  BIGINT       NOT NULL COMMENT '出库单ID',
  `order_item_id` BIGINT      DEFAULT NULL COMMENT '来源订单明细ID',
  `product_id`   BIGINT       NOT NULL COMMENT '产品ID',
  `location_id`  BIGINT       DEFAULT NULL COMMENT '库位ID',
  `batch_no`     VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
  `serial_no`    VARCHAR(64)  DEFAULT NULL COMMENT '序列号',
  `quantity`     DECIMAL(18,6) NOT NULL COMMENT '数量',
  `unit`         VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '单位',
  PRIMARY KEY (`id`),
  KEY `idx_outbound` (`outbound_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售出库明细';

-- ------------------------------------------------------------
-- 五、采购管理
-- ------------------------------------------------------------

-- 采购订单
CREATE TABLE `purchase_order` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`      VARCHAR(32)  NOT NULL COMMENT '采购单号（PO+日期+流水）',
  `supplier_id`   BIGINT       NOT NULL COMMENT '供应商ID',
  `order_date`    DATE         NOT NULL COMMENT '订单日期',
  `purchase_type` VARCHAR(32)  NOT NULL COMMENT '采购类型：stock备货/production生产/mrp采购',
  `expected_date` DATE         DEFAULT NULL COMMENT '预计到货日期',
  `contact`       VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
  `department_id` BIGINT       DEFAULT NULL COMMENT '部门ID',
  `buyer`         VARCHAR(64)  DEFAULT NULL COMMENT '采购员',
  `source_type`   VARCHAR(32)  DEFAULT NULL COMMENT '来源类型：manual/mrp',
  `source_id`     BIGINT       DEFAULT NULL COMMENT '来源MRP建议ID',
  `approve_status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '审批状态',
  `biz_status`    VARCHAR(16)  NOT NULL DEFAULT 'pending_inbound' COMMENT '业务状态：pending_inbound待入库/partial_inbound部分入库/completed已完成',
  `total_amount`  DECIMAL(18,2) DEFAULT 0 COMMENT '价税合计总额',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_supplier` (`supplier_id`),
  KEY `idx_status` (`approve_status`, `biz_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单';

-- 采购订单明细
CREATE TABLE `purchase_order_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`     BIGINT       NOT NULL COMMENT '采购订单ID',
  `material_id`  BIGINT       NOT NULL COMMENT '物料ID',
  `spec`         VARCHAR(128) DEFAULT NULL COMMENT '规格',
  `unit`         VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '单位',
  `quantity`     DECIMAL(18,6) NOT NULL COMMENT '数量',
  `price`        DECIMAL(18,6) NOT NULL COMMENT '含税单价',
  `tax_rate`     DECIMAL(10,2) NOT NULL DEFAULT 13 COMMENT '税率(%)',
  `amount_ex_tax` DECIMAL(18,2) NOT NULL COMMENT '不含税金额',
  `tax_amount`   DECIMAL(18,2) NOT NULL COMMENT '税额',
  `total_amount` DECIMAL(18,2) NOT NULL COMMENT '价税合计',
  `expected_date` DATE         DEFAULT NULL COMMENT '预计到货日期',
  `inbound_qty`  DECIMAL(18,6) DEFAULT 0 COMMENT '已入库数量',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细';

-- 采购入库单
CREATE TABLE `purchase_inbound` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inbound_no`   VARCHAR(32)  NOT NULL COMMENT '入库单号（RK+日期+流水）',
  `order_id`     BIGINT       NOT NULL COMMENT '来源采购订单ID',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `inbound_date` DATE         NOT NULL COMMENT '入库日期',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inbound_no` (`inbound_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库单';

-- 采购入库明细
CREATE TABLE `purchase_inbound_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inbound_id`   BIGINT       NOT NULL COMMENT '入库单ID',
  `order_item_id` BIGINT      DEFAULT NULL COMMENT '来源订单明细ID',
  `material_id`  BIGINT       NOT NULL COMMENT '物料ID',
  `location_id`  BIGINT       DEFAULT NULL COMMENT '库位ID',
  `batch_no`     VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
  `serial_no`    VARCHAR(64)  DEFAULT NULL COMMENT '序列号',
  `quantity`     DECIMAL(18,6) NOT NULL COMMENT '数量',
  `unit`         VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '单位',
  PRIMARY KEY (`id`),
  KEY `idx_inbound` (`inbound_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库明细';

-- ------------------------------------------------------------
-- 六、生产管理
-- ------------------------------------------------------------

-- 生产工单
CREATE TABLE `production_order` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`      VARCHAR(32)  NOT NULL COMMENT '工单号（MO+日期+流水）',
  `product_id`    BIGINT       NOT NULL COMMENT '产品ID',
  `quantity`      DECIMAL(18,6) NOT NULL COMMENT '生产数量',
  `production_type` VARCHAR(32) NOT NULL COMMENT '生产类型：order订单/stock库存/mrp生产',
  `plan_date`     DATE         NOT NULL COMMENT '计划生产日期',
  `source_type`   VARCHAR(32)  DEFAULT NULL COMMENT '来源类型：manual/sales_order/mrp',
  `source_id`     BIGINT       DEFAULT NULL COMMENT '来源单据ID',
  `plan_finish_date` DATE      DEFAULT NULL COMMENT '计划完成日期',
  `department_id` BIGINT       DEFAULT NULL COMMENT '生产部门ID',
  `owner`         VARCHAR(64)  DEFAULT NULL COMMENT '负责人',
  `bom_id`        BIGINT       DEFAULT NULL COMMENT '使用的BOM ID',
  `bom_version_id` BIGINT      DEFAULT NULL COMMENT '使用的BOM版本ID',
  `approve_status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '审批状态',
  `biz_status`    VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '业务状态：pending待生产/producing生产中/partial部分完成/completed已完成',
  `completed_qty` DECIMAL(18,6) DEFAULT 0 COMMENT '已完成数量',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_product` (`product_id`),
  KEY `idx_status` (`approve_status`, `biz_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产工单';

-- 生产领料单
CREATE TABLE `material_issue` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `issue_no`     VARCHAR(32)  NOT NULL COMMENT '领料单号',
  `order_id`     BIGINT       NOT NULL COMMENT '生产工单ID',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `issue_date`   DATE         NOT NULL COMMENT '领料日期',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_issue_no` (`issue_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产领料单';

-- 生产领料明细
CREATE TABLE `material_issue_item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `issue_id`     BIGINT       NOT NULL COMMENT '领料单ID',
  `material_id`  BIGINT       NOT NULL COMMENT '物料ID',
  `location_id`  BIGINT       DEFAULT NULL COMMENT '库位ID',
  `batch_no`     VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
  `serial_no`    VARCHAR(64)  DEFAULT NULL COMMENT '序列号',
  `quantity`     DECIMAL(18,6) NOT NULL COMMENT '数量',
  `unit`         VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '单位',
  PRIMARY KEY (`id`),
  KEY `idx_issue` (`issue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产领料明细';

-- 生产退料单
CREATE TABLE `material_return` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `return_no`    VARCHAR(32)  NOT NULL COMMENT '退料单号',
  `order_id`     BIGINT       NOT NULL COMMENT '生产工单ID',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `return_date`  DATE         NOT NULL COMMENT '退料日期',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_return_no` (`return_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产退料单';

-- 生产退料明细
CREATE TABLE `material_return_item` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `return_id`   BIGINT       NOT NULL COMMENT '退料单ID',
  `material_id` BIGINT       NOT NULL COMMENT '物料ID',
  `location_id` BIGINT       DEFAULT NULL COMMENT '库位ID',
  `quantity`    DECIMAL(18,6) NOT NULL COMMENT '数量',
  `unit`        VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '单位',
  PRIMARY KEY (`id`),
  KEY `idx_return` (`return_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产退料明细';

-- 生产入库单
CREATE TABLE `production_inbound` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inbound_no`   VARCHAR(32)  NOT NULL COMMENT '入库单号（RK+日期+流水）',
  `order_id`     BIGINT       NOT NULL COMMENT '生产工单ID',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `inbound_date` DATE         NOT NULL COMMENT '入库日期',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inbound_no` (`inbound_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产入库单';

-- 生产入库明细
CREATE TABLE `production_inbound_item` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inbound_id`  BIGINT       NOT NULL COMMENT '入库单ID',
  `product_id`  BIGINT       NOT NULL COMMENT '产品ID',
  `location_id` BIGINT       DEFAULT NULL COMMENT '库位ID',
  `batch_no`    VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
  `serial_no`   VARCHAR(64)  DEFAULT NULL COMMENT '序列号',
  `quantity`    DECIMAL(18,6) NOT NULL COMMENT '数量',
  `unit`        VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '单位',
  PRIMARY KEY (`id`),
  KEY `idx_inbound` (`inbound_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产入库明细';

-- ------------------------------------------------------------
-- 七、MRP 计划
-- ------------------------------------------------------------

-- MRP 计划
CREATE TABLE `mrp_plan` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`      VARCHAR(32)  NOT NULL COMMENT 'MRP计划号（MRP+日期+流水）',
  `plan_date`    DATE         NOT NULL COMMENT '计划日期',
  `start_date`   DATE         DEFAULT NULL COMMENT '需求开始日期',
  `end_date`     DATE         DEFAULT NULL COMMENT '需求结束日期',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'calculated' COMMENT '状态：calculated已计算/confirmed已确认',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_no` (`plan_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MRP计划';

-- MRP 需求
CREATE TABLE `mrp_requirement` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id`       BIGINT       NOT NULL COMMENT 'MRP计划ID',
  `item_id`       BIGINT       NOT NULL COMMENT '物料/产品ID',
  `item_type`     VARCHAR(16)  NOT NULL COMMENT '类型：material物料/product产品',
  `gross_qty`     DECIMAL(18,6) NOT NULL COMMENT '毛需求',
  `available_qty` DECIMAL(18,6) NOT NULL COMMENT '可用库存',
  `in_transit_qty` DECIMAL(18,6) DEFAULT 0 COMMENT '在途供应',
  `net_qty`       DECIMAL(18,6) NOT NULL COMMENT '净需求',
  `supply_type`   VARCHAR(16)  NOT NULL COMMENT '供应方式：purchase采购/make自制',
  `demand_date`   DATE         DEFAULT NULL COMMENT '需求日期',
  `suggest_date`  DATE         DEFAULT NULL COMMENT '建议日期（采购/开工）',
  `source_type`   VARCHAR(32)  DEFAULT NULL COMMENT '需求来源类型',
  `source_id`     BIGINT       DEFAULT NULL COMMENT '需求来源ID',
  `source_no`     VARCHAR(64)  DEFAULT NULL COMMENT '需求来源编号',
  `converted_qty` DECIMAL(18,6) DEFAULT 0 COMMENT '已转采购/生产数量',
  `converted_status` VARCHAR(16) DEFAULT 'none' COMMENT '转换状态：none未转/purchase已转采购/make已转生产',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MRP需求';

SET FOREIGN_KEY_CHECKS = 1;
