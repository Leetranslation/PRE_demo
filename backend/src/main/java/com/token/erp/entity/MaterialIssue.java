package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_issue")
public class MaterialIssue extends BaseEntity {
    private String issueNo;
    private String issueType;
    private Long orderId;
    private Long outsourceId;
    private Long warehouseId;
    private LocalDate issueDate;
    private String status;
    private String remark;
}
