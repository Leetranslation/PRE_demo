-- ============================================================
-- Token 制造 ERP V2 第一档断环：物料替代料关系
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `material_substitute` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `material_id`           BIGINT        NOT NULL COMMENT '原物料ID',
  `substitute_material_id` BIGINT       NOT NULL COMMENT '替代物料ID',
  `ratio`                 DECIMAL(18,6) NOT NULL DEFAULT 1 COMMENT '替代比例（1单位原物料=ratio单位替代物料）',
  `priority`              INT           NOT NULL DEFAULT 0 COMMENT '优先级（越小越优先）',
  `status`                INT           NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
  `remark`                VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `deleted`               TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`             VARCHAR(64)   DEFAULT NULL COMMENT '创建人',
  `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`             VARCHAR(64)   DEFAULT NULL COMMENT '修改人',
  `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_material` (`material_id`),
  KEY `idx_substitute` (`substitute_material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料替代料关系';

SET FOREIGN_KEY_CHECKS = 1;
