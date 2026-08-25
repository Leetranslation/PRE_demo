package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("material_issue_item")
public class MaterialIssueItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long issueId;
    private Long materialId;
    private Long locationId;
    private String batchNo;
    private String serialNo;
    private BigDecimal quantity;
    private String unit;
}
