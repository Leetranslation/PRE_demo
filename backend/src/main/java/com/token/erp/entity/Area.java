package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("area")
@Schema(description = "库区")
public class Area extends BaseEntity {
    @Schema(description = "库区编码")
    private String code;
    @Schema(description = "库区名称")
    private String name;
    @Schema(description = "所属仓库ID")
    private Long warehouseId;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
