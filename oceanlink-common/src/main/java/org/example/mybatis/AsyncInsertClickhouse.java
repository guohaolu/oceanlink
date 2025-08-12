package org.example.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.stream.Collectors;

/**
 * Clickhouse异步插入
 *
 * @author guohao.lu
 */
public class AsyncInsertClickhouse extends AbstractMethod {
    public AsyncInsertClickhouse() {
        super("asyncInsertClickhouse");
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        String sql = "INSERT INTO " + tableInfo.getTableName() + "(" + tableInfo.getFieldList().stream()
                .map(TableFieldInfo::getColumn)
                .collect(Collectors.joining(","))
                + ") SETTINGS async_insert=1, wait_for_async_insert=1 VALUES ";

        String value = "(" + tableInfo.getFieldList().stream().map(tableFieldInfo -> "#{" + ENTITY + DOT + tableFieldInfo.getProperty() + "}")
                .collect(Collectors.joining(",")) + ")";

        String valuesScript = SqlScriptUtils.convertForeach(value, "list", null, ENTITY, COMMA);
        SqlSource sqlSource = super.createSqlSource(configuration, "<script>" + sql + valuesScript +
                "</script>", modelClass);
        KeyGenerator keyGenerator = tableInfo.getIdType() == IdType.AUTO ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
        // 第三个参数必须和baseMapper的自定义方法名一致
        return this.addInsertMappedStatement(mapperClass, modelClass, this.methodName, sqlSource, keyGenerator, tableInfo.getKeyProperty(), tableInfo.getKeyColumn());
    }
}