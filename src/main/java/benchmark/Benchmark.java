package benchmark;

import analyzer.ConcurrentLogAnalyzer;
import analyzer.LogAnalyzer;
import analyzer.SingleThreadLogAnalyzer;
import parser.LogParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Benchmark {

    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 5;
    private static final int LOG_COUNT = 1_000_000;

    private record BenchmarkCase(
            String name,
            LogAnalyzer analyzer
    ) {}

    static void main(String[] args) throws Exception {
        Path path = Path.of("benchmark-logs.txt");
        LogGenerator.generate(path, LOG_COUNT);
        List<BenchmarkCase> cases = List.of(
                new BenchmarkCase(
                        "Sequential",
                        new SingleThreadLogAnalyzer(new LogParser())
                ),
                new BenchmarkCase(
                        "Concurrent, 2 workers",
                        new ConcurrentLogAnalyzer(new LogParser(), 2, 100)
                ),
                new BenchmarkCase(
                        "Concurrent, 4 workers",
                        new ConcurrentLogAnalyzer(new LogParser(), 4, 100)
                ),
                new BenchmarkCase(
                        "Concurrent, 8 workers",
                        new ConcurrentLogAnalyzer(new LogParser(), 8, 100)
                )
        );
        for (BenchmarkCase benchmarkCase : cases) {
            double averageTime = benchmark(benchmarkCase.analyzer(), path);
            System.out.printf(
                    "%-25s %.2f ms%n",
                    benchmarkCase.name(),
                    averageTime
            );
        }
    }

    private static double benchmark(
            LogAnalyzer analyzer,
            Path path
    ) throws IOException, ExecutionException, InterruptedException {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            analyzer.analyze(path);
        }
        long totalTime = 0;
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            analyzer.analyze(path);
            long end = System.nanoTime();
            totalTime += end - start;
        }
        return totalTime / (double) MEASURED_RUNS / 1_000_000;
    }
}