package com.token.erp.entity;

import com.token.erp.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("outsource_order")
public class OutsourceOrder extends BaseEntity {

    private String orderNo;
    /** whole整单外协 / process工序外协 */
    private String type;
    private Long orderId;
    private Long processId;
    private Long supplierId;
    private BigDecimal quantity;
    private LocalDate deliveryDate;
    /** pending/approving/approved */
    private String approveStatus;
    /** pending未开始/outsourcing外协中/finished已完工/voided已作废 */
    private String bizStatus;
    private String remark;

    /** 供应商名称（冗余展示字段） */
    @TableField(exist = false)
    private String supplierName;
    /** 生产工单号（冗余展示字段） */
    @TableField(exist = false)
    private String sourceOrderNo;
    /** 工序名称（冗余展示字段） */
    @TableField(exist = false)
    private String processName;
}
