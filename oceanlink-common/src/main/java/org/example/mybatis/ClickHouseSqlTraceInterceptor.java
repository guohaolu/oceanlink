package org.example.mybatis;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.skywalking.apm.toolkit.trace.SpanRef;
import org.apache.skywalking.apm.toolkit.trace.Tracer;

/**
 * ClickHouse SQL Trace Interceptor
 *
 * @author guohao.lu
 */
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}
        ),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
        )
})
public class ClickHouseSqlTraceInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();

        String operation = ms.getSqlCommandType().name();
        String spanName = "DB/ClickHouse/" + operation;

        SpanRef span = Tracer.createLocalSpan(spanName);
        span.tag("db.type", "clickhouse");
        span.tag("db.statement", sql);

        try {
            return invocation.proceed();
        } catch (Throwable t) {
            span.log(t);
            throw t;
        } finally {
            Tracer.stopSpan();
        }
    }
}

