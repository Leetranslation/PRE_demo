package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.ProcessRouteRequest;
import com.token.erp.entity.ProcessRoute;
import com.token.erp.entity.ProcessRouteItem;
import com.token.erp.mapper.ProcessRouteItemMapper;
import com.token.erp.mapper.ProcessRouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessRouteService {

    private final ProcessRouteMapper processRouteMapper;
    private final ProcessRouteItemMapper processRouteItemMapper;
    private final CodeGenerator codeGenerator;

    public Page<ProcessRoute> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<ProcessRoute> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(ProcessRoute::getName, keyword)
                    .or().like(ProcessRoute::getCode, keyword));
        }
        if (status != null) {
            qw.eq(ProcessRoute::getStatus, status);
        }
        qw.orderByDesc(ProcessRoute::getId);
        return processRouteMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public List<ProcessRouteItem> items(Long routeId) {
        return processRouteItemMapper.selectList(
                new LambdaQueryWrapper<ProcessRouteItem>()
                        .eq(ProcessRouteItem::getRouteId, routeId)
                        .orderByAsc(ProcessRouteItem::getSeqNo));
    }

    @Transactional
    public ProcessRoute create(ProcessRouteRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException("工艺路线名称不能为空");
        }
        if (req.getProductId() == null) {
            throw new BizException("产品不能为空");
        }
        ProcessRoute route = new ProcessRoute();
        route.setId(null);
        route.setCode(codeGenerator.next("GY"));
        route.setName(req.getName());
        route.setProductId(req.getProductId());
        route.setVersion(req.getVersion());
        route.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        route.setRemark(req.getRemark());
        processRouteMapper.insert(route);

        saveItems(route.getId(), req.getItems());
        return route;
    }

    @Transactional
    public ProcessRoute update(ProcessRouteRequest req) {
        ProcessRoute existing = processRouteMapper.selectById(req.getId());
        if (existing == null) {
            throw new BizException(404, "工艺路线不存在");
        }
        if (StringUtils.hasText(req.getName())) {
            existing.setName(req.getName());
        }
        if (req.getProductId() != null) {
            existing.setProductId(req.getProductId());
        }
        existing.setVersion(req.getVersion());
        existing.setRemark(req.getRemark());
        // 编号不可修改
        processRouteMapper.updateById(existing);

        // 明细整体替换
        processRouteItemMapper.delete(
                new LambdaQueryWrapper<ProcessRouteItem>()
                        .eq(ProcessRouteItem::getRouteId, existing.getId()));
        saveItems(existing.getId(), req.getItems());
        return processRouteMapper.selectById(existing.getId());
    }

    public void changeStatus(Long id, Integer status) {
        if (processRouteMapper.selectById(id) == null) {
            throw new BizException(404, "工艺路线不存在");
        }
        ProcessRoute upd = new ProcessRoute();
        upd.setId(id);
        upd.setStatus(status);
        processRouteMapper.updateById(upd);
    }

    public void delete(Long id) {
        if (processRouteMapper.selectById(id) == null) {
            throw new BizException(404, "工艺路线不存在");
        }
        processRouteMapper.deleteById(id);
        processRouteItemMapper.delete(
                new LambdaQueryWrapper<ProcessRouteItem>()
                        .eq(ProcessRouteItem::getRouteId, id));
    }

    private void saveItems(Long routeId, List<ProcessRouteRequest.Item> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        int seq = 1;
        for (ProcessRouteRequest.Item item : items) {
            if (item.getProcessId() == null) {
                continue;
            }
            ProcessRouteItem pi = new ProcessRouteItem();
            pi.setRouteId(routeId);
            pi.setProcessId(item.getProcessId());
            pi.setSeqNo(item.getSeqNo() != null ? item.getSeqNo() : seq);
            pi.setStandardHours(item.getStandardHours());
            pi.setIsKey(item.getIsKey() == null ? 0 : item.getIsKey());
            pi.setIsQc(item.getIsQc() == null ? 0 : item.getIsQc());
            pi.setRemark(item.getRemark());
            processRouteItemMapper.insert(pi);
            seq++;
        }
    }
}
