package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equipment_repair")
@Schema(description = "设备维修工单")
public class EquipmentRepair extends BaseEntity {
    @Schema(description = "维修单号（JX+日期+流水）")
    private String repairNo;
    @Schema(description = "设备ID")
    private Long machineId;
    @Schema(description = "故障描述")
    private String faultDesc;
    @Schema(description = "报修日期")
    private LocalDate reportDate;
    @Schema(description = "维修人")
    private String repairer;
    @Schema(description = "状态：pending待维修/repairing维修中/done已完成")
    private String repairStatus;
    @Schema(description = "完成日期")
    private LocalDate finishDate;
    @Schema(description = "停机时长（小时）")
    private BigDecimal downtimeHours;
    @Schema(description = "维修费用")
    private BigDecimal cost;
    @Schema(description = "备注")
    private String remark;

    /** 设备名称/编号（冗余展示字段） */
    @TableField(exist = false)
    private String machineName;
    @TableField(exist = false)
    private String machineCode;
}
