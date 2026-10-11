package statistics;

import lombok.Getter;
import model.LogEntry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
public class Statistics {
    private final static int ERROR_STATUS_THRESHOLD = 500;

    private long totalRequests;
    private long errorRequests;
    private long totalResponseTime;
    private long maxResponseTime;

    private final Map<Integer, Integer> statusCounts = new HashMap<>();
    private final Map<String, Integer> pathCounts = new HashMap<>();

    public synchronized void update(LogEntry log) {
        totalRequests++;
        if (log.statusCode() >= ERROR_STATUS_THRESHOLD) {
            errorRequests++;
        }
        totalResponseTime += log.responseTimeMs();
        maxResponseTime = Math.max(maxResponseTime, log.responseTimeMs());

        statusCounts.merge(log.statusCode(), 1, Integer::sum);
        pathCounts.merge(log.path(), 1, Integer::sum);
    }

    public double getAverageResponseTime() {
        if (totalRequests == 0) {
            return 0;
        }
        return (double) totalResponseTime / totalRequests;
    }

    public Map<Integer, Integer> getStatusCounts() {
        return Collections.unmodifiableMap(statusCounts);
    }

    public Map<String, Integer> getPathCounts() {
        return Collections.unmodifiableMap(pathCounts);
    }
}
