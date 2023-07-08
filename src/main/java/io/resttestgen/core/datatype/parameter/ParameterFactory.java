package io.resttestgen.core.datatype.parameter;

import com.google.gson.*;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.combined.AllOfParameter;
import io.resttestgen.core.datatype.parameter.combined.AnyOfParameter;
import io.resttestgen.core.datatype.parameter.combined.OneOfParameter;
import io.resttestgen.core.datatype.parameter.exceptions.ParameterCreationException;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.openapi.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;

public class ParameterFactory {

    private static final Logger logger = LogManager.getLogger(ParameterFactory.class);

    public static Parameter getParameter(Map<String, Object> elementMap, String name) {

        checkUnsupportedFeature(elementMap, name);

        // Before the type check, look for combined schemas
        if (elementMap.containsKey("allOf")) {
            return new AllOfParameter(elementMap, name);
        }

        if (elementMap.containsKey("anyOf")) {
            return new AnyOfParameter(elementMap, name);
        }

        if (elementMap.containsKey("oneOf")) {
            return new OneOfParameter(elementMap, name);
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
                return new ArrayParameter(elementMap, name);
            case OBJECT:
                return new ObjectParameter(elementMap, name);
            case NUMBER:
            case INTEGER:
                return new NumberParameter(elementMap, name);
            case STRING:
                return new StringParameter(elementMap, name);
            case BOOLEAN:
                return new BooleanParameter(elementMap, name);
            case UNKNOWN:
            default:
                // Fallback
                logger.warn("Unsupported type '" + elementMap.get("type") + "' for parameter " +
                        name + ". Created a generic parameter.");
                try {
                    return new GenericParameter(elementMap, name);
                } catch (ClassCastException e) {
                    throw new ParameterCreationException("Unable to create generic parameter");
                }
        }
    }

    public static Parameter getParameter(Map<String, Object> elementMap) {
        return getParameter(elementMap, null);
    }

    public static Parameter getParameter(JsonElement jsonElement, String name) {
        if (jsonElement instanceof JsonObject) {
            return new ObjectParameter((JsonObject) jsonElement, name);
        } else if (jsonElement instanceof JsonArray) {
            return new ArrayParameter((JsonArray) jsonElement, name);
        } else if (jsonElement instanceof JsonPrimitive) {
            JsonPrimitive primitive = (JsonPrimitive) jsonElement;
            if (primitive.isString()) {
                return new StringParameter(primitive, name);
            } else if (primitive.isNumber()) {
                return new NumberParameter(primitive, name);
            } else if (primitive.isBoolean()) {
                return new BooleanParameter(primitive, name);
            }
        } else if (jsonElement instanceof JsonNull) {
            return new NullParameter(jsonElement, name);
        }

        // Fallback: return null if the jsonElement is not what is expected
        return null;
    }

    public static Parameter getParameterOfType(ParameterType type) {
        switch (type) {
            case NUMBER: return new NumberParameter();
            case STRING: return new StringParameter();
            case BOOLEAN: return new BooleanParameter();
            case ARRAY: return new ArrayParameter();
            case OBJECT: return new ObjectParameter();
            default: return null;
        }
    }

    public static StructuredParameter getStructuredParameter (Map<String, Object> elementMap, String name) {
        Parameter parameter = getParameter(elementMap, name);
        try {
            return (StructuredParameter) parameter;
        } catch (ClassCastException e) {
            name = getParameterName(elementMap, name);
            throw new ParameterCreationException("Cannot cast to structured parameter " +
                    (name.equals("") ? "" : "'" + name + "'"));
        }
    }

    private static void checkUnsupportedFeature(Map<String, Object> elementMap, String name) {
        name = getParameterName(elementMap, name);
        if (elementMap.containsKey("not")) {
            throw new UnsupportedSpecificationFeature("Unsupported property 'not' found in " +
                    (name.equals("") ? "" : "'" + name + "', "));
        }
    }

    private static String getParameterName(Map<String, Object> elementMap, String name) {
        return name != null ? name:
                elementMap.containsKey("name") ? (String) elementMap.get("name") : "";
    }
}
