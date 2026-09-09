package com.token.erp.entity;

import com.token.erp.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_scheme")
public class InspectionScheme extends BaseEntity {

    private String code;
    private String name;
    private String itemType;
    private Long inspectionMethodId;
    private String standard;
    private Integer status;
    private String remark;
}
