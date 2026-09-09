package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.common.BizException;
import com.token.erp.dto.MaterialShortageVO;
import com.token.erp.entity.Bom;
import com.token.erp.entity.BomItem;
import com.token.erp.entity.Material;
import com.token.erp.entity.Product;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.mapper.BomItemMapper;
import com.token.erp.mapper.BomMapper;
import com.token.erp.mapper.MaterialMapper;
import com.token.erp.mapper.ProductMapper;
import com.token.erp.mapper.ProductionOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 齐套分析：按工单 BOM 展开物料需求，对比可用库存，得出缺料清单
 */
@Service
@RequiredArgsConstructor
public class CompleteAnalysisService {

    private final ProductionOrderMapper productionOrderMapper;
    private final BomMapper bomMapper;
    private final BomItemMapper bomItemMapper;
    private final MaterialMapper materialMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;

    public List<MaterialShortageVO> analysis(Long workOrderId) {
        ProductionOrder order = productionOrderMapper.selectById(workOrderId);
        if (order == null) throw new BizException(404, "工单不存在");
        if (order.getProductId() == null) throw new BizException("工单未关联产品");

        Bom bom = bomMapper.selectOne(new LambdaQueryWrapper<Bom>()
                .eq(Bom::getProductId, order.getProductId())
                .orderByDesc(Bom::getId).last("limit 1"));
        if (bom == null) return List.of();

        List<BomItem> items = bomItemMapper.selectList(
                new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, bom.getId()));

        BigDecimal orderQty = order.getQuantity() == null ? BigDecimal.ZERO : order.getQuantity();
        List<MaterialShortageVO> result = new ArrayList<>();
        for (BomItem bi : items) {
            BigDecimal usage = bi.getQuantity() == null ? BigDecimal.ZERO : bi.getQuantity();
            BigDecimal loss = bi.getLossRate() == null ? BigDecimal.ZERO : bi.getLossRate();
            BigDecimal lossFactor = BigDecimal.ONE.add(
                    loss.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
            BigDecimal required = orderQty.multiply(usage).multiply(lossFactor).setScale(6, RoundingMode.HALF_UP);

            String childType = bi.getChildType() == null ? "material" : bi.getChildType();
            BigDecimal available = inventoryService.availableQty(bi.getChildId(), childType);

            BigDecimal shortage = required.subtract(available);
            if (shortage.compareTo(BigDecimal.ZERO) < 0) shortage = BigDecimal.ZERO;

            MaterialShortageVO vo = new MaterialShortageVO();
            vo.setMaterialId(bi.getChildId());
            vo.setMaterialType(childType);
            vo.setUnitUsage(usage);
            vo.setLossRate(loss);
            vo.setRequiredQty(required);
            vo.setAvailableQty(available);
            vo.setShortageQty(shortage);
            if ("product".equals(childType)) {
                Product p = productMapper.selectById(bi.getChildId());
                if (p != null) {
                    vo.setMaterialCode(p.getCode());
                    vo.setMaterialName(p.getName());
                }
            } else {
                Material m = materialMapper.selectById(bi.getChildId());
                if (m != null) {
                    vo.setMaterialCode(m.getCode());
                    vo.setMaterialName(m.getName());
                }
            }
            result.add(vo);
        }
        return result;
    }
}
