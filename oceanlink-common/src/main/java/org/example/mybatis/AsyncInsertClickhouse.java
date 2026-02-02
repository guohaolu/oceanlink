package org.example.mybatis;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Clickhouse异步插入, 暂时不可用
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
        // 1. 定义 SQL 模板，使用 /*mp:ignore*/ 绕过 MP 拦截器的语法检查
        String sql = """
                <script>
                /*mp:ignore*/
                INSERT INTO %s %s SETTINGS async_insert=1, wait_for_async_insert=1 VALUES %s
                </script>
                """;

        String table = tableInfo.getTableName();

        // 2. 直接获取 MP 封装好的所有插入列（会自动包含 @TableId 字段和普通的 @TableField 字段）
        // false 表示最后不带逗号
        String allInsertColumns = tableInfo.getFieldList().stream()
                .map(TableFieldInfo::getInsertSqlColumn)
                .filter(Objects::nonNull)
                .collect(joining(NEWLINE));
        String allInsertProperties = tableInfo.getFieldList().stream()
                .map(i -> i.getInsertSqlProperty(ENTITY + DOT))
                .filter(Objects::nonNull)
                .collect(joining(NEWLINE));

        // 3. 封装为括号形式，如 (col1, col2, col3)
        String columnScript = SqlScriptUtils.convertTrim(allInsertColumns, LEFT_BRACKET, RIGHT_BRACKET, null, COMMA);
        String valueTrim = SqlScriptUtils.convertTrim(allInsertProperties, LEFT_BRACKET, RIGHT_BRACKET, null, COMMA);

        // 4. 构造批量插入的 foreach 循环
        // 最终生成：VALUES (item1.id, item1.name), (item2.id, item2.name)
        String valuesScript = SqlScriptUtils.convertForeach(valueTrim, COLL, null, ENTITY, COMMA);

        SqlSource sqlSource = super.createSqlSource(configuration, sql.formatted(table, columnScript, valuesScript), modelClass);

        return this.addInsertMappedStatement(mapperClass, modelClass, this.methodName, sqlSource, NoKeyGenerator.INSTANCE, null, null);
    }
}