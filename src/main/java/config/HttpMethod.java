package config;

import java.util.Locale;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE;

    public static HttpMethod from(String value) {
        return HttpMethod.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
