import config.HttpMethod;
import model.LogEntry;
import org.junit.jupiter.api.Test;
import parser.LogParser;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogParserTest {
    private final LogParser parser = new LogParser();

    @Test
    void shouldParseCorrectLogLine() {
        LogEntry entry = parser.parse(
                "2026-10-10T10:15:31 GET /users 200 53"
        );

        assertEquals(LocalDateTime.of(2026, 10, 10, 10, 15, 31), entry.timestamp());
        assertEquals(HttpMethod.GET, entry.method());
        assertEquals("/users", entry.path());
        assertEquals(200, entry.statusCode());
        assertEquals(53, entry.responseTimeMs());
    }

    @Test
    void shouldThrowExceptionWhenTokenCountIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("2026-10-10 GET /users")
        );
    }

    @Test
    void shouldThrowExceptionWhenStatusCodeIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(
                        "2026-10-10T10:15:31 GET /users abc 53"
                )
        );
    }
}