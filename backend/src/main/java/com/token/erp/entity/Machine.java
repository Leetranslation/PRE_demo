package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("machine")
@Schema(description = "机台设备")
public class Machine extends BaseEntity {
    @Schema(description = "机台编号（JT+日期+流水）")
    private String code;
    @Schema(description = "机台名称")
    private String name;
    @Schema(description = "所属车间ID")
    private Long workshopId;
    @Schema(description = "机台类型（字典）")
    private String type;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
