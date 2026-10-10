package model;

import config.HttpMethod;

import java.time.LocalDateTime;

public record LogEntry(
        LocalDateTime timestamp,
        HttpMethod method,
        String path,
        int statusCode,
        long responseTimeMs
) {}
