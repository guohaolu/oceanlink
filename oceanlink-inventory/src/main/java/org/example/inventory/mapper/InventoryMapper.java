package org.example.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存 Mapper
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
}
