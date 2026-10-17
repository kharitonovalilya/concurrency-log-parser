package statistics;

import model.LogEntry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Statistics implements StatisticsCollector {
    private final static int ERROR_STATUS_THRESHOLD = 500;

    private long totalRequests;
    private long errorRequests;
    private long totalResponseTime;
    private long maxResponseTime;

    private final Map<Integer, Long> statusCounts = new HashMap<>();
    private final Map<String, Long> pathCounts = new HashMap<>();

    @Override
    public void update(LogEntry log) {
        totalRequests++;
        if (log.statusCode() >= ERROR_STATUS_THRESHOLD) {
            errorRequests++;
        }
        totalResponseTime += log.responseTimeMs();
        maxResponseTime = Math.max(maxResponseTime, log.responseTimeMs());

        statusCounts.merge(log.statusCode(), 1L, Long::sum);
        pathCounts.merge(log.path(), 1L, Long::sum);
    }

    @Override
    public long getTotalRequests() {
        return totalRequests;
    }

    @Override
    public long getErrorRequests() {
        return errorRequests;
    }

    @Override
    public long getTotalResponseTime() {
        return totalResponseTime;
    }

    @Override
    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    public double getAverageResponseTime() {
        if (totalRequests == 0) {
            return 0;
        }
        return (double) totalResponseTime / totalRequests;
    }

    public Map<Integer, Long> getStatusCounts() {
        return Collections.unmodifiableMap(statusCounts);
    }

    public Map<String, Long> getPathCounts() {
        return Collections.unmodifiableMap(pathCounts);
    }
}
