package app;

import analyzer.ConcurrentLogAnalyzer;
import analyzer.LogAnalyzer;
import analyzer.SingleThreadLogAnalyzer;
import parser.LogParser;
import statistics.ConcurrentStatistics;
import statistics.StatisticsCollector;
import statistics.SynchronizedStatistics;
import statistics.ThreadSafeStatisticsCollector;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class Main {

    private final static int DEFAULT_QUEUE_CAPACITY = 1000;
    private final static int DEFAULT_WORKER_COUNT = 4;

    static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        if (args.length < 2) {
            printUsage();
            return;
        }
        Path path = Path.of(args[0]);
        String mode = args[1].toLowerCase();

        LogParser logParser = new LogParser();

        LogAnalyzer logAnalyzer = switch (mode) {
            case "sequential" -> new SingleThreadLogAnalyzer(logParser);
            case "synchronized" -> createConcurrentAnalyzer(args, logParser, SynchronizedStatistics::new);
            case "concurrent" -> createConcurrentAnalyzer(args, logParser, ConcurrentStatistics::new);
            default -> {
                System.out.println("Unknown mode: " + mode);
                printUsage();
                yield null;
            }
        };
        if (logAnalyzer == null) {
            return;
        }

        StatisticsCollector statistics = logAnalyzer.analyze(path);
        printStatistics(statistics);
    }

    private static void printStatistics(StatisticsCollector statistics) {
        System.out.println("Total requests: " + statistics.getTotalRequests());
        System.out.println("Errors: " + statistics.getErrorRequests());
        System.out.println("Average response time: " + statistics.getAverageResponseTime());
        System.out.println("Max response time: " + statistics.getMaxResponseTime());
        System.out.println("Status counts: " + statistics.getStatusCounts());
        System.out.println("Path counts: " + statistics.getPathCounts());
    }

    private static int getIntArgument(String[] args, int index, int defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        int value = Integer.parseInt(args[index]);
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Argument must be positive: " + value
            );
        }
        return value;
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  <log-file> sequential
                  <log-file> synchronized [workers] [queue-capacity]
                  <log-file> concurrent [workers] [queue-capacity]

                Defaults:
                  workers = 4
                  queue-capacity = 1000
                """);
    }

    private static LogAnalyzer createConcurrentAnalyzer(String[] args, LogParser logParser, Supplier<? extends ThreadSafeStatisticsCollector> supplier) {
        int workerCount = getIntArgument(
                args,
                2,
                DEFAULT_WORKER_COUNT
        );
        int queueCapacity = getIntArgument(
                args,
                3,
                DEFAULT_QUEUE_CAPACITY
        );
        return new ConcurrentLogAnalyzer(logParser, workerCount, queueCapacity, supplier);
    }
}
