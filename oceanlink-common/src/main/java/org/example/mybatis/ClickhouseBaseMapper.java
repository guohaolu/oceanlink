package org.example.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Collection;

/**
 * base mapper 支持 clickhouse自定义方法
 *
 * @author guohao.lu
 */
public interface ClickhouseBaseMapper<T> extends BaseMapper<T> {
    /**
     * clickhouse异步批量插入
     *
     * @param entityList 实体列表
     */
    void asyncInsertClickhouse(Collection<T> entityList);
}
