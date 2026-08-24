package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bom")
public class Bom extends BaseEntity {
    private String code;
    private Long productId;
    private String name;
    private Integer status;
    private String remark;
}
