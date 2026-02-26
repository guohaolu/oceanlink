package org.example;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * @author guohao.lu
 */
@State(Scope.Thread)
public class StringInternBenchmark {
    @Benchmark
    public String testIntern() {
        return new String("abc").intern();
    }
}
