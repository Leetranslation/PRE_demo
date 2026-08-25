package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mrp_plan")
public class MrpPlan implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String planNo;
    private LocalDate planDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String createBy;
    private LocalDateTime createTime;
}
