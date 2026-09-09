package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_requisition")
@Schema(description = "采购申请单")
public class PurchaseRequisition extends BaseEntity {
    @Schema(description = "申请单号（SQ+日期+流水）")
    private String requisitionNo;
    @Schema(description = "申请部门ID")
    private Long departmentId;
    @Schema(description = "申请日期")
    private LocalDate requisitionDate;
    @Schema(description = "申请人")
    private String applicant;
    @Schema(description = "审批状态：pending/approving/approved")
    private String approveStatus;
    @Schema(description = "业务状态：pending未采购/purchased已转采购")
    private String bizStatus;
    @Schema(description = "备注")
    private String remark;
}
