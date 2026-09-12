package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("department")
public class Department extends BaseEntity {
    private String code;
    private String name;
    private Long parentId;
    private Integer sort;
    private Integer status;
    private String remark;

    /** 上级部门名称（冗余展示字段） */
    @TableField(exist = false)
    private String parentName;
}
