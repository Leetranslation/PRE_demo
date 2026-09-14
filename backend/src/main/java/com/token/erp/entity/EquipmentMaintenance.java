package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment_maintenance")
@Schema(description = "设备保养记录")
public class EquipmentMaintenance extends BaseEntity {
    @Schema(description = "设备ID")
    private Long machineId;
    @Schema(description = "计划保养日期")
    private LocalDate planDate;
    @Schema(description = "实际保养日期")
    private LocalDate actualDate;
    @Schema(description = "保养内容")
    private String content;
    @Schema(description = "状态：planned计划/done完成")
    private String status;
    @Schema(description = "保养人")
    private String maintainer;
    @Schema(description = "备注")
    private String remark;

    /** 设备名称/编号（冗余展示字段） */
    @TableField(exist = false)
    private String machineName;
    @TableField(exist = false)
    private String machineCode;
}
