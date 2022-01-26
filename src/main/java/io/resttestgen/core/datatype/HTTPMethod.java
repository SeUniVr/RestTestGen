package io.resttestgen.core.datatype;

public enum HTTPMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE;

    public static HTTPMethod getMethod(String stringMethod) {
        for (HTTPMethod method : HTTPMethod.values()) {
            if (method.name().equalsIgnoreCase(stringMethod)) {
                return method;
            }
        }

        throw new IllegalArgumentException("Invalid value '" + stringMethod + "' for HTTP method.");
    }
}
