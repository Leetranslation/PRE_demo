package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("quality_result")
public class QualityResult implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspectionId;
    private String handleType;
    private BigDecimal quantity;
    private String reason;
    private String resultStatus;
    private String createBy;
    private java.time.LocalDateTime createTime;
}
