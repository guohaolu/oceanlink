package org.example.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.inventory.entity.InventoryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存扣减流水 Mapper
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {
}
