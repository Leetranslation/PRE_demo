package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("location")
public class Location extends BaseEntity {
    private Long warehouseId;
    /** 库区ID（可空，层级：仓库→库区→库位） */
    private Long areaId;
    private String code;
    private String name;
    private Integer status;
}
