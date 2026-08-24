package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("bom_version")
public class BomVersion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomId;
    private String version;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer status;
    private LocalDateTime createTime;
}
