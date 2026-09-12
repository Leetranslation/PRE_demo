package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.PackingRequest;
import com.token.erp.entity.Packing;
import com.token.erp.entity.PackingItem;
import com.token.erp.mapper.PackingItemMapper;
import com.token.erp.mapper.PackingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackingService {

    private final PackingMapper packingMapper;
    private final PackingItemMapper itemMapper;
    private final CodeGenerator codeGenerator;
    private final NameFillUtil nameFillUtil;

    public Page<Packing> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<Packing> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) qw.like(Packing::getPackingNo, keyword);
        if (StringUtils.hasText(approveStatus)) qw.eq(Packing::getApproveStatus, approveStatus);
        qw.orderByDesc(Packing::getId);
        Page<Packing> result = packingMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            Map<Long, String> om = nameFillUtil.salesOutboundNoMap(
                    result.getRecords().stream().map(Packing::getOutboundId).collect(Collectors.toList()));
            result.getRecords().forEach(p -> p.setOutboundNo(om.get(p.getOutboundId())));
        }
        return result;
    }

    public List<PackingItem> items(Long packingId) {
        return itemMapper.selectList(
                new LambdaQueryWrapper<PackingItem>().eq(PackingItem::getPackingId, packingId));
    }

    @Transactional
    public Packing create(PackingRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("装箱明细不能为空");
        Packing packing = new Packing();
        packing.setPackingNo(codeGenerator.next("ZX"));
        packing.setOutboundId(request.getOutboundId());
        packing.setApproveStatus("pending");
        packing.setRemark(request.getRemark());
        packingMapper.insert(packing);

        for (PackingRequest.Item item : request.getItems()) {
            if (item.getProductId() == null) throw new BizException("产品不能为空");
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("数量必须大于0");
            }
            PackingItem pi = new PackingItem();
            pi.setPackingId(packing.getId());
            pi.setBoxNo(item.getBoxNo());
            pi.setProductId(item.getProductId());
            pi.setQuantity(item.getQuantity());
            pi.setRemark(item.getRemark());
            itemMapper.insert(pi);
        }
        return packing;
    }
}
