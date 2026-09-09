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
@TableName("contract")
@Schema(description = "合同")
public class Contract extends BaseEntity {
    @Schema(description = "合同号（HT+日期+流水）")
    private String contractNo;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "签订日期")
    private LocalDate contractDate;
    @Schema(description = "合同金额")
    private BigDecimal amount;
    @Schema(description = "业务员")
    private String salesman;
    @Schema(description = "付款方式/条款")
    private String paymentTerms;
    @Schema(description = "审批状态")
    private String approveStatus;
    @Schema(description = "业务状态：pending/converted")
    private String bizStatus;
    @Schema(description = "备注")
    private String remark;
}
