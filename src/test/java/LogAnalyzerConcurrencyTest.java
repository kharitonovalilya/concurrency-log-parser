import analyzer.ConcurrentLogAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import parser.LogParser;
import statistics.ConcurrentStatistics;
import statistics.StatisticsCollector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class LogAnalyzerConcurrencyTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldProcessAllLogsWithMultipleWorkers()
            throws Exception {

        int logsCount = 10_000;

        Path logFile = tempDir.resolve("logs.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(logFile)) {
            for (int i = 0; i < logsCount; i++) {
                writer.write(
                        "2026-10-10T10:15:31 GET /users 200 50"
                );
                writer.newLine();
            }
        }

        LogParser parser = new LogParser();

        ConcurrentLogAnalyzer analyzer =
                new ConcurrentLogAnalyzer(parser, 10, 100, ConcurrentStatistics::new);

        StatisticsCollector statistics = analyzer.analyze(logFile);

        assertEquals(
                logsCount,
                statistics.getTotalRequests()
        );

        assertEquals(
                logsCount,
                statistics.getStatusCounts().get(200)
        );

        assertEquals(
                logsCount,
                statistics.getPathCounts().get("/users")
        );

        assertEquals(
                logsCount * 50L,
                statistics.getTotalResponseTime()
        );

        assertEquals(
                50,
                statistics.getMaxResponseTime()
        );
    }

    @Test
    @Timeout(2)
    void shouldFailFastWhenWorkerCannotParseLog() throws IOException {
        Path logFile = tempDir.resolve("invalid-logs.txt");
        Files.write(
                logFile,
                List.of(
                        "2026-10-10T10:15:31 GET /users 200 53",
                        "2026-10-10T10:15:32 GET /orders 200 80",
                        "this-is-not-a-log",
                        "2026-10-10T10:15:34 GET /products 200 30"
                )
        );
        ConcurrentLogAnalyzer analyzer =
                new ConcurrentLogAnalyzer(
                        new LogParser(),
                        4,
                        2,
                        ConcurrentStatistics::new
                );
        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> analyzer.analyze(logFile)
        );

        assertInstanceOf(
                IllegalArgumentException.class,
                exception.getCause()
        );
    }

    @Test
    @Timeout(2)
    void workerFailureShouldNotLeakInterruptFlagToCaller() throws Exception {
        Path logFile = tempDir.resolve("invalid-logs.txt");

        Files.write(
                logFile,
                List.of(
                        "2026-10-10T10:15:31 GET /users 200 53",
                        "invalid log line",
                        "2026-10-10T10:15:32 GET /orders 200 80"
                )
        );

        ConcurrentLogAnalyzer analyzer =
                new ConcurrentLogAnalyzer(
                        new LogParser(),
                        1,
                        1,
                        ConcurrentStatistics::new
                );

        assertFalse(Thread.currentThread().isInterrupted());

        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> analyzer.analyze(logFile)
        );

        assertInstanceOf(
                IllegalArgumentException.class,
                exception.getCause()
        );

        assertFalse(Thread.currentThread().isInterrupted());
    }
}
