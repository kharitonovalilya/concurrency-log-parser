package app;

import analyzer.SingleThreadLogAnalyzer;
import parser.LogParser;
import statistics.StatisticsCollector;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    static void main() throws IOException {
        LogParser logParser = new LogParser();
        SingleThreadLogAnalyzer singleThreadLogAnalyzer = new SingleThreadLogAnalyzer(logParser);

        StatisticsCollector statistics = singleThreadLogAnalyzer.analyze(Path.of("logs.txt"));

        System.out.println("Total requests: " + statistics.getTotalRequests());
        System.out.println("Errors: " + statistics.getErrorRequests());
        System.out.println("Average response time: " + statistics.getAverageResponseTime());
        System.out.println("Max response time: " + statistics.getMaxResponseTime());
        System.out.println("Status counts: " + statistics.getStatusCounts());
        System.out.println("Path counts: " + statistics.getPathCounts());
    }
}
