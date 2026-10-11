package benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class LogGenerator {

    private record LogTemplate(
            String method,
            String path,
            int statusCode,
            long responseTimeMs
    ) {}

    private static final List<LogTemplate> TEMPLATES = List.of(
            new LogTemplate("GET", "/users", 200, 30),
            new LogTemplate("POST", "/orders", 201, 100),
            new LogTemplate("GET", "/orders", 500, 250),
            new LogTemplate("GET", "/products", 200, 15),
            new LogTemplate("PUT", "/users", 204, 70)
    );

    public static void generate(Path path, int logCount) throws IOException {
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 10, 10, 0);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (int i = 0; i < logCount; i++) {
                LogTemplate template =
                        TEMPLATES.get(i % TEMPLATES.size());
                writer.write(
                        "%s %s %s %d %d".formatted(
                                timestamp.plusSeconds(i),
                                template.method(),
                                template.path(),
                                template.statusCode(),
                                template.responseTimeMs()
                        )
                );
                writer.newLine();
            }
        }
    }
}