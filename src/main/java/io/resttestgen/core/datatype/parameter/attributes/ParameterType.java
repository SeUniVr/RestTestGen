package io.resttestgen.core.datatype.parameter.attributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum ParameterType {
    INTEGER,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY,
    STRING,
    // To support combined schemas
    ALLOF,
    ANYOF,
    ONEOF,

    MISSING, // To codify missing type and increase fault tolerance
    UNKNOWN // Unknown type. To increase fault tolerance
    ;

    private static final Logger logger = LogManager.getLogger(ParameterType.class);

    public static ParameterType getTypeFromString(String typeName) {
        if (typeName == null) {
            return  MISSING;
        }

        switch (typeName.toLowerCase()) {
            case "string":
                return STRING;
            case "integer":
                return INTEGER;
            case "number":
                return NUMBER;
            case "boolean":
                return BOOLEAN;
            case "object":
                return OBJECT;
            case "array":
                return ARRAY;
            case "allof":
                return ALLOF;
            case "anyof":
                return ANYOF;
            case "oneof":
                return ONEOF;
            default:
                logger.warn("Unknown type \"" + typeName + "\".");
                return UNKNOWN;
        }
    }


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
