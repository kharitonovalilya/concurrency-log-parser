package statistics;

import lombok.Getter;
import lombok.Synchronized;
import model.LogEntry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SynchronizedStatistics implements ThreadSafeStatisticsCollector {
    private final static int ERROR_STATUS_THRESHOLD = 500;

    private long totalRequests;
    private long errorRequests;
    private long totalResponseTime;
    private long maxResponseTime;

    private final Map<Integer, Long> statusCounts = new HashMap<>();
    private final Map<String, Long> pathCounts = new HashMap<>();

    @Override
    public synchronized void update(LogEntry log) {
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
    public synchronized long getTotalRequests() {
        return totalRequests;
    }

    @Override
    public synchronized long getErrorRequests() {
        return errorRequests;
    }

    @Override
    public synchronized long getTotalResponseTime() {
        return totalResponseTime;
    }

    @Override
    public synchronized long getMaxResponseTime() {
        return maxResponseTime;
    }

    public synchronized double getAverageResponseTime() {
        if (totalRequests == 0) {
            return 0;
        }
        return (double) totalResponseTime / totalRequests;
    }

    public synchronized Map<Integer, Long> getStatusCounts() {
        return Collections.unmodifiableMap(statusCounts);
    }

    public synchronized Map<String, Long> getPathCounts() {
        return Collections.unmodifiableMap(pathCounts);
    }
}
