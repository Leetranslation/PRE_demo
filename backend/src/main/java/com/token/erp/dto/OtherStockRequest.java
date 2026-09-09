package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "其他出入库单请求")
public class OtherStockRequest {
    @Schema(description = "方向：in入库/out出库")
    private String direction;
    @Schema(description = "类型：期初/盘盈/盘亏/领用/其他")
    private String type;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "日期")
    private LocalDate stockDate;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "明细")
    private List<Item> items;

    @Data
    @Schema(description = "明细")
    public static class Item {
        @Schema(description = "物料/产品ID")
        private Long itemId;
        @Schema(description = "类型：material/product")
        private String itemType;
        @Schema(description = "数量")
        private BigDecimal quantity;
        @Schema(description = "单位")
        private String unit;
        @Schema(description = "备注")
        private String remark;
    }
}
