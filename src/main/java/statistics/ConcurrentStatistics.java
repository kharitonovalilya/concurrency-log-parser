package statistics;

import model.LogEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class ConcurrentStatistics implements ThreadSafeStatisticsCollector  {
    private final static int ERROR_STATUS_THRESHOLD = 500;

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder errorRequests = new LongAdder();
    private final LongAdder totalResponseTime = new LongAdder();
    private final AtomicLong maxResponseTime = new AtomicLong();

    private final Map<Integer, LongAdder> statusCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> pathCounts = new ConcurrentHashMap<>();

    @Override
    public void update(LogEntry log) {
        totalRequests.increment();
        if (log.statusCode() >= ERROR_STATUS_THRESHOLD) {
            errorRequests.increment();
        }
        totalResponseTime.add(log.responseTimeMs());
        maxResponseTime.accumulateAndGet(log.responseTimeMs(), Math::max);

        statusCounts.computeIfAbsent(log.statusCode(), k -> new LongAdder()).increment();
        pathCounts.computeIfAbsent(log.path(), k -> new LongAdder()).increment();
    }

    public long getTotalRequests() {
        return totalRequests.sum();
    }

    public long getErrorRequests() {
        return errorRequests.sum();
    }

    public long getTotalResponseTime() {
        return totalResponseTime.sum();
    }

    public long getMaxResponseTime() {
        return maxResponseTime.get();
    }

    public double getAverageResponseTime() {
        long requests = getTotalRequests();
        if (requests == 0) {
            return 0;
        }
        return (double) totalResponseTime.sum() / requests;
    }

    public Map<Integer, Long> getStatusCounts() {
        return statusCounts.entrySet().stream().
                collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().longValue()));
    }

    public Map<String, Long> getPathCounts() {
        return pathCounts.entrySet().stream().
                collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().longValue()));
    }
}
