package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("work_calendar")
@Schema(description = "工作日历")
public class WorkCalendar {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "日期")
    private LocalDate workDate;
    @Schema(description = "是否工作日：1是 0否")
    private Integer isWorkday;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
}
