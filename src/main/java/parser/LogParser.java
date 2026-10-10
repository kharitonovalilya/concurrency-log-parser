package parser;

import config.HttpMethod;
import model.LogEntry;

import java.time.LocalDateTime;

public class LogParser {
    private static final int EXPECTED_TOKEN_COUNT = 5;

    public LogEntry parse(String line) {
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length != EXPECTED_TOKEN_COUNT) {
            throw new IllegalArgumentException("Invalid log line: " + line);
        }
        try {
            LocalDateTime timestamp = LocalDateTime.parse(tokens[0]);
            HttpMethod method = HttpMethod.from(tokens[1]);
            String path = tokens[2];
            int code = Integer.parseInt(tokens[3]);
            long responseTimeMs = Long.parseLong(tokens[4]);
            return new LogEntry(
                    timestamp,
                    method,
                    path,
                    code,
                    responseTimeMs
            );
        }
        catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid log line: " + line, e);
        }
    }
}
