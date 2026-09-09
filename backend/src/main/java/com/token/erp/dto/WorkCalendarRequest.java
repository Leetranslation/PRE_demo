package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工作日历批量设置请求")
public class WorkCalendarRequest {
    @Schema(description = "月份（yyyy-MM）")
    private String yearMonth;
    @Schema(description = "是否工作日：1是 0否")
    private Integer isWorkday;
    @Schema(description = "备注")
    private String remark;
}
