package org.example.mybatis;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.session.Configuration;

import java.util.List;

/**
 * ClickHouseSqlInjector
 *
 * @author guohao.lu
 */
public class ClickHouseSqlInjector extends DefaultSqlInjector {
    /**
     * 获取注入的方法
     *
     * @param configuration 配置对象
     * @param mapperClass   当前mapper
     * @param tableInfo     表信息
     * @return 注入方法集合
     * @since 3.5.6
     */
    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(configuration,mapperClass,tableInfo);
        methodList.add(new AsyncInsertClickhouse());

        return methodList;
    }
}