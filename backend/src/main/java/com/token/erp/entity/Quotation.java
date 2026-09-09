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
@TableName("quotation")
@Schema(description = "报价单")
public class Quotation extends BaseEntity {
    @Schema(description = "报价单号（BJ+日期+流水）")
    private String quotationNo;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "报价日期")
    private LocalDate quoteDate;
    @Schema(description = "有效期至")
    private LocalDate validUntil;
    @Schema(description = "业务员")
    private String salesman;
    @Schema(description = "价税合计")
    private BigDecimal totalAmount;
    @Schema(description = "审批状态：pending/approving/approved")
    private String approveStatus;
    @Schema(description = "业务状态：pending未转单/converted已转单")
    private String bizStatus;
    @Schema(description = "备注")
    private String remark;
}
