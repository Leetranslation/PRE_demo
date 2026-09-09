package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("serial_rule")
@Schema(description = "序列号规则")
public class SerialRule extends BaseEntity {
    @Schema(description = "规则编码")
    private String code;
    @Schema(description = "规则名称")
    private String name;
    @Schema(description = "序列号前缀")
    private String prefix;
    @Schema(description = "起始序号")
    private Long startNo;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
