package benchmark;

import analyzer.ConcurrentLogAnalyzer;
import analyzer.LogAnalyzer;
import analyzer.SingleThreadLogAnalyzer;
import parser.LogParser;
import statistics.ConcurrentStatistics;
import statistics.StatisticsCollector;
import statistics.SynchronizedStatistics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EndToEndBenchmark {

    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 5;

    private static final int LOG_COUNT = 1_000_000;
    private static final int QUEUE_CAPACITY = 1_000;

    private record BenchmarkCase(
            String name,
            LogAnalyzer analyzer
    ) {
    }

    static void main(String[] args) throws Exception {
        Path logFile = Path.of("benchmark-logs.txt");

        LogGenerator.generate(logFile, LOG_COUNT);

        try {
            List<BenchmarkCase> cases = List.of(
                    new BenchmarkCase(
                            "Sequential",
                            new SingleThreadLogAnalyzer(
                                    new LogParser()
                            )
                    ),

                    new BenchmarkCase(
                            "Synchronized, 1 worker",
                            new ConcurrentLogAnalyzer(
                                    new LogParser(),
                                    1,
                                    QUEUE_CAPACITY,
                                    SynchronizedStatistics::new
                            )
                    ),
                    new BenchmarkCase(
                            "Synchronized, 2 workers",
                            new ConcurrentLogAnalyzer(
                                    new LogParser(),
                                    2,
                                    QUEUE_CAPACITY,
                                    SynchronizedStatistics::new
                            )
                    ),
                    new BenchmarkCase(
                            "Synchronized, 4 workers",
                            new ConcurrentLogAnalyzer(
                                    new LogParser(),
                                    4,
                                    QUEUE_CAPACITY,
                                    SynchronizedStatistics::new
                            )
                    ),
                    new BenchmarkCase(
                            "Synchronized, 8 workers",
                            new ConcurrentLogAnalyzer(
                                    new LogParser(),
                                    8,
                                    QUEUE_CAPACITY,
                                    SynchronizedStatistics::new
                            )
                    ),

                    new BenchmarkCase(
                            "Concurrent structures, 1 worker",
                            new ConcurrentLogAnalyzer(
                                    new LogParser(),
                                    1,
                                    QUEUE_CAPACITY,
                                    ConcurrentStatistics::new
                            )
                    ),
                    new BenchmarkCase(
                            "Concurrent structures, 2 workers",
                            new ConcurrentLogAnalyzer(
                                    new LogParser(),
                                    2,
                                    QUEUE_CAPACITY,
                                    ConcurrentStatistics::new
                            )
                    ),
                    new BenchmarkCase(
                            "Concurrent structures, 4 workers",
                            new ConcurrentLogAnalyzer(
                                    new LogParser(),
                                    4,
                                    QUEUE_CAPACITY,
                                    ConcurrentStatistics::new
                            )
                    ),
                    new BenchmarkCase(
                            "Concurrent structures, 8 workers",
                            new ConcurrentLogAnalyzer(
                                    new LogParser(),
                                    8,
                                    QUEUE_CAPACITY,
                                    ConcurrentStatistics::new
                            )
                    )
            );
            for (BenchmarkCase benchmarkCase : cases) {
                double averageTime = benchmark(benchmarkCase.analyzer(), logFile);
                System.out.printf(
                        "%-35s %.2f ms%n",
                        benchmarkCase.name(),
                        averageTime
                );
            }
        } finally {
            Files.deleteIfExists(logFile);
        }
    }

    private static double benchmark(
            LogAnalyzer analyzer,
            Path path
    ) throws Exception {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            StatisticsCollector statistics = analyzer.analyze(path);
            validateStatistics(statistics);
        }
        long totalTime = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();

            StatisticsCollector statistics =
                    analyzer.analyze(path);

            long end = System.nanoTime();

            validateStatistics(statistics);
            totalTime += end - start;
        }

        return totalTime / (double) MEASURED_RUNS / 1_000_000;
    }

    private static void validateStatistics(StatisticsCollector statistics) {
        if (statistics.getTotalRequests() != LOG_COUNT) {
            throw new IllegalStateException(
                    "Expected " + LOG_COUNT
                            + " requests, but got "
                            + statistics.getTotalRequests()
            );
        }
    }
}