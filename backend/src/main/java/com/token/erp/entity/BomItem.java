package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("bom_item")
public class BomItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomId;
    private Long bomVersionId;
    private Long childId;
    private String childType;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal lossRate;
    private String process;
    private Integer sort;
    private String remark;
}
