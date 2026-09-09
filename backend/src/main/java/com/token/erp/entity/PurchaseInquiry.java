package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_inquiry")
@Schema(description = "采购询价单")
public class PurchaseInquiry extends BaseEntity {
    @Schema(description = "询价单号（XJ+日期+流水）")
    private String inquiryNo;
    @Schema(description = "询价日期")
    private LocalDate inquiryDate;
    @Schema(description = "询价人")
    private String inquirer;
    @Schema(description = "审批状态")
    private String approveStatus;
    @Schema(description = "业务状态：pending未比价/priced已比价")
    private String bizStatus;
    @Schema(description = "备注")
    private String remark;
}
