package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    @Schema(description = "型号")
    private String model;
    @Schema(description = "厂商")
    private String manufacturer;
    @Schema(description = "购置日期")
    private LocalDate purchaseDate;
    @Schema(description = "购置价格")
    private BigDecimal purchasePrice;
    @Schema(description = "保修期至")
    private LocalDate warrantyUntil;
    @Schema(description = "设备状态：normal正常/repairing维修中/stopped停用/scrapped报废")
    private String equipStatus;
    @Schema(description = "保养周期（天）")
    private Integer maintenanceCycle;
    @Schema(description = "上次保养日期")
    private LocalDate lastMaintenanceDate;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
