package benchmark;

import config.HttpMethod;
import model.LogEntry;
import statistics.ConcurrentStatistics;
import statistics.Statistics;
import statistics.StatisticsCollector;
import statistics.SynchronizedStatistics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

// TODO: use JMH for a more precise benchmark
public class StatisticsBenchmark {

    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 5;
    private static final int LOG_COUNT = 1_000_000;

    private record BenchmarkCase(
            String name,
            Supplier<StatisticsCollector> statisticsFactory,
            int workerCount
    ) {
    }

    static void main(String[] args) throws Exception {
        List<LogEntry> logs = generateLogs(LOG_COUNT);

        List<BenchmarkCase> cases = List.of(
                new BenchmarkCase(
                        "Synchronized, 1 worker",
                        SynchronizedStatistics::new,
                        1
                ),
                new BenchmarkCase(
                        "Synchronized, 2 workers",
                        SynchronizedStatistics::new,
                        2
                ),
                new BenchmarkCase(
                        "Synchronized, 4 workers",
                        SynchronizedStatistics::new,
                        4
                ),
                new BenchmarkCase(
                        "Synchronized, 8 workers",
                        SynchronizedStatistics::new,
                        8
                ),

                new BenchmarkCase(
                        "Concurrent structures, 1 worker",
                        ConcurrentStatistics::new,
                        1
                ),
                new BenchmarkCase(
                        "Concurrent structures, 2 workers",
                        ConcurrentStatistics::new,
                        2
                ),
                new BenchmarkCase(
                        "Concurrent structures, 4 workers",
                        ConcurrentStatistics::new,
                        4
                ),
                new BenchmarkCase(
                        "Concurrent structures, 8 workers",
                        ConcurrentStatistics::new,
                        8
                )
        );

//        for (BenchmarkCase benchmarkCase : cases) {
//            double averageTime = benchmarkStatistics(
//                    benchmarkCase.statisticsFactory(),
//                    logs,
//                    benchmarkCase.workerCount()
//            );
//            System.out.printf(
//                    "%-35s %.2f ms%n",
//                    benchmarkCase.name(),
//                    averageTime
//            );
//        }
        double averageTime = benchmarkStatistics(Statistics::new, logs, 1);
        System.out.printf(
                "%-35s %.2f ms%n",
                "Single-thread benchmark",
                averageTime
        );
    }

    private static List<LogEntry> generateLogs(int count) {
        List<LogEntry> logs = new ArrayList<>(count);
        LocalDateTime timestamp =
                LocalDateTime.of(2026, 10, 10, 10, 0);
        for (int i = 0; i < count; i++) {
            logs.add(new LogEntry(
                    timestamp,
                    HttpMethod.GET,
                    switch (i % 5) {
                        case 0 -> "/users";
                        case 1 -> "/orders";
                        case 2 -> "/friends";
                        case 3 -> "/favorites";
                        default -> "/products";
                    },
                    i % 10 == 0 ? 500 : 200,
                    50
            ));
        }
        return logs;
    }

    private static long runDirect(
            Supplier<StatisticsCollector> statisticsFactory,
            List<LogEntry> logs
    ) {
        StatisticsCollector statistics =
                statisticsFactory.get();
        long start = System.nanoTime();
        for (LogEntry log : logs) {
            statistics.update(log);
        }
        long end = System.nanoTime();
        return end - start;
    }

    private static double benchmarkStatistics(
            Supplier<StatisticsCollector> statisticsFactory,
            List<LogEntry> logs,
            int workerCount
    ) throws Exception {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            runDirect(statisticsFactory, logs);
        }
        long totalTime = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            totalTime += runOnce(
                    statisticsFactory,
                    logs,
                    workerCount
            );
        }
        return totalTime
                / (double) MEASURED_RUNS
                / 1_000_000;
    }

    private static long runOnce(
            Supplier<StatisticsCollector> statisticsFactory,
            List<LogEntry> logs,
            int workerCount
    ) throws Exception {
        StatisticsCollector statistics =
                statisticsFactory.get();
        ExecutorService executor =
                Executors.newFixedThreadPool(workerCount);
        try {
            int chunkSize = logs.size() / workerCount;
            List<Future<?>> futures = new ArrayList<>();
            CountDownLatch runLatch = new CountDownLatch(workerCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            for (int worker = 0; worker < workerCount; worker++) {
                int from = worker * chunkSize;
                int to = worker == workerCount - 1
                        ? logs.size()
                        : from + chunkSize;
                futures.add(executor.submit(() -> {
                    runLatch.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = from; i < to; i++) {
                        statistics.update(logs.get(i));
                    }
                }));
            }
            runLatch.await();

            long start = System.nanoTime();

            startLatch.countDown();
            for (Future<?> future : futures) {
                future.get();
            }

            long end = System.nanoTime();

            if (statistics.getTotalRequests() != logs.size()) {
                throw new IllegalStateException(
                        "Expected " + logs.size()
                                + " requests, but got "
                                + statistics.getTotalRequests()
                );
            }

            return end - start;
        } finally {
            executor.shutdown();
        }
    }
}