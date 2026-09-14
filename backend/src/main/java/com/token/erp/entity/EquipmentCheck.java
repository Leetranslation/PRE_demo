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
@TableName("equipment_check")
@Schema(description = "设备点检记录")
public class EquipmentCheck extends BaseEntity {
    @Schema(description = "设备ID")
    private Long machineId;
    @Schema(description = "点检日期")
    private LocalDate checkDate;
    @Schema(description = "点检项目")
    private String checkItem;
    @Schema(description = "结果：normal正常/abnormal异常")
    private String result;
    @Schema(description = "点检人")
    private String checker;
    @Schema(description = "备注")
    private String remark;

    /** 设备名称/编号（冗余展示字段） */
    @TableField(exist = false)
    private String machineName;
    @TableField(exist = false)
    private String machineCode;
}
