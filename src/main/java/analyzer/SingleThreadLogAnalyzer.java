package analyzer;

import model.LogEntry;
import parser.LogParser;
import statistics.Statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SingleThreadLogAnalyzer implements LogAnalyzer {

    private final LogParser logParser;

    public SingleThreadLogAnalyzer(LogParser logParser) {
        this.logParser = logParser;
    }

    @Override
    public Statistics analyze(Path path) throws IOException {
        Statistics statistics = new Statistics();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry log = logParser.parse(line);
                statistics.update(log);
            }
        }
        return statistics;
    }
}
