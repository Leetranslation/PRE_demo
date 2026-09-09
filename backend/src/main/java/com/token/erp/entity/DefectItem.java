package com.token.erp.entity;

import com.token.erp.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("defect_item")
public class DefectItem extends BaseEntity {

    private String code;
    private String name;
    private String category;
    private Integer status;
    private String remark;
}
