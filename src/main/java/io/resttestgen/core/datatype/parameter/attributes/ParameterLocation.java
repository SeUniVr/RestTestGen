package io.resttestgen.core.datatype.parameter.attributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum ParameterLocation {
    HEADER,
    PATH,
    QUERY,
    COOKIE,
    REQUEST_BODY,
    RESPONSE_BODY,

    MISSING,
    UNKNOWN
    ;

    private static final Logger logger = LogManager.getLogger(ParameterLocation.class);

    public static ParameterLocation getLocationFromString(String location) {
        if (location == null) {
            return MISSING;
        }
        switch (location.toLowerCase()) {
            case "header":
                return HEADER;
            case "path":
                return PATH;
            case "query":
                return QUERY;
            case "cookie":
                return COOKIE;
            case "request_body":
                return REQUEST_BODY;
            case "response_body":
                return RESPONSE_BODY;
            default:
                logger.warn("Unsupported location \"" + location + "\" for parameters.");
                return UNKNOWN;
        }
    }
}
