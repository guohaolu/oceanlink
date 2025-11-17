package org.example.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.mybatis.ClickhouseBaseMapper;
import org.example.pojo.entity.FinanceTpWarehouseAgeEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * 三方仓库龄Clickhouse Mapper
 *
 * @author guohao.lu
 */
@Mapper
@DS("clickhouse")
@InterceptorIgnore(tenantLine = "true")
public interface FinanceTpWarehouseAgeCkMapper extends ClickhouseBaseMapper<FinanceTpWarehouseAgeEntity> {
    void removeByDate(@Param("date") LocalDate date, @Param("list") List<String> providerNames);

    Page<FinanceTpWarehouseAgeEntity> pageByDate(@Param("date") LocalDate date, @Param("page") Page<?> page);
}
