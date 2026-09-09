-- ============================================================
-- Token 制造 ERP V2.0 阶段4：其他出入库单
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 其他出入库单主表（direction 区分 in入库/out出库）
CREATE TABLE `other_stock` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_no`    VARCHAR(32)  NOT NULL COMMENT '单号（QTR其他入库/QTC其他出库+日期+流水）',
  `direction`   VARCHAR(8)   NOT NULL COMMENT '方向：in入库/out出库',
  `type`        VARCHAR(32)  DEFAULT NULL COMMENT '类型：期初/盘盈/盘亏/领用/其他',
  `warehouse_id` BIGINT      DEFAULT NULL COMMENT '仓库ID',
  `stock_date`  DATE         NOT NULL COMMENT '日期',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'completed' COMMENT '状态',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`stock_no`),
  KEY `idx_direction` (`direction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='其他出入库单';

-- 其他出入库单明细
CREATE TABLE `other_stock_item` (
  `id`        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_id`  BIGINT        NOT NULL COMMENT '单ID',
  `item_id`   BIGINT        NOT NULL COMMENT '物料/产品ID',
  `item_type` VARCHAR(16)   NOT NULL COMMENT '类型：material/product',
  `quantity`  DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '数量',
  `unit`      VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  `remark`    VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_stock` (`stock_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='其他出入库单明细';

SET FOREIGN_KEY_CHECKS = 1;
