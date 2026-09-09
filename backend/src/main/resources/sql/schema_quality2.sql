-- ============================================================
-- Token 制造 ERP V2.0 阶段4：质检申请单（三段式第一段）+ 四线区分
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 质检申请单（三段式：申请单 → 检验单 → 处理）
CREATE TABLE `qc_apply` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_no`     VARCHAR(32)   NOT NULL COMMENT '申请单号（QS+日期+流水）',
  `source_type`  VARCHAR(32)   NOT NULL COMMENT '质检来源（四线）：purchase_qc来料/production_qc生产/process_qc工序/outbound_qc出库',
  `source_id`    BIGINT        DEFAULT NULL COMMENT '来源单据ID',
  `source_no`    VARCHAR(32)   DEFAULT NULL COMMENT '来源单据号',
  `item_id`      BIGINT        NOT NULL COMMENT '物料/产品ID',
  `item_type`    VARCHAR(16)   NOT NULL COMMENT '类型：material/product',
  `warehouse_id` BIGINT        DEFAULT NULL COMMENT '仓库ID',
  `quantity`     DECIMAL(18,6) NOT NULL DEFAULT 0 COMMENT '申请质检数量',
  `apply_date`   DATE          NOT NULL COMMENT '申请日期',
  `status`       VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '状态：pending待检/inspecting检验中/finished已检',
  `inspection_id` BIGINT       DEFAULT NULL COMMENT '关联检验单ID',
  `remark`       VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`    VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`apply_no`),
  KEY `idx_source` (`source_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检申请单';

SET FOREIGN_KEY_CHECKS = 1;
