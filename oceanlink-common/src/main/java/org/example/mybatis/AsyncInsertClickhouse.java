package org.example.mybatis;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Clickhouse异步插入
 *
 * @author guohao.lu
 */
@Slf4j
public class AsyncInsertClickhouse extends AbstractMethod {
    public AsyncInsertClickhouse() {
        super("asyncInsertClickhouse");
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // SETTINGS async_insert=1, wait_for_async_insert=1
        String sql = """
                <script>
                INSERT INTO %s %s VALUES %s
                </script>
                """;

        String table = tableInfo.getTableName();

        String column = tableInfo.getFieldList().stream()
                .map(TableFieldInfo::getInsertSqlColumn)
                .filter(Objects::nonNull)
                .collect(joining(NEWLINE));
        String columnScript = SqlScriptUtils.convertTrim(column, LEFT_BRACKET, RIGHT_BRACKET, null, COMMA);

        String value = tableInfo.getFieldList().stream()
                .map(i -> i.getInsertSqlProperty(ENTITY + DOT))
                .filter(Objects::nonNull)
                .collect(joining(NEWLINE));
        String valueTrim = SqlScriptUtils.convertTrim(value, LEFT_BRACKET, RIGHT_BRACKET, null, COMMA);
        String valuesScript = SqlScriptUtils.convertForeach(valueTrim, COLL, null, ENTITY, COMMA);

        SqlSource sqlSource = super.createSqlSource(configuration, sql.formatted(table, columnScript, valuesScript), modelClass);

        // 第三个参数必须和baseMapper的自定义方法名一致
        return this.addInsertMappedStatement(mapperClass, modelClass, this.methodName, sqlSource, NoKeyGenerator.INSTANCE, null, null);
    }
}