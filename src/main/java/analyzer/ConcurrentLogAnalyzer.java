package analyzer;

import model.LogEntry;
import parser.LogParser;
import statistics.ConcurrentStatistics;
import statistics.Statistics;
import statistics.StatisticsCollector;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class ConcurrentLogAnalyzer implements LogAnalyzer {

    private final LogParser logParser;
    private final int workerCount;
    private static final String STOP = "STOP";
    private final int queueCapacity;

    private final Supplier<StatisticsCollector> statisticsFactory;

    public ConcurrentLogAnalyzer(LogParser logParser, int workerCount, int queueCapacity, Supplier<StatisticsCollector> statisticsFactory) {
        this.logParser = logParser;
        this.workerCount = workerCount;
        this.queueCapacity = queueCapacity;
        this.statisticsFactory = statisticsFactory;
    }

    private void processLogs(BlockingQueue<String> logQueue, StatisticsCollector statistics) throws InterruptedException{
        while (true) {
            String line = logQueue.take();
            if (line.equals(STOP)) {
                break;
            }
            LogEntry log = logParser.parse(line);
            statistics.update(log);
        }
    }

    @Override
    public StatisticsCollector analyze(Path path) throws IOException, InterruptedException, ExecutionException {
        BlockingQueue<String> logQueue = new ArrayBlockingQueue<>(queueCapacity);
        StatisticsCollector statistics = statisticsFactory.get();
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < workerCount; i++) {
                Future<?> future = executor.submit(() -> {
                    try {
                        processLogs(logQueue, statistics);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        throw e;
                    }
                });
                futures.add(future);
            }
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logQueue.put(line);
                }
            }
            for (int i = 0; i < workerCount; i++) {
                logQueue.put(STOP);
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }
        return statistics;
    }
}
