package analyzer;

import model.LogEntry;
import parser.LogParser;
import statistics.Statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LogAnalyzer {

    private final LogParser logParser;

    public LogAnalyzer(LogParser logParser) {
        this.logParser = logParser;
    }

    public Statistics analyze(Path path) throws IOException {
        Statistics statistics = new Statistics();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry logEntry = logParser.parse(line);
                statistics.update(logEntry);
            }
        }
        return statistics;
    }
}
