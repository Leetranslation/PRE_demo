package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.OtherStockRequest;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.OtherStock;
import com.token.erp.entity.OtherStockItem;
import com.token.erp.mapper.OtherStockItemMapper;
import com.token.erp.mapper.OtherStockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OtherStockService {

    private final OtherStockMapper stockMapper;
    private final OtherStockItemMapper itemMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;
    private final IdempotencyService idempotencyService;

    public Page<OtherStock> page(long page, long pageSize, String direction, String type) {
        LambdaQueryWrapper<OtherStock> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(direction)) qw.eq(OtherStock::getDirection, direction);
        if (StringUtils.hasText(type)) qw.eq(OtherStock::getType, type);
        qw.orderByDesc(OtherStock::getId);
        return stockMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public List<OtherStockItem> items(Long stockId) {
        return itemMapper.selectList(
                new LambdaQueryWrapper<OtherStockItem>().eq(OtherStockItem::getStockId, stockId));
    }

    @Transactional
    public OtherStock create(OtherStockRequest request) {
        if (!"in".equals(request.getDirection()) && !"out".equals(request.getDirection())) {
            throw new BizException("方向必须为 in（入库）或 out（出库）");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("明细不能为空");
        }
        idempotencyService.checkAndOccupy("other_stock_" + System.currentTimeMillis(), "other_stock");

        OtherStock stock = new OtherStock();
        stock.setStockNo(codeGenerator.next("in".equals(request.getDirection()) ? "QTR" : "QTC"));
        stock.setDirection(request.getDirection());
        stock.setType(request.getType());
        stock.setWarehouseId(request.getWarehouseId());
        stock.setStockDate(request.getStockDate() != null ? request.getStockDate() : LocalDate.now());
        stock.setStatus("completed");
        stock.setRemark(request.getRemark());
        stockMapper.insert(stock);

        for (OtherStockRequest.Item item : request.getItems()) {
            if (item.getItemId() == null) throw new BizException("物料/产品不能为空");
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("数量必须大于0");
            }
            // 调库存服务变动（in 入库 / out 出库）
            Inventory dim = new Inventory();
            dim.setItemId(item.getItemId());
            dim.setItemType(item.getItemType() != null ? item.getItemType() : "material");
            dim.setWarehouseId(request.getWarehouseId());
            dim.setStockType("available");
            inventoryService.change(dim, request.getDirection(), item.getQuantity(),
                    "other_stock", stock.getId(), stock.getStockNo());

            OtherStockItem si = new OtherStockItem();
            si.setStockId(stock.getId());
            si.setItemId(item.getItemId());
            si.setItemType(item.getItemType() != null ? item.getItemType() : "material");
            si.setQuantity(item.getQuantity());
            si.setUnit(item.getUnit());
            si.setRemark(item.getRemark());
            itemMapper.insert(si);
        }
        return stock;
    }
}
