package analyzer;

import statistics.Statistics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public interface LogAnalyzer {
    Statistics analyze(Path path) throws IOException, InterruptedException, ExecutionException;
}
