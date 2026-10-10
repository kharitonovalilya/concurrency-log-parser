package app;

import analyzer.LogAnalyzer;
import parser.LogParser;
import statistics.Statistics;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    static void main() throws IOException {
        LogParser logParser = new LogParser();
        LogAnalyzer logAnalyzer = new LogAnalyzer(logParser);

        Statistics statistics = logAnalyzer.analyze(Path.of("logs.txt"));

        System.out.println("Total requests: " + statistics.getTotalRequests());
        System.out.println("Errors: " + statistics.getErrorRequests());
        System.out.println("Average response time: " + statistics.getAverageResponseTime());
        System.out.println("Max response time: " + statistics.getMaxResponseTime());
        System.out.println("Status counts: " + statistics.getStatusCounts());
        System.out.println("Path counts: " + statistics.getPathCounts());
    }
}
