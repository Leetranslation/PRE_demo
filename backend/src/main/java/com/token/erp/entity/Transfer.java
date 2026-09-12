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
@TableName("transfer")
public class Transfer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String transferNo;
    private Long sourceWarehouseId;
    private Long targetWarehouseId;
    private LocalDate transferDate;
    private String status;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;

    /** 调出/调入仓库名称（冗余展示字段） */
    @TableField(exist = false)
    private String sourceWarehouseName;
    @TableField(exist = false)
    private String targetWarehouseName;
}
