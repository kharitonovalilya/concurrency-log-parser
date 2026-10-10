import config.HttpMethod;
import model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import statistics.Statistics;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsTest {
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new Statistics();
    }

    @Test
    void shouldUpdateStatistics() {
        statistics.update(new LogEntry(
                LocalDateTime.now(),
                HttpMethod.GET,
                "/users",
                200,
                53
        ));

        statistics.update(new LogEntry(
                LocalDateTime.now(),
                HttpMethod.POST,
                "/wishlist",
                201,
                81
        ));

        statistics.update(new LogEntry(
                LocalDateTime.now(),
                HttpMethod.GET,
                "/wishes",
                500,
                340
        ));

        statistics.update(new LogEntry(
                LocalDateTime.now(),
                HttpMethod.GET,
                "/users",
                200,
                41
        ));

        assertEquals(4, statistics.getTotalRequests());
        assertEquals(1, statistics.getErrorRequests());
        assertEquals(515, statistics.getTotalResponseTime());
        assertEquals(340, statistics.getMaxResponseTime());
        assertEquals(128.75, statistics.getAverageResponseTime());

        assertEquals(2, statistics.getStatusCounts().get(200));
        assertEquals(1, statistics.getStatusCounts().get(201));
        assertEquals(1, statistics.getStatusCounts().get(500));

        assertEquals(2, statistics.getPathCounts().get("/users"));
        assertEquals(1, statistics.getPathCounts().get("/wishlist"));
        assertEquals(1, statistics.getPathCounts().get("/wishes"));
    }

}