package org.example.repository;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;

import java.math.BigDecimal;

/**
 * @author guohao.lu
 */
public class TableTest {
    public static void main(String[] args) {
        Table<Pair<Long/*whid*/, Long/*skuid*/>, Triple<String, String, Long>, BigDecimal> table = HashBasedTable.create();
        table.put(Pair.create(1L, 1L), Triple.of("1", "1", 1L), BigDecimal.ONE);

        System.out.println(table.contains(Pair.create(1L, 1L), Triple.of("1", "1", 1L)));
    }
}
