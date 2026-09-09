package com.token.erp.entity;

import com.token.erp.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("begin_balance")
public class BeginBalance extends BaseEntity {

    private String targetType;
    private Long targetId;
    private String targetName;
    private BigDecimal amount;
    private String remark;
}
