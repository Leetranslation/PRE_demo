package com.token.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.token.erp.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 按库存维度查询并加行级锁（FOR UPDATE），用于并发安全地更新库存数量
     * 库位/批次/序列号可能为 null，使用 IS NULL 判断
     */
    @Select("SELECT * FROM inventory " +
            "WHERE item_id = #{itemId} AND item_type = #{itemType} " +
            "AND warehouse_id = #{warehouseId} AND stock_type = #{stockType} " +
            "AND location_id <=> #{locationId} " +
            "AND batch_no <=> #{batchNo} " +
            "AND serial_no <=> #{serialNo} " +
            "FOR UPDATE")
    Inventory selectForUpdate(@Param("itemId") Long itemId,
                              @Param("itemType") String itemType,
                              @Param("warehouseId") Long warehouseId,
                              @Param("stockType") String stockType,
                              @Param("locationId") Long locationId,
                              @Param("batchNo") String batchNo,
                              @Param("serialNo") String serialNo);
}
