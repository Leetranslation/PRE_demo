package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("transfer_item")
public class TransferItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long transferId;
    private Long itemId;
    private String itemType;
    private BigDecimal quantity;
    private String unit;
}
