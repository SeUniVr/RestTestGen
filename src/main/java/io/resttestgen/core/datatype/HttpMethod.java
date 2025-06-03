package io.resttestgen.core.datatype;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE;

    public static HttpMethod getMethod(String stringMethod) {
        for (HttpMethod method : HttpMethod.values()) {
            if (method.name().equalsIgnoreCase(stringMethod)) {
                return method;
            }
        }

        throw new IllegalArgumentException("Invalid value '" + stringMethod + "' for HTTP method.");
    }

    public static Boolean isHttpMethod(String stringMethod) {
        for (HttpMethod method : HttpMethod.values()) {
            if (method.name().equalsIgnoreCase(stringMethod)) {
                return true;
            }
        }
        return false;
    }
}
