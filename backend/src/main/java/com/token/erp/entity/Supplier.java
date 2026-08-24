package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier")
public class Supplier extends BaseEntity {
    private String code;
    private String name;
    private String category;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private Integer status;
    private String remark;
}
