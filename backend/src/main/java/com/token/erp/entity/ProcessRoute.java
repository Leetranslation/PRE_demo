package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("process_route")
public class ProcessRoute extends BaseEntity {
    private String code;
    private String name;
    private Long productId;
    private String version;
    private Integer status;
    private String remark;
}
