package analyzer;

import model.LogEntry;
import parser.LogParser;
import statistics.StatisticsCollector;
import statistics.ThreadSafeStatisticsCollector;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ConcurrentLogAnalyzer implements LogAnalyzer {

    private final LogParser logParser;
    private final int workerCount;
    private final int queueCapacity;

    private final Supplier<? extends ThreadSafeStatisticsCollector> statisticsFactory;

    private record QueueItem(String line, boolean flag) {
        static QueueItem log(String line) {
            return new QueueItem(line, false);
        }
        static QueueItem stop() {
            return new QueueItem(null, true);
        }
    }

    public ConcurrentLogAnalyzer(LogParser logParser, int workerCount, int queueCapacity, Supplier<? extends ThreadSafeStatisticsCollector> statisticsFactory) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException(
                    "workerCount must be positive"
            );
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException(
                    "queueCapacity must be positive"
            );
        }
        this.logParser = logParser;
        this.workerCount = workerCount;
        this.queueCapacity = queueCapacity;
        this.statisticsFactory = statisticsFactory;
    }

    private void processLogs(BlockingQueue<QueueItem> logQueue, StatisticsCollector statistics) throws InterruptedException{
        while (true) {
            QueueItem queueItem = logQueue.take();
            if (queueItem.flag()) {
                break;
            }
            LogEntry log = logParser.parse(queueItem.line());
            statistics.update(log);
        }
    }

    private void putOrFail(BlockingQueue<QueueItem> queue, QueueItem item, AtomicReference<Throwable> failure) throws InterruptedException, ExecutionException {
        while (true) {
            if (failure.get() != null) {
                throw new ExecutionException(failure.get());
            }
            if (queue.offer(item, 100, TimeUnit.MILLISECONDS)) {
                return;
            }
        }
    }

    @Override
    public StatisticsCollector analyze(Path path) throws IOException, InterruptedException, ExecutionException {
        BlockingQueue<QueueItem> logQueue = new ArrayBlockingQueue<>(queueCapacity);
        StatisticsCollector statistics = statisticsFactory.get();
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        boolean completedSuccessfully = false;
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < workerCount; i++) {
                Future<?> future = executor.submit(() -> {
                    try {
                        processLogs(logQueue, statistics);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (RuntimeException | Error e) {
                        failure.compareAndSet(null, e);
                        throw e;
                    }
                });
                futures.add(future);
            }
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    putOrFail(logQueue, QueueItem.log(line), failure);
                }
            }
            for (int i = 0; i < workerCount; i++) {
                putOrFail(logQueue, QueueItem.stop(), failure);
            }
            for (Future<?> future : futures) {
                future.get();
            }
            completedSuccessfully = true;
        } catch (InterruptedException e) {
            if (failure.get() != null) {
                throw new ExecutionException(failure.get());
            }
            throw e;
        }
        finally {
            if (completedSuccessfully) {
                executor.shutdown();
            } else {
                executor.shutdownNow();
            }
        }
        return statistics;
    }
}
