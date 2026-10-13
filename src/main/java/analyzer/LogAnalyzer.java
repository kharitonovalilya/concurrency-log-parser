package analyzer;

import statistics.Statistics;
import statistics.StatisticsCollector;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public interface LogAnalyzer {
    StatisticsCollector analyze(Path path) throws IOException, InterruptedException, ExecutionException;
}
