package statistics;

import model.LogEntry;

import java.util.Map;

public interface StatisticsCollector {
    void update(LogEntry log);

    long getTotalRequests();

    long getErrorRequests();

    long getTotalResponseTime();

    long getMaxResponseTime();

    double getAverageResponseTime();

    Map<Integer, Integer> getStatusCounts();

    Map<String, Integer> getPathCounts();
}
