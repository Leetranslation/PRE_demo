package com.token.erp.dto;

import com.token.erp.entity.BomItem;
import lombok.Data;

import java.util.List;

/**
 * BOM 创建/更新请求（主表 + 明细）
 */
@Data
public class BomRequest {
    private Long id;
    private Long productId;
    private String name;
    private Integer status;
    private String remark;
    private String version;
    private List<BomItem> items;
}
