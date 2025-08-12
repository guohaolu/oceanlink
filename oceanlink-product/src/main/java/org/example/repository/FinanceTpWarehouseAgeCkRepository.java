package org.example.repository;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.mapper.FinanceTpWarehouseAgeCkMapper;
import org.example.pojo.entity.FinanceTpWarehouseAgeEntity;
import org.springframework.stereotype.Repository;

/**
 * 三方仓库龄Repository
 *
 * @author guohao.lu
 */
@Repository
@DS("clickhouse")
public class FinanceTpWarehouseAgeCkRepository extends ServiceImpl<FinanceTpWarehouseAgeCkMapper, FinanceTpWarehouseAgeEntity> {
}
