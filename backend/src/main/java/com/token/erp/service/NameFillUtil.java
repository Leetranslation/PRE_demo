package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.entity.Customer;
import com.token.erp.entity.Location;
import com.token.erp.entity.Material;
import com.token.erp.entity.Process;
import com.token.erp.entity.Product;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.entity.Supplier;
import com.token.erp.entity.Warehouse;
import com.token.erp.entity.SalesOrder;
import com.token.erp.entity.PurchaseOrder;
import com.token.erp.entity.SalesInvoice;
import com.token.erp.entity.PurchaseInvoice;
import com.token.erp.entity.SalesOutbound;
import com.token.erp.mapper.CustomerMapper;
import com.token.erp.mapper.LocationMapper;
import com.token.erp.mapper.MaterialMapper;
import com.token.erp.mapper.ProcessMapper;
import com.token.erp.mapper.ProductMapper;
import com.token.erp.mapper.ProductionOrderMapper;
import com.token.erp.mapper.SupplierMapper;
import com.token.erp.mapper.WarehouseMapper;
import com.token.erp.mapper.SalesOrderMapper;
import com.token.erp.mapper.PurchaseOrderMapper;
import com.token.erp.mapper.SalesInvoiceMapper;
import com.token.erp.mapper.PurchaseInvoiceMapper;
import com.token.erp.mapper.SalesOutboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 名称填充工具：批量查询基础资料，把外键 ID 映射成可读的编码/名称，
 * 供各 Service 的列表/明细接口填充冗余展示字段，避免前端裸显示 ID。
 */
@Component
@RequiredArgsConstructor
public class NameFillUtil {

    private final ProductMapper productMapper;
    private final MaterialMapper materialMapper;
    private final SupplierMapper supplierMapper;
    private final CustomerMapper customerMapper;
    private final WarehouseMapper warehouseMapper;
    private final LocationMapper locationMapper;
    private final ProductionOrderMapper productionOrderMapper;
    private final ProcessMapper processMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SalesInvoiceMapper salesInvoiceMapper;
    private final PurchaseInvoiceMapper purchaseInvoiceMapper;
    private final SalesOutboundMapper salesOutboundMapper;

    private static Set<Long> nonNull(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptySet();
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** 产品 id -> [code, name] */
    public Map<Long, String[]> productMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<Product> list = productMapper.selectList(new LambdaQueryWrapper<Product>().in(Product::getId, set));
        Map<Long, String[]> map = new HashMap<>();
        for (Product p : list) map.put(p.getId(), new String[]{p.getCode(), p.getName()});
        return map;
    }

    /** 物料 id -> [code, name] */
    public Map<Long, String[]> materialMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<Material> list = materialMapper.selectList(new LambdaQueryWrapper<Material>().in(Material::getId, set));
        Map<Long, String[]> map = new HashMap<>();
        for (Material m : list) map.put(m.getId(), new String[]{m.getCode(), m.getName()});
        return map;
    }

    /** 供应商 id -> name */
    public Map<Long, String> supplierNameMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<Supplier> list = supplierMapper.selectList(new LambdaQueryWrapper<Supplier>().in(Supplier::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (Supplier s : list) map.put(s.getId(), s.getName());
        return map;
    }

    /** 客户 id -> name */
    public Map<Long, String> customerNameMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<Customer> list = customerMapper.selectList(new LambdaQueryWrapper<Customer>().in(Customer::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (Customer c : list) map.put(c.getId(), c.getName());
        return map;
    }

    /** 仓库 id -> name */
    public Map<Long, String> warehouseNameMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<Warehouse> list = warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>().in(Warehouse::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (Warehouse w : list) map.put(w.getId(), w.getName());
        return map;
    }

    /** 库位 id -> name */
    public Map<Long, String> locationNameMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<Location> list = locationMapper.selectList(new LambdaQueryWrapper<Location>().in(Location::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (Location l : list) map.put(l.getId(), l.getName());
        return map;
    }

    /** 生产工单 id -> orderNo */
    public Map<Long, String> productionOrderNoMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<ProductionOrder> list = productionOrderMapper.selectList(new LambdaQueryWrapper<ProductionOrder>().in(ProductionOrder::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (ProductionOrder o : list) map.put(o.getId(), o.getOrderNo());
        return map;
    }

    /** 工序 id -> name */
    public Map<Long, String> processNameMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<Process> list = processMapper.selectList(new LambdaQueryWrapper<Process>().in(Process::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (Process p : list) map.put(p.getId(), p.getName());
        return map;
    }

    /** 销售订单 id -> orderNo */
    public Map<Long, String> salesOrderNoMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<SalesOrder> list = salesOrderMapper.selectList(new LambdaQueryWrapper<SalesOrder>().in(SalesOrder::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (SalesOrder o : list) map.put(o.getId(), o.getOrderNo());
        return map;
    }

    /** 采购订单 id -> orderNo */
    public Map<Long, String> purchaseOrderNoMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<PurchaseOrder> list = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>().in(PurchaseOrder::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (PurchaseOrder o : list) map.put(o.getId(), o.getOrderNo());
        return map;
    }

    /** 销售发票 id -> invoiceNo */
    public Map<Long, String> salesInvoiceNoMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<SalesInvoice> list = salesInvoiceMapper.selectList(new LambdaQueryWrapper<SalesInvoice>().in(SalesInvoice::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (SalesInvoice o : list) map.put(o.getId(), o.getInvoiceNo());
        return map;
    }

    /** 采购发票 id -> invoiceNo */
    public Map<Long, String> purchaseInvoiceNoMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<PurchaseInvoice> list = purchaseInvoiceMapper.selectList(new LambdaQueryWrapper<PurchaseInvoice>().in(PurchaseInvoice::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (PurchaseInvoice o : list) map.put(o.getId(), o.getInvoiceNo());
        return map;
    }

    /** 销售出库单 id -> outboundNo */
    public Map<Long, String> salesOutboundNoMap(Collection<Long> ids) {
        Set<Long> set = nonNull(ids);
        if (set.isEmpty()) return Collections.emptyMap();
        List<SalesOutbound> list = salesOutboundMapper.selectList(new LambdaQueryWrapper<SalesOutbound>().in(SalesOutbound::getId, set));
        Map<Long, String> map = new HashMap<>();
        for (SalesOutbound o : list) map.put(o.getId(), o.getOutboundNo());
        return map;
    }

    /** 根据 itemType 解析编码（product / material 统一取 code） */
    public String itemCode(Long itemId, String itemType, Map<Long, String[]> pm, Map<Long, String[]> mm) {
        String[] arr = resolve(itemId, itemType, pm, mm);
        return arr == null ? null : arr[0];
    }

    /** 根据 itemType 解析名称（product / material 统一取 name） */
    public String itemName(Long itemId, String itemType, Map<Long, String[]> pm, Map<Long, String[]> mm) {
        String[] arr = resolve(itemId, itemType, pm, mm);
        return arr == null ? null : arr[1];
    }

    private String[] resolve(Long itemId, String itemType, Map<Long, String[]> pm, Map<Long, String[]> mm) {
        if (itemId == null) return null;
        return "product".equals(itemType) ? pm.get(itemId) : mm.get(itemId);
    }
}
