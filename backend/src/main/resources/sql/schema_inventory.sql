-- ============================================================
-- Token 制造 ERP V1.0 库存/质量建表脚本
-- 依赖 schema.sql 先执行
-- 库存三类：可用 available / 待检 pending_qc / 不合格 unqualified
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 八、库存管理
-- ------------------------------------------------------------

-- 库存台账（当前状态）
CREATE TABLE `inventory` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`      BIGINT       NOT NULL COMMENT '物料/产品ID',
  `item_type`    VARCHAR(16)  NOT NULL COMMENT '类型：material物料/product产品',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `location_id`  BIGINT       DEFAULT NULL COMMENT '库位ID',
  `batch_no`     VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
  `serial_no`    VARCHAR(64)  DEFAULT NULL COMMENT '序列号',
  `stock_type`   VARCHAR(16)  NOT NULL DEFAULT 'available' COMMENT '库存类型：available可用/pending_qc待检/unqualified不合格',
  `quantity`     DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '实际库存数量',
  `reserved_qty` DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '预留库存数量',
  `unit_cost`    DECIMAL(18,6) DEFAULT NULL COMMENT '单位成本（V1预留，可空）',
  `total_cost`   DECIMAL(18,2) DEFAULT NULL COMMENT '总成本（V1预留，可空）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_dim` (`item_id`, `item_type`, `warehouse_id`, `location_id`, `batch_no`, `serial_no`, `stock_type`),
  KEY `idx_item` (`item_id`, `item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存台账';

-- 库存预留
CREATE TABLE `inventory_reservation` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inventory_id` BIGINT       NOT NULL COMMENT '库存台账ID',
  `biz_type`     VARCHAR(32)  NOT NULL COMMENT '来源单据类型：sales_order',
  `biz_id`       BIGINT       NOT NULL COMMENT '来源单据ID',
  `biz_no`       VARCHAR(64)  NOT NULL COMMENT '来源单据编号',
  `quantity`     DECIMAL(18,6) NOT NULL COMMENT '预留数量',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态：active有效/released已释放',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `release_time` DATETIME     DEFAULT NULL COMMENT '释放时间',
  PRIMARY KEY (`id`),
  KEY `idx_inv` (`inventory_id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存预留';

-- 库存流水（事实记录）
CREATE TABLE `inventory_transaction` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`      BIGINT       NOT NULL COMMENT '物料/产品ID',
  `item_type`    VARCHAR(16)  NOT NULL COMMENT '类型：material/product',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `location_id`  BIGINT       DEFAULT NULL COMMENT '库位ID',
  `batch_no`     VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
  `serial_no`    VARCHAR(64)  DEFAULT NULL COMMENT '序列号',
  `stock_type`   VARCHAR(16)  NOT NULL COMMENT '库存类型：available/pending_qc/unqualified',
  `direction`    VARCHAR(8)   NOT NULL COMMENT '方向：in入库/out出库',
  `quantity`     DECIMAL(18,6) NOT NULL COMMENT '变动数量（正数）',
  `balance_qty`  DECIMAL(18,6) NOT NULL COMMENT '变动后结存数量',
  `biz_type`     VARCHAR(32)  NOT NULL COMMENT '业务类型：purchase_inbound/production_inbound/sales_outbound/material_issue/return/transfer/stocktake/adjustment',
  `biz_id`       BIGINT       NOT NULL COMMENT '业务单据ID',
  `biz_no`       VARCHAR(64)  NOT NULL COMMENT '业务单据编号',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`, `item_type`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水';

-- 调拨单
CREATE TABLE `transfer` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `transfer_no`   VARCHAR(32)  NOT NULL COMMENT '调拨单号',
  `source_warehouse_id` BIGINT NOT NULL COMMENT '源仓库ID',
  `target_warehouse_id` BIGINT NOT NULL COMMENT '目标仓库ID',
  `transfer_date` DATE         NOT NULL COMMENT '调拨日期',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transfer_no` (`transfer_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调拨单';

-- 调拨明细
CREATE TABLE `transfer_item` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `transfer_id` BIGINT       NOT NULL COMMENT '调拨单ID',
  `item_id`     BIGINT       NOT NULL COMMENT '物料/产品ID',
  `item_type`   VARCHAR(16)  NOT NULL COMMENT '类型',
  `quantity`    DECIMAL(18,6) NOT NULL COMMENT '数量',
  `unit`        VARCHAR(16)  NOT NULL COMMENT '单位',
  PRIMARY KEY (`id`),
  KEY `idx_transfer` (`transfer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调拨明细';

-- 盘点单
CREATE TABLE `stocktake` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stocktake_no`  VARCHAR(32)  NOT NULL COMMENT '盘点单号',
  `warehouse_id`  BIGINT       NOT NULL COMMENT '仓库ID',
  `stocktake_date` DATE        NOT NULL COMMENT '盘点日期',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'created' COMMENT '状态：created已创建/counted已盘点/adjusted已调整',
  `approve_status` VARCHAR(16) DEFAULT 'pending' COMMENT '审批状态（差异调整需审批）',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stocktake_no` (`stocktake_no`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点单';

-- 盘点明细
CREATE TABLE `stocktake_item` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stocktake_id`  BIGINT       NOT NULL COMMENT '盘点单ID',
  `item_id`       BIGINT       NOT NULL COMMENT '物料/产品ID',
  `item_type`     VARCHAR(16)  NOT NULL COMMENT '类型',
  `location_id`   BIGINT       DEFAULT NULL COMMENT '库位ID',
  `batch_no`      VARCHAR(64)  DEFAULT NULL COMMENT '批次号',
  `book_qty`      DECIMAL(18,6) NOT NULL COMMENT '账面数量',
  `actual_qty`    DECIMAL(18,6) DEFAULT NULL COMMENT '实际数量',
  `diff_qty`      DECIMAL(18,6) DEFAULT NULL COMMENT '差异数量',
  PRIMARY KEY (`id`),
  KEY `idx_stocktake` (`stocktake_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点明细';

-- 库存调整单
CREATE TABLE `stock_adjustment` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `adjust_no`    VARCHAR(32)  NOT NULL COMMENT '调整单号',
  `item_id`      BIGINT       NOT NULL COMMENT '物料/产品ID',
  `item_type`    VARCHAR(16)  NOT NULL COMMENT '类型',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `location_id`  BIGINT       DEFAULT NULL COMMENT '库位ID',
  `adjust_qty`   DECIMAL(18,6) NOT NULL COMMENT '调整数量（正增负减）',
  `reason`       VARCHAR(255) NOT NULL COMMENT '调整原因',
  `source_type`  VARCHAR(32)  DEFAULT NULL COMMENT '来源类型：stocktake盘点',
  `source_id`    BIGINT       DEFAULT NULL COMMENT '来源单据ID',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adjust_no` (`adjust_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存调整单';

-- ------------------------------------------------------------
-- 九、质量管理
-- ------------------------------------------------------------

-- 质检单
CREATE TABLE `quality_inspection` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inspection_no` VARCHAR(32) NOT NULL COMMENT '质检单号（QC+日期+流水）',
  `source_type`  VARCHAR(32)  NOT NULL COMMENT '来源类型：purchase_inbound/production_inbound',
  `source_id`    BIGINT       NOT NULL COMMENT '来源单据ID',
  `source_no`    VARCHAR(64)  DEFAULT NULL COMMENT '来源单据编号',
  `item_id`      BIGINT       NOT NULL COMMENT '物料/产品ID',
  `item_type`    VARCHAR(16)  NOT NULL COMMENT '类型',
  `quantity`     DECIMAL(18,6) NOT NULL COMMENT '质检数量',
  `inspection_date` DATE      NOT NULL COMMENT '质检日期',
  `result`       VARCHAR(16)  DEFAULT NULL COMMENT '结果：qualified合格/unqualified不合格',
  `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspection_no` (`inspection_no`),
  KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检单';

-- 质检结果（不合格处理）
CREATE TABLE `quality_result` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inspection_id` BIGINT       NOT NULL COMMENT '质检单ID',
  `handle_type`   VARCHAR(16)  NOT NULL COMMENT '处理方式：return退货/scrap报废',
  `quantity`      DECIMAL(18,6) NOT NULL COMMENT '处理数量',
  `reason`        VARCHAR(500) DEFAULT NULL COMMENT '不合格原因',
  `result_status` VARCHAR(16)  NOT NULL COMMENT '处理后库存状态：available可用/unqualified不合格',
  `create_by`     VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_inspection` (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检结果';

SET FOREIGN_KEY_CHECKS = 1;
