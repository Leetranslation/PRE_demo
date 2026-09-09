package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("unit")
@Schema(description = "单位")
public class Unit extends BaseEntity {
    @Schema(description = "单位编码")
    private String code;
    @Schema(description = "单位名称")
    private String name;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
