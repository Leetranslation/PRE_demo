package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.ContractRequest;
import com.token.erp.dto.SalesOrderRequest;
import com.token.erp.entity.Contract;
import com.token.erp.entity.ContractItem;
import com.token.erp.entity.SalesOrder;
import com.token.erp.entity.SalesOrderItem;
import com.token.erp.mapper.ContractItemMapper;
import com.token.erp.mapper.ContractMapper;
import com.token.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;
    private final ContractItemMapper itemMapper;
    private final SalesOrderService salesOrderService;
    private final CodeGenerator codeGenerator;
    private final SysConfigService sysConfigService;
    private final SecurityUtils securityUtils;
    private final NameFillUtil nameFillUtil;

    public Page<Contract> page(long page, long pageSize, String keyword, String approveStatus) {
        LambdaQueryWrapper<Contract> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Contract::getContractNo, keyword);
        }
        if (StringUtils.hasText(approveStatus)) {
            qw.eq(Contract::getApproveStatus, approveStatus);
        }
        qw.orderByDesc(Contract::getId);
        Page<Contract> result = contractMapper.selectPage(new Page<>(page, pageSize), qw);
        if (!result.getRecords().isEmpty()) {
            List<Long> cids = result.getRecords().stream().map(Contract::getId).collect(Collectors.toList());
            List<ContractItem> allItems = itemMapper.selectList(
                    new LambdaQueryWrapper<ContractItem>()
                            .in(ContractItem::getContractId, cids)
                            .orderByAsc(ContractItem::getId));
            Map<Long, String[]> pm = nameFillUtil.productMap(
                    allItems.stream().map(ContractItem::getProductId).distinct().collect(Collectors.toList()));
            Map<Long, List<ContractItem>> grouped = allItems.stream()
                    .collect(Collectors.groupingBy(ContractItem::getContractId));
            result.getRecords().forEach(c -> {
                List<ContractItem> its = grouped.get(c.getId());
                if (its == null || its.isEmpty()) return;
                String[] arr = pm.get(its.get(0).getProductId());
                long kinds = its.stream().map(ContractItem::getProductId).distinct().count();
                if (arr != null) {
                    c.setProductCode(arr[0] + (kinds > 1 ? " 等" + kinds + "种" : ""));
                    c.setProductName(arr[1]);
                }
            });
        }
        return result;
    }

    public List<ContractItem> items(Long contractId) {
        return itemMapper.selectList(
                new LambdaQueryWrapper<ContractItem>().eq(ContractItem::getContractId, contractId));
    }

    @Transactional
    public Contract create(ContractRequest request) {
        if (request.getCustomerId() == null) throw new BizException("客户不能为空");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException("合同明细不能为空");

        Contract contract = new Contract();
        contract.setContractNo(codeGenerator.next("HT"));
        contract.setCustomerId(request.getCustomerId());
        contract.setContractDate(request.getContractDate() != null ? request.getContractDate() : LocalDate.now());
        contract.setSalesman(request.getSalesman());
        contract.setPaymentTerms(request.getPaymentTerms());
        contract.setApproveStatus("pending");
        contract.setBizStatus("pending");
        contract.setAmount(BigDecimal.ZERO);
        contract.setRemark(request.getRemark());
        contractMapper.insert(contract);

        BigDecimal total = saveItems(contract.getId(), request.getItems());
        contract.setAmount(total);
        contractMapper.updateById(contract);
        return contract;
    }

    @Transactional
    public void submit(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BizException(404, "合同不存在");
        if (!"pending".equals(c.getApproveStatus())) throw new BizException("仅待提交状态可提交");
        c.setApproveStatus("approving");
        contractMapper.updateById(c);
    }

    @Transactional
    public void approve(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BizException(404, "合同不存在");
        if (!"approving".equals(c.getApproveStatus())) throw new BizException("仅审批中状态可审批");
        checkSelfApprove(c.getCreateBy());
        c.setApproveStatus("approved");
        contractMapper.updateById(c);
    }

    @Transactional
    public void unApprove(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BizException(404, "合同不存在");
        if (!"approved".equals(c.getApproveStatus())) throw new BizException("仅已审批可反审核");
        if ("converted".equals(c.getBizStatus())) throw new BizException("已转销售订单的合同不能反审核");
        c.setApproveStatus("pending");
        contractMapper.updateById(c);
    }

    /** 合同转销售订单 */
    @Transactional
    public SalesOrder convertToSalesOrder(Long id) {
        Contract c = contractMapper.selectById(id);
        if (c == null) throw new BizException(404, "合同不存在");
        if (!"approved".equals(c.getApproveStatus())) throw new BizException("仅已审批的合同可转销售订单");
        if ("converted".equals(c.getBizStatus())) throw new BizException("该合同已转销售订单");

        List<ContractItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ContractItem>().eq(ContractItem::getContractId, id));

        SalesOrderRequest req = new SalesOrderRequest();
        req.setCustomerId(c.getCustomerId());
        req.setSalesman(c.getSalesman());
        List<SalesOrderItem> soItems = new ArrayList<>();
        for (ContractItem it : items) {
            SalesOrderItem soItem = new SalesOrderItem();
            soItem.setProductId(it.getProductId());
            soItem.setSpec(it.getSpec());
            soItem.setUnit(it.getUnit());
            soItem.setQuantity(it.getQuantity());
            soItem.setPrice(it.getPrice());
            soItem.setTaxRate(it.getTaxRate());
            soItem.setRemark(it.getRemark());
            soItems.add(soItem);
        }
        req.setItems(soItems);

        SalesOrder order = salesOrderService.create(req);
        c.setBizStatus("converted");
        contractMapper.updateById(c);
        return order;
    }

    private void checkSelfApprove(String createBy) {
        if (!sysConfigService.getBoolean("allow_self_approve")) {
            String currentUser = securityUtils.currentUsername();
            if (createBy != null && createBy.equals(currentUser)) {
                throw new BizException("禁止自审：不能审批自己提交的单据");
            }
        }
    }

    private BigDecimal saveItems(Long contractId, List<ContractRequest.Item> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (ContractRequest.Item item : items) {
            if (item.getProductId() == null) throw new BizException("产品不能为空");
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            BigDecimal taxRate = item.getTaxRate() == null ? new BigDecimal("13") : item.getTaxRate();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("数量必须大于0");
            if (price.compareTo(BigDecimal.ZERO) < 0) throw new BizException("单价不能为负数");

            BigDecimal totalAmount = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);

            ContractItem ci = new ContractItem();
            ci.setContractId(contractId);
            ci.setProductId(item.getProductId());
            ci.setSpec(item.getSpec());
            ci.setUnit(item.getUnit());
            ci.setQuantity(qty);
            ci.setPrice(price);
            ci.setTaxRate(taxRate);
            ci.setTotalAmount(totalAmount);
            ci.setRemark(item.getRemark());
            itemMapper.insert(ci);

            total = total.add(totalAmount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
