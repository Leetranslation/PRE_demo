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
@TableName("qc_apply")
@Schema(description = "质检申请单")
public class QcApply extends BaseEntity {
    @Schema(description = "申请单号（QS+日期+流水）")
    private String applyNo;
    @Schema(description = "质检来源（四线）：purchase_qc/production_qc/process_qc/outbound_qc")
    private String sourceType;
    @Schema(description = "来源单据ID")
    private Long sourceId;
    @Schema(description = "来源单据号")
    private String sourceNo;
    @Schema(description = "物料/产品ID")
    private Long itemId;
    @Schema(description = "类型：material/product")
    private String itemType;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "申请质检数量")
    private BigDecimal quantity;
    @Schema(description = "申请日期")
    private LocalDate applyDate;
    @Schema(description = "状态：pending待检/inspecting检验中/finished已检")
    private String status;
    @Schema(description = "关联检验单ID")
    private Long inspectionId;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "库位ID（精确维度匹配）")
    private Long locationId;
    @Schema(description = "批次号（精确维度匹配）")
    private String batchNo;
    @Schema(description = "序列号（精确维度匹配）")
    private String serialNo;

    /** 物料/产品编码、名称、仓库名称（冗余展示字段） */
    @TableField(exist = false)
    private String itemCode;
    @TableField(exist = false)
    private String itemName;
    @TableField(exist = false)
    private String warehouseName;
}
