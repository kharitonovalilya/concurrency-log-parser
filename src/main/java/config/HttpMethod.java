package config;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE;

    public static HttpMethod from(String value) {
        return HttpMethod.valueOf(value.toUpperCase());
    }
}
