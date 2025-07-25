package org.example.mybatis;


import com.clickhouse.data.Tuple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Clickhouse的{@code Array(Tuple(String, Nullable(Date)))}类型转换器
 *
 * @author guohao.lu
 */
@MappedTypes({List.class})
@MappedJdbcTypes({JdbcType.ARRAY})
public class ClickhousePairListTypeHandler extends BaseTypeHandler<List<Pair<String, LocalDate>>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    List<Pair<String, LocalDate>> parameter, JdbcType jdbcType) throws SQLException {
        // 将 List<Pair<String, LocalDate>> 转换成 ClickHouse 的 Array(Tuple(String, Nullable(Date)))
        Tuple[] array = parameter.stream()
                .map(pair -> new Tuple(pair.getKey(), pair.getValue()))
                .toArray(Tuple[]::new);

        ps.setArray(i, ps.getConnection().createArrayOf("Tuple", array));
    }

    @Override
    public List<Pair<String, LocalDate>> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseArray(rs.getArray(columnName));
    }

    @Override
    public List<Pair<String, LocalDate>> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseArray(rs.getArray(columnIndex));
    }

    @Override
    public List<Pair<String, LocalDate>> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseArray(cs.getArray(columnIndex));
    }

    private List<Pair<String, LocalDate>> parseArray(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return null;
        }
        Object[] array = (Object[]) sqlArray.getArray();
        List<Pair<String, LocalDate>> result = new ArrayList<>();
        for (Object item : array) {
            Object[] tuple = (Object[]) item;
            String strVal = (String) tuple[0];
            LocalDate dateVal = tuple[1] == null ? null : ((Date) tuple[1]).toLocalDate();
            result.add(Pair.of(strVal, dateVal));
        }
        return result;
    }
}