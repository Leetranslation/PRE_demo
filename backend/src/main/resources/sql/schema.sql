-- ============================================================
-- Token 制造 ERP V1.0 数据库建表脚本
-- 数据库：token_erp  MySQL 8.0 / InnoDB / utf8mb4
-- 编号规则：业务前缀+YYYYMMDD+4位流水号，按日连续自增，删除留空号
-- 精度：数量6位、单价6位、金额2位、税率2位
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 一、系统管理模块
-- ------------------------------------------------------------

-- 部门
CREATE TABLE `department` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)  NOT NULL COMMENT '部门编码',
  `name`        VARCHAR(64)  NOT NULL COMMENT '部门名称',
  `parent_id`   BIGINT       DEFAULT 0 COMMENT '父部门ID，0为顶级',
  `sort`        INT          DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门';

-- 用户
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(64)  NOT NULL COMMENT '登录用户名',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt哈希）',
  `real_name`   VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `email`       VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
  `department_id` BIGINT     DEFAULT NULL COMMENT '部门ID',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

-- 角色
CREATE TABLE `role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码',
  `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
  `data_scope`  VARCHAR(16)  NOT NULL DEFAULT 'self' COMMENT '数据权限：all全部/dept本部门/self本人',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

-- 权限
CREATE TABLE `permission` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(64)  NOT NULL COMMENT '权限编码',
  `name`        VARCHAR(64)  NOT NULL COMMENT '权限名称',
  `type`        VARCHAR(16)  NOT NULL COMMENT '类型：menu菜单/button按钮',
  `parent_id`   BIGINT       DEFAULT 0 COMMENT '父级ID',
  `path`        VARCHAR(128) DEFAULT NULL COMMENT '路由路径',
  `sort`        INT          DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限';

-- 用户-角色关联
CREATE TABLE `user_role` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联';

-- 角色-权限关联
CREATE TABLE `role_permission` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`       BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  KEY `idx_role` (`role_id`),
  KEY `idx_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联';

-- 系统参数
CREATE TABLE `sys_config` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`  VARCHAR(64)  NOT NULL COMMENT '参数键',
  `config_value` VARCHAR(255) NOT NULL COMMENT '参数值',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数';

-- 审批记录
CREATE TABLE `approval_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type`      VARCHAR(32)  NOT NULL COMMENT '单据类型：sales_order/purchase_order/production_order等',
  `biz_no`        VARCHAR(64)  NOT NULL COMMENT '业务单据编号',
  `biz_id`        BIGINT       NOT NULL COMMENT '业务单据ID',
  `role_id`       BIGINT       DEFAULT NULL COMMENT '审批角色ID',
  `approver_id`   BIGINT       DEFAULT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(64)  DEFAULT NULL COMMENT '审批人姓名',
  `result`        VARCHAR(16)  DEFAULT NULL COMMENT '结果：pass通过',
  `opinion`       VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录';

-- 附件
CREATE TABLE `attachment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type`    VARCHAR(32)  NOT NULL COMMENT '关联单据类型',
  `biz_id`      BIGINT       NOT NULL COMMENT '关联单据ID',
  `file_name`   VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_path`   VARCHAR(500) NOT NULL COMMENT '存储路径',
  `file_size`   BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
  `file_type`   VARCHAR(64)  DEFAULT NULL COMMENT '文件类型',
  `upload_by`   VARCHAR(64)  DEFAULT NULL COMMENT '上传人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件';

-- 操作日志
CREATE TABLE `operation_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `username`    VARCHAR(64)  DEFAULT NULL COMMENT '操作人用户名',
  `module`      VARCHAR(32)  DEFAULT NULL COMMENT '模块',
  `biz_type`    VARCHAR(32)  DEFAULT NULL COMMENT '单据类型',
  `biz_no`      VARCHAR(64)  DEFAULT NULL COMMENT '单据编号',
  `action`      VARCHAR(32)  NOT NULL COMMENT '动作：create/submit/approve等',
  `before_state` VARCHAR(32) DEFAULT NULL COMMENT '操作前状态',
  `after_state`  VARCHAR(32) DEFAULT NULL COMMENT '操作后状态',
  `detail`      VARCHAR(2000) DEFAULT NULL COMMENT '详情',
  `ip`          VARCHAR(64)  DEFAULT NULL COMMENT 'IP地址',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- 通知
CREATE TABLE `notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '接收用户ID',
  `type`        VARCHAR(32)  NOT NULL COMMENT '类型：approve待审批/low_stock低库存/delivery交期等',
  `title`       VARCHAR(128) NOT NULL COMMENT '标题',
  `content`     VARCHAR(1000) DEFAULT NULL COMMENT '内容',
  `biz_type`    VARCHAR(32)  DEFAULT NULL COMMENT '关联单据类型',
  `biz_id`      BIGINT       DEFAULT NULL COMMENT '关联单据ID',
  `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知';

-- ------------------------------------------------------------
-- 二、基础资料模块
-- ------------------------------------------------------------

-- 数据字典
CREATE TABLE `dictionary` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type`   VARCHAR(64)  NOT NULL COMMENT '字典类型',
  `dict_label`  VARCHAR(64)  NOT NULL COMMENT '字典标签',
  `dict_value`  VARCHAR(64)  NOT NULL COMMENT '字典值',
  `sort`        INT          DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典';

-- 客户档案
CREATE TABLE `customer` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)  NOT NULL COMMENT '客户编号（KH+日期+流水）',
  `name`        VARCHAR(128) NOT NULL COMMENT '客户名称',
  `category`    VARCHAR(32)  NOT NULL COMMENT '分类（字典）',
  `contact`     VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '电话',
  `email`       VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
  `address`     VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户档案';

-- 供应商档案
CREATE TABLE `supplier` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)  NOT NULL COMMENT '供应商编号（GY+日期+流水）',
  `name`        VARCHAR(128) NOT NULL COMMENT '供应商名称',
  `category`    VARCHAR(32)  NOT NULL COMMENT '分类（字典）',
  `contact`     VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '电话',
  `email`       VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
  `address`     VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商档案';

-- 物料档案
CREATE TABLE `material` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`              VARCHAR(32)  NOT NULL COMMENT '物料编号（WL+日期+流水）',
  `name`              VARCHAR(128) NOT NULL COMMENT '物料名称',
  `category`          VARCHAR(32)  NOT NULL COMMENT '分类（字典）',
  `unit`              VARCHAR(16)  NOT NULL COMMENT '单位（字典）',
  `spec`              VARCHAR(128) DEFAULT NULL COMMENT '规格',
  `supply_type`       VARCHAR(16)  NOT NULL DEFAULT 'purchase' COMMENT '供应属性：purchase采购/make自制/purchase_make采购+自制',
  `inventory_manage`  TINYINT      NOT NULL DEFAULT 1 COMMENT '库存管理：1是 0否',
  `default_warehouse_id` BIGINT    DEFAULT NULL COMMENT '默认仓库ID',
  `default_location_id`  BIGINT    DEFAULT NULL COMMENT '默认库位ID',
  `safety_stock`      DECIMAL(18,6) DEFAULT 0 COMMENT '安全库存',
  `max_stock`         DECIMAL(18,6) DEFAULT NULL COMMENT '最大库存',
  `min_stock`         DECIMAL(18,6) DEFAULT NULL COMMENT '最小库存',
  `batch_manage`      VARCHAR(16)  NOT NULL DEFAULT 'none' COMMENT '批次/序列号：none不管理/batch批次/serial序列号',
  `purchase_qc`       TINYINT      NOT NULL DEFAULT 0 COMMENT '采购质检：1是 0否',
  `production_qc`     TINYINT      NOT NULL DEFAULT 0 COMMENT '生产质检：1是 0否',
  `purchase_lead_days` INT         DEFAULT 0 COMMENT '采购提前期(天)',
  `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`            VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`         VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料档案';

-- 产品档案
CREATE TABLE `product` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`              VARCHAR(32)  NOT NULL COMMENT '产品编号（CP+日期+流水）',
  `name`              VARCHAR(128) NOT NULL COMMENT '产品名称',
  `category`          VARCHAR(32)  NOT NULL COMMENT '分类（字典）',
  `unit`              VARCHAR(16)  NOT NULL COMMENT '单位（字典）',
  `spec`              VARCHAR(128) DEFAULT NULL COMMENT '规格',
  `inventory_manage`  TINYINT      NOT NULL DEFAULT 1 COMMENT '库存管理：1是 0否',
  `batch_manage`      VARCHAR(16)  NOT NULL DEFAULT 'none' COMMENT '批次/序列号：none/batch/serial',
  `production_qc`     TINYINT      NOT NULL DEFAULT 0 COMMENT '生产质检：1是 0否',
  `production_lead_days` INT       DEFAULT 0 COMMENT '生产提前期(天)',
  `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`            VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`         VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品档案';

-- 仓库
CREATE TABLE `warehouse` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32)  NOT NULL COMMENT '仓库编号',
  `name`        VARCHAR(64)  NOT NULL COMMENT '仓库名称',
  `type`        VARCHAR(32)  NOT NULL COMMENT '类型（字典）',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库';

-- 库位
CREATE TABLE `location` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `warehouse_id` BIGINT      NOT NULL COMMENT '仓库ID',
  `code`         VARCHAR(32) NOT NULL COMMENT '库位编号',
  `name`         VARCHAR(64) DEFAULT NULL COMMENT '库位名称',
  `status`       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `deleted`      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`    VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64) DEFAULT NULL COMMENT '修改人',
  `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库位';

-- ------------------------------------------------------------
-- 三、BOM 模块
-- ------------------------------------------------------------

-- BOM 主表
CREATE TABLE `bom` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(32) NOT NULL COMMENT 'BOM编号（BOM+日期+流水）',
  `product_id`  BIGINT      NOT NULL COMMENT '产品ID（父项）',
  `name`        VARCHAR(128) DEFAULT NULL COMMENT 'BOM名称',
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1生效 0失效',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64) DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM主表';

-- BOM 版本
CREATE TABLE `bom_version` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bom_id`       BIGINT      NOT NULL COMMENT 'BOM ID',
  `version`      VARCHAR(32) NOT NULL COMMENT '版本号',
  `effective_date` DATE      DEFAULT NULL COMMENT '生效日期',
  `expire_date`  DATE        DEFAULT NULL COMMENT '失效日期',
  `status`       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1生效 0失效',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_bom` (`bom_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM版本';

-- BOM 明细
CREATE TABLE `bom_item` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bom_id`      BIGINT       NOT NULL COMMENT 'BOM ID',
  `bom_version_id` BIGINT    DEFAULT NULL COMMENT 'BOM版本ID',
  `child_id`    BIGINT       NOT NULL COMMENT '子物料/子产品ID',
  `child_type`  VARCHAR(16)  NOT NULL COMMENT '子项类型：material物料/product产品',
  `quantity`    DECIMAL(18,6) NOT NULL COMMENT '数量',
  `unit`        VARCHAR(16)  NOT NULL COMMENT '单位',
  `loss_rate`   DECIMAL(10,6) DEFAULT 0 COMMENT '损耗率',
  `process`     VARCHAR(64)  DEFAULT NULL COMMENT '工序',
  `sort`        INT          DEFAULT 0 COMMENT '排序',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_bom` (`bom_id`),
  KEY `idx_child` (`child_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM明细';

SET FOREIGN_KEY_CHECKS = 1;
