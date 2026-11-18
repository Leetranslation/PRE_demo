-- ============================================================
-- AI 交期风险智能预警（交付周期 + 风险评估）
-- 说明：全部为新增，不改现有字段语义，可在运行库直接执行
-- ============================================================

-- 1) 人工录入的周期（文档要求：优先用人工录入值，没有再按历史单据计算）
ALTER TABLE product
  ADD COLUMN purchase_cycle_days  DECIMAL(10,2) NULL COMMENT '人工录入的平均采购周期(整批天)',
  ADD COLUMN outsource_cycle_days DECIMAL(10,2) NULL COMMENT '人工录入的平均外协周期(整批天)';

ALTER TABLE material
  ADD COLUMN production_cycle_days DECIMAL(10,2) NULL COMMENT '人工录入的平均生产周期(天/件)',
  ADD COLUMN outsource_cycle_days  DECIMAL(10,2) NULL COMMENT '人工录入的平均外协周期(整批天)';

-- 2) 报价单/合同明细补行级交付日期（销售订单明细 sales_order_item 已有）
ALTER TABLE quotation_item ADD COLUMN delivery_date DATE NULL COMMENT '行级交付日期';
ALTER TABLE contract_item  ADD COLUMN delivery_date DATE NULL COMMENT '行级交付日期';

-- 3) 历史周期均值（预计算，避免每次评估都遍历历史单据）
CREATE TABLE IF NOT EXISTS item_cycle_stat (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_type    VARCHAR(16)   NOT NULL COMMENT 'product / material',
  item_id      BIGINT        NOT NULL,
  cycle_type   VARCHAR(16)   NOT NULL COMMENT 'production / purchase / outsource',
  avg_days     DECIMAL(12,4) NOT NULL COMMENT '平均周期；生产为「天/件」，采购/外协为「整批天」',
  sample_count INT           NOT NULL DEFAULT 0 COMMENT '参与计算的单据数',
  window_days  INT           NOT NULL DEFAULT 365 COMMENT '取数窗口(天)',
  calc_time    DATETIME      NOT NULL,
  UNIQUE KEY uk_item_cycle (item_type, item_id, cycle_type)
) COMMENT '物料/产品历史周期均值';

-- 4) 评估结果快照（列表筛选、追溯都用它；打开评估时实时重算覆盖）
CREATE TABLE IF NOT EXISTS delivery_risk (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_type             VARCHAR(16)   NOT NULL COMMENT 'sales_order / quotation / contract',
  biz_id               BIGINT        NOT NULL,
  item_id              BIGINT        NOT NULL COMMENT '销售/报价/合同明细行 id',
  item_type            VARCHAR(16)   NOT NULL DEFAULT 'product',
  qty                  DECIMAL(18,4) NULL COMMENT '基本数量',
  delivery_date        DATE          NULL COMMENT '行级交付日期',
  risk_level           VARCHAR(16)   NOT NULL COMMENT 'on_time 可准时 / at_risk 有逾期风险 / late 已逾期',
  earliest_action_date DATE          NULL COMMENT '最早行动日期',
  suggest_date         DATE          NULL COMMENT 'AI 建议交付日期',
  stock_qty            DECIMAL(18,4) DEFAULT 0 COMMENT '可用库存(quantity - reserved_qty)',
  shortage_qty         DECIMAL(18,4) DEFAULT 0 COMMENT '净缺料数量',
  cycle_days           DECIMAL(12,4) DEFAULT 0 COMMENT '本次用于倒推的周期天数',
  detail_json          LONGTEXT      NULL COMMENT '完整计算过程(前端「查看分析」用)',
  calc_time            DATETIME      NOT NULL,
  UNIQUE KEY uk_risk_row (biz_type, biz_id, item_id),
  KEY idx_risk_level (risk_level)
) COMMENT '交期风险评估结果';
