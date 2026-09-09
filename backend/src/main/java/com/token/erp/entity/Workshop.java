package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workshop")
@Schema(description = "车间")
public class Workshop extends BaseEntity {
    @Schema(description = "车间编码（CJ+日期+流水）")
    private String code;
    @Schema(description = "车间名称")
    private String name;
    @Schema(description = "车间描述")
    private String description;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
