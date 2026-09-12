package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stocktake")
public class Stocktake implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String stocktakeNo;
    private Long warehouseId;
    private LocalDate stocktakeDate;
    private String status;
    private String approveStatus;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;

    /** 仓库名称（冗余展示字段） */
    @TableField(exist = false)
    private String warehouseName;
}
