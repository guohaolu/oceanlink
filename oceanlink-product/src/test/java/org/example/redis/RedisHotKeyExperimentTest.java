package org.example.redis;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Hot key experiment harness for comparing Redis behavior under different key distributions.
 *
 * <p>Scenarios:
 * A: 100% traffic hits the same hot key
 * B: 95% traffic hits the hot key, 5% traffic hits uniform keys
 * C: 100% traffic hits uniform keys
 *
 * <p>Enable and run manually against the local Sentinel cluster:
 * mvn -pl oceanlink-product -Dtest=RedisHotKeyExperimentTest test
 */
@Disabled("Manual experiment only. Requires a local Redis Sentinel cluster.")
public class RedisHotKeyExperimentTest {

    private static final String MASTER_NAME = System.getProperty("redis.masterName", "mymaster");
    private static final String SENTINELS = System.getProperty(
            "redis.sentinels",
            "redis://127.0.0.1:26379,redis://127.0.0.1:26380,redis://127.0.0.1:26381"
    );
    private static final String KEY_PREFIX = System.getProperty("redis.keyPrefix", "hotkey:experiment:");
    private static final String HOT_KEY = KEY_PREFIX + "hot";
    private static final String VALUE = System.getProperty("redis.value", "payload");
    private static final int UNIFORM_KEY_COUNT = Integer.getInteger("redis.uniformKeyCount", 10_000);
    private static final int THREADS = Integer.getInteger("redis.threads", 128);
    private static final int TOTAL_REQUESTS = Integer.getInteger("redis.totalRequests", 100_000);
    private static final double QPS = Double.parseDouble(System.getProperty("redis.qps", "30000"));
    private static final int WARMUP_REQUESTS = Integer.getInteger("redis.warmupRequests", 5_000);
    private static final String SCENARIO_FILTER = System.getProperty("redis.scenario", "ALL");

    /**
     * 手动执行实验入口，按过滤条件依次运行各个场景。
     */
    public static void main(String[] args) throws Exception {
        RedisHotKeyExperimentTest experiment = new RedisHotKeyExperimentTest();
        for (Scenario scenario : Scenario.values()) {
            if (scenario.matches(SCENARIO_FILTER)) {
                experiment.runScenario(scenario);
            }
        }
    }

    /**
     * 场景 A：全部流量打到同一个热点 key。
     * <p>
     * Scenario A (100% hot key)
     * requests=100000 success=100000 failure=0 elapsed=10649ms actualQps=9390.43
     * latencyMs[min=0,p50=9,p95=16,p99=31,max=153]
     */
    @Test
    void scenarioA_allTrafficOnHotKey() throws Exception {
        runScenario(Scenario.ALL_HOT_KEY);
    }

    /**
     * 场景 B：绝大多数流量命中热点 key，少量流量分散到普通 key。
     * <p>
     * Scenario B (95% hot key + 5% uniform keys)
     * requests=100000 success=100000 failure=0 elapsed=40113ms actualQps=2492.90
     * latencyMs[min=0,p50=2,p95=4,p99=7,max=66]
     */
    @Test
    void scenarioB_mostTrafficOnHotKey() throws Exception {
        runScenario(Scenario.MOSTLY_HOT_KEY);
    }

    /**
     * 场景 C：全部流量均匀分布到普通 key。
     * <p>
     * Scenario C (100% uniform keys)
     * requests=100000 success=100000 failure=0 elapsed=10213ms actualQps=9791.12
     * latencyMs[min=0,p50=9,p95=16,p99=20,max=83]
     */
    @Test
    void scenarioC_allTrafficOnUniformKeys() throws Exception {
        runScenario(Scenario.ALL_UNIFORM_KEYS);
    }

    /**
     * 执行单个实验场景，包含建连、准备数据、预热和正式压测。
     */
    private void runScenario(Scenario scenario) throws Exception {
        RedissonClient client = createClient();
        prepareData(client);
        warmup(client, scenario);
        ExperimentResult result = execute(client, scenario);
        System.out.println(result.format());
    }

    /**
     * 创建指向本地 Sentinel 集群的 Redisson 客户端。
     */
    private RedissonClient createClient() {
        Config config = new Config();
        config.useSentinelServers()
                .setMasterName(MASTER_NAME)
                .addSentinelAddress(parseSentinelAddresses())
                .setPassword("lgh@LGH123")
                .setCheckSentinelsList(false);
        return Redisson.create(config);
    }

    /**
     * 解析 Sentinel 地址列表，转换为 Redisson 所需的数组格式。
     */
    private String[] parseSentinelAddresses() {
        return Arrays.stream(SENTINELS.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * 预先写入热点 key 和均匀分布 key，避免压测阶段掺杂初始化开销。
     */
    private void prepareData(RedissonClient client) {
        client.getBucket(HOT_KEY).set(VALUE);
        for (int i = 0; i < UNIFORM_KEY_COUNT; i++) {
            client.getBucket(uniformKey(i)).set(VALUE);
        }
    }

    /**
     * 通过少量读取请求预热连接与 Redis 相关缓存。
     */
    private void warmup(RedissonClient client, Scenario scenario) {
        for (int i = 0; i < WARMUP_REQUESTS; i++) {
            client.getBucket(scenario.pickKey()).get();
        }
    }

    /**
     * 并发执行正式压测，并统计吞吐与延迟数据。
     */
    private ExperimentResult execute(RedissonClient client, Scenario scenario) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        RateLimiter rateLimiter = createRateLimiter();
        LongAdder success = new LongAdder();
        LongAdder failure = new LongAdder();
        ConcurrentLinkedQueue<Long> latenciesNanos = new ConcurrentLinkedQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        int requestsPerThread = TOTAL_REQUESTS / THREADS;
        int remainder = TOTAL_REQUESTS % THREADS;

        long startedAt = System.nanoTime();
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int threadIndex = 0; threadIndex < THREADS; threadIndex++) {
                int taskRequests = requestsPerThread + (threadIndex < remainder ? 1 : 0);
                futures.add(executor.submit(new Worker(
                        client,
                        scenario,
                        rateLimiter,
                        latenciesNanos,
                        success,
                        failure,
                        startLatch,
                        taskRequests
                )));
            }

            startLatch.countDown();
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
        long finishedAt = System.nanoTime();
        return ExperimentResult.of(scenario, success.sum(), failure.sum(), latenciesNanos, startedAt, finishedAt);
    }

    /**
     * 基于目标 QPS 创建限流器，每 100ms 发 1000 个牌
     */
    private RateLimiter createRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(100))
                .limitForPeriod(3000)
                .timeoutDuration(Duration.ofSeconds(1))
                .build();
        return RateLimiter.of("redis-hot-key-experiment", config);
    }

    /**
     * 生成普通 key 名称。
     */
    private String uniformKey(int index) {
        return KEY_PREFIX + "uniform:" + index;
    }

    private enum Scenario {
        ALL_HOT_KEY("A", "100% hot key") {
            @Override
            String pickKey() {
                return HOT_KEY;
            }
        },
        MOSTLY_HOT_KEY("B", "95% hot key + 5% uniform keys") {
            @Override
            String pickKey() {
                if (ThreadLocalRandom.current().nextInt(100) < 95) {
                    return HOT_KEY;
                }
                return HOT_KEY_PREFIX.pickUniformKey();
            }
        },
        ALL_UNIFORM_KEYS("C", "100% uniform keys") {
            @Override
            String pickKey() {
                return HOT_KEY_PREFIX.pickUniformKey();
            }
        };

        private static final UniformKeyPicker HOT_KEY_PREFIX = new UniformKeyPicker();
        private final String code;
        private final String description;

        Scenario(String code, String description) {
            this.code = code;
            this.description = description;
        }

        /**
         * 按场景规则选择本次请求要访问的 key。
         */
        abstract String pickKey();

        /**
         * 判断当前场景是否匹配外部传入的过滤条件。
         */
        boolean matches(String scenarioFilter) {
            return "ALL".equalsIgnoreCase(scenarioFilter)
                    || code.equalsIgnoreCase(scenarioFilter)
                    || name().equalsIgnoreCase(scenarioFilter);
        }
    }

    private static final class UniformKeyPicker {
        /**
         * 随机选择一个普通 key，模拟均匀访问分布。
         */
        String pickUniformKey() {
            return KEY_PREFIX + "uniform:" + ThreadLocalRandom.current().nextInt(UNIFORM_KEY_COUNT);
        }
    }

    private static final class Worker implements Callable<Void> {
        private final RedissonClient client;
        private final Scenario scenario;
        private final RateLimiter rateLimiter;
        private final ConcurrentLinkedQueue<Long> latenciesNanos;
        private final LongAdder success;
        private final LongAdder failure;
        private final CountDownLatch startLatch;
        private final int requests;

        private Worker(
                RedissonClient client,
                Scenario scenario,
                RateLimiter rateLimiter,
                ConcurrentLinkedQueue<Long> latenciesNanos,
                LongAdder success,
                LongAdder failure,
                CountDownLatch startLatch,
                int requests
        ) {
            this.client = client;
            this.scenario = scenario;
            this.rateLimiter = rateLimiter;
            this.latenciesNanos = latenciesNanos;
            this.success = success;
            this.failure = failure;
            this.startLatch = startLatch;
            this.requests = requests;
        }

        /**
         * 等待统一起跑信号后执行请求，并在每次请求前申请限流许可。
         */
        @Override
        public Void call() throws Exception {
            startLatch.await();
            for (int i = 0; i < requests; i++) {
                if (!rateLimiter.acquirePermission()) {
                    failure.increment();
                    continue;
                }
                String key = scenario.pickKey();
                long start = System.nanoTime();
                try {
                    RBucket<String> bucket = client.getBucket(key);
                    bucket.get();
                    success.increment();
                } catch (Exception exception) {
                    failure.increment();
                } finally {
                    latenciesNanos.add(System.nanoTime() - start);
                }
            }
            return null;
        }
    }

    private static final class ExperimentResult {
        private final Scenario scenario;
        private final long success;
        private final long failure;
        private final Duration elapsed;
        private final double actualQps;
        private final long minMillis;
        private final long p50Millis;
        private final long p95Millis;
        private final long p99Millis;
        private final long maxMillis;

        private ExperimentResult(
                Scenario scenario,
                long success,
                long failure,
                Duration elapsed,
                double actualQps,
                long minMillis,
                long p50Millis,
                long p95Millis,
                long p99Millis,
                long maxMillis
        ) {
            this.scenario = scenario;
            this.success = success;
            this.failure = failure;
            this.elapsed = elapsed;
            this.actualQps = actualQps;
            this.minMillis = minMillis;
            this.p50Millis = p50Millis;
            this.p95Millis = p95Millis;
            this.p99Millis = p99Millis;
            this.maxMillis = maxMillis;
        }

        static ExperimentResult of(
                Scenario scenario,
                long success,
                long failure,
                ConcurrentLinkedQueue<Long> latenciesNanos,
                long startedAt,
                long finishedAt
        ) {
            List<Long> sorted = new ArrayList<>(latenciesNanos);
            sorted.sort(Long::compareTo);

            long min = sorted.isEmpty() ? 0 : nanosToMillis(sorted.get(0));
            long p50 = percentileMillis(sorted, 0.50d);
            long p95 = percentileMillis(sorted, 0.95d);
            long p99 = percentileMillis(sorted, 0.99d);
            long max = sorted.isEmpty() ? 0 : nanosToMillis(sorted.get(sorted.size() - 1));
            long total = success + failure;
            Duration elapsed = Duration.ofNanos(finishedAt - startedAt);
            double actualQps = total == 0 ? 0 : total / (elapsed.toNanos() / 1_000_000_000d);
            return new ExperimentResult(scenario, success, failure, elapsed, actualQps, min, p50, p95, p99, max);
        }

        /**
         * 将实验结果格式化为便于控制台查看的摘要文本。
         */
        String format() {
            return String.format(
                    Locale.ROOT,
                    "Scenario %s (%s)%nrequests=%d success=%d failure=%d elapsed=%dms actualQps=%.2f%nlatencyMs[min=%d,p50=%d,p95=%d,p99=%d,max=%d]%n",
                    scenario.code,
                    scenario.description,
                    success + failure,
                    success,
                    failure,
                    elapsed.toMillis(),
                    actualQps,
                    minMillis,
                    p50Millis,
                    p95Millis,
                    p99Millis,
                    maxMillis
            );
        }

        /**
         * 计算指定百分位的延迟，并转换为毫秒。
         */
        private static long percentileMillis(List<Long> values, double percentile) {
            if (values.isEmpty()) {
                return 0;
            }
            int index = (int) Math.ceil(percentile * values.size()) - 1;
            index = Math.max(0, Math.min(index, values.size() - 1));
            return nanosToMillis(values.get(index));
        }

        /**
         * 将纳秒延迟转换为毫秒。
         */
        private static long nanosToMillis(long nanos) {
            return TimeUnit.NANOSECONDS.toMillis(nanos);
        }
    }
}
