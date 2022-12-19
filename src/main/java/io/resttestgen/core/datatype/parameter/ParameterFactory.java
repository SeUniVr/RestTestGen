package io.resttestgen.core.datatype.parameter;

import com.google.gson.*;
import io.resttestgen.core.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;

public class ParameterFactory {

    private static final Logger logger = LogManager.getLogger(ParameterFactory.class);

    public static ParameterElement getParameterElement(ParameterElement parent, Map<String, Object> elementMap, Operation operation, String name) {

        checkUnsupportedFeature(elementMap, operation, name);

        // Before the type check, look for combined schemas
        if (elementMap.containsKey("allOf")) {
            return new AllOfParameter(parent, elementMap, operation, name);
        }

        if (elementMap.containsKey("anyOf")) {
            return new AnyOfParameter(parent, elementMap, operation, name);
        }

        if (elementMap.containsKey("oneOf")) {
            return new OneOfParameter(parent, elementMap, operation, name);
        }

        // The type can be defined in the element map or in the schema defined in the element map, depending on
        // the kind of the parameter (request body/response body parameter vs. header/path/query/cookie parameter)
        @SuppressWarnings("unchecked")
        Map<String, Object> targetMap = elementMap.containsKey("schema") ?
                (Map<String, Object>) elementMap.get("schema") :
                elementMap;
        ParameterType type = ParameterType.getTypeFromString((String) targetMap.get("type"));

        switch (type) {
            case ARRAY:
                return new ParameterArray(parent, elementMap, operation, name);
            case OBJECT:
                return new ParameterObject(parent, elementMap, operation, name);
            case NUMBER:
            case INTEGER:
                return new NumberParameter(parent, elementMap, operation, name);
            case STRING:
                return new StringParameter(parent, elementMap, operation, name);
            case BOOLEAN:
                return new BooleanParameter(parent, elementMap, operation, name);
            case UNKNOWN:
            default:
                // Fallback
                logger.warn("Unsupported type '" + elementMap.get("type") + "' for parameter " +
                        name + "(" + operation + "). Created a generic parameter.");
                try {
                    return new GenericParameter(parent, elementMap, operation, name);
                } catch (ClassCastException e) {
                    throw new ParameterCreationException("Unable to create generic parameter");
                }
        }
    }

    public static ParameterElement getParameterElement(ParameterElement parent, Map<String, Object> elementMap, Operation operation) {
        return getParameterElement(parent, elementMap, operation, null);
    }

    public static ParameterElement getParameterElement(ParameterElement parent, JsonElement jsonElement, Operation operation, String name) {
        if (jsonElement instanceof JsonObject) {
            return new ParameterObject((JsonObject) jsonElement, operation, parent, name);
        } else if (jsonElement instanceof JsonArray) {
            return new ParameterArray((JsonArray) jsonElement, operation, parent, name);
        } else if (jsonElement instanceof JsonPrimitive) {
            JsonPrimitive primitive = (JsonPrimitive) jsonElement;
            if (primitive.isString()) {
                return new StringParameter(primitive, operation, parent, name);
            } else if (primitive.isNumber()) {
                return new NumberParameter(primitive, operation, parent, name);
            } else if (primitive.isBoolean()) {
                return new BooleanParameter(primitive, operation, parent, name);
            }
        } else if (jsonElement instanceof JsonNull) {
            return new NullParameter(jsonElement, operation, parent, name);
        }

        // Fallback: return null if the jsonElement is not what is expected
        return null;
    }

    public static StructuredParameterElement getStructuredParameter (ParameterElement parent, Map<String, Object> elementMap, Operation operation, String name) {
        ParameterElement parameter = getParameterElement(parent, elementMap, operation, name);
        try {
            return (StructuredParameterElement) parameter;
        } catch (ClassCastException e) {
            name = getParameterName(elementMap, name);
            throw new ParameterCreationException("Cannot cast to structured parameter " +
                    (name.equals("") ? "" : "'" + name + "' ") +
                    "in operation '" + operation + "'.");
        }
    }

    private static void checkUnsupportedFeature(Map<String, Object> elementMap, Operation operation, String name) {
        name = getParameterName(elementMap, name);
        if (elementMap.containsKey("not")) {
            throw new UnsupportedSpecificationFeature("Unsupported property 'not' found in " +
                    (name.equals("") ? "" : "'" + name + "', ") +
                    "operation '" + operation + "'.");
        }
    }

    private static String getParameterName(Map<String, Object> elementMap, String name) {
        return name != null ? name:
                elementMap.containsKey("name") ? (String) elementMap.get("name") : "";
    }
}
