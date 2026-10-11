package analyzer;

import model.LogEntry;
import parser.LogParser;
import statistics.Statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ConcurrentLogAnalyzer implements LogAnalyzer {

    private final LogParser logParser;
    private final int workerCount;
    private static final String STOP = "STOP";
    private final int queueCapacity;

    public ConcurrentLogAnalyzer(LogParser logParser, int workerCount, int queueCapacity) {
        this.logParser = logParser;
        this.workerCount = workerCount;
        this.queueCapacity = queueCapacity;
    }

    private void processLogs(BlockingQueue<String> logQueue, Statistics statistics) throws InterruptedException{
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
    public Statistics analyze(Path path) throws IOException, InterruptedException, ExecutionException {
        BlockingQueue<String> logQueue = new ArrayBlockingQueue<>(queueCapacity);
        Statistics statistics = new Statistics();
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < workerCount; i++) {
                Future<?> future = executor.submit(() -> {
                    try {
                        processLogs(logQueue, statistics);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
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
