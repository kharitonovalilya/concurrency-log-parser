import analyzer.ConcurrentLogAnalyzer;
import analyzer.SingleThreadLogAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import parser.LogParser;
import statistics.Statistics;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                new ConcurrentLogAnalyzer(parser, 10, 100);

        Statistics statistics = analyzer.analyze(logFile);

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
}
