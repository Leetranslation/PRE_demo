-- ============================================================
-- Token 制造 ERP V2 第一档断环：客户价格资料
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 产品加库存上下限（补齐产品库存预警）
ALTER TABLE `product`
  ADD COLUMN `min_stock` DECIMAL(18,6) DEFAULT NULL COMMENT '最小库存' AFTER `production_lead_days`,
  ADD COLUMN `safety_stock` DECIMAL(18,6) DEFAULT NULL COMMENT '安全库存' AFTER `min_stock`,
  ADD COLUMN `max_stock` DECIMAL(18,6) DEFAULT NULL COMMENT '最大库存' AFTER `safety_stock`;

CREATE TABLE `customer_price` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_id` BIGINT        NOT NULL COMMENT '客户ID',
  `item_id`     BIGINT        NOT NULL COMMENT '产品/物料ID',
  `item_type`   VARCHAR(16)   NOT NULL DEFAULT 'product' COMMENT '类型：product产品/material物料',
  `price`       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '销售单价',
  `status`      INT           NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_item` (`customer_id`, `item_id`, `item_type`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户价格资料';

SET FOREIGN_KEY_CHECKS = 1;
