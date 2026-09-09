package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("other_stock")
@Schema(description = "其他出入库单")
public class OtherStock extends BaseEntity {
    @Schema(description = "单号（QTR其他入库/QTC其他出库+日期+流水）")
    private String stockNo;
    @Schema(description = "方向：in入库/out出库")
    private String direction;
    @Schema(description = "类型：期初/盘盈/盘亏/领用/其他")
    private String type;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "日期")
    private LocalDate stockDate;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "备注")
    private String remark;
}
