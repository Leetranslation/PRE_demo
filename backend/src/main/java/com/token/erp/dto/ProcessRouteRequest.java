package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工艺路线请求 DTO（主表 + 明细）
 */
@Data
@Schema(description = "工艺路线请求参数")
public class ProcessRouteRequest {
    @Schema(description = "工艺路线ID（更新时必填）")
    private Long id;
    @Schema(description = "工艺路线名称")
    private String name;
    @Schema(description = "产品ID")
    private Long productId;
    @Schema(description = "版本号")
    private String version;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "工序明细")
    private List<Item> items;

    @Data
    @Schema(description = "工艺路线工序明细")
    public static class Item {
        @Schema(description = "明细ID（更新时可选）")
        private Long id;
        @Schema(description = "工序ID")
        private Long processId;
        @Schema(description = "工序顺序号")
        private Integer seqNo;
        @Schema(description = "标准工时（小时）")
        private BigDecimal standardHours;
        @Schema(description = "是否关键工序：1是 0否")
        private Integer isKey;
        @Schema(description = "是否检验工序：1是 0否")
        private Integer isQc;
        @Schema(description = "备注")
        private String remark;
    }
}
