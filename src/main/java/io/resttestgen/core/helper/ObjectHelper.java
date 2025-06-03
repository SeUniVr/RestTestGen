package io.resttestgen.core.helper;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;


public class ObjectHelper {

    private static final Logger logger = LogManager.getLogger(ObjectHelper.class);

    public static <T> T deepCloneObject(T o) {
        if (o == null) {
            return null;
        } else if (o instanceof Number) {
            return o;
        } else if (o instanceof String) {
            return o;
        } else if (o instanceof Boolean) {
            return o;
        } else if (o instanceof LeafParameter) {
            return o;
        } else if (o instanceof Set) {
            HashSet cloned = new HashSet();
            for (Object oo : ((Set) o)) {
                cloned.add(deepCloneObject(oo));
            }
            return (T) cloned;
        } else if (o instanceof ArrayList) {
            List cloned = new ArrayList();
            for (Object oo : ((List) o)) {
                cloned.add(deepCloneObject(oo));
            }
            return (T) cloned;
        } else if (o instanceof List) {
            List cloned = new LinkedList();
            for (Object oo : ((List) o)) {
                cloned.add(deepCloneObject(oo));
            }
            return (T) cloned;
        } else if (o instanceof Map) {
            Map cloned = new LinkedTreeMap();
            for (Object key : ((Map) o).keySet()) {
                cloned.put(key, deepCloneObject(((Map) o).get(key)));
            }
            return (T) cloned;
        } else {
            //logger.warn("Cloning a complex object: {}", o);#

            String json = new Gson().toJson(o);

            return new Gson().fromJson(json, new TypeToken<>() {
            }.getType());
        }
    }

    // TODO: Add Deep Clone Value for leaves

    public static boolean isObjectCastableToParameter(Object o, ParameterType parameterType) {
        try {
            ObjectHelper.castToParameterValueType(o, parameterType);
        } catch (ClassCastException exception) {
            return false;
        }
        return true;
    }

    public static Object castToParameterValueType(Object o, ParameterType type) throws ClassCastException {
        switch (type) {
            case INTEGER:
                return castToInteger(o);
            case NUMBER:
                return castToNumber(o);
            case BOOLEAN:
                return castToBoolean(o);
            case OBJECT:
                return castToMap(o);
            case ARRAY:
                return castToList(o);
            case STRING:
                return castToString(o);
            case MISSING:
            case UNKNOWN:
            default:
                throw new ClassCastException();
        }
    }

    public static Boolean castToBoolean(Object o) throws ClassCastException {
        if (o == null) {
            throw new ClassCastException();
        }

        if (o instanceof String) {
            String stringO = (String) o;
            if (stringO.equalsIgnoreCase("true")) {
                return true;
            } else if (stringO.equalsIgnoreCase("false")) {
                return false;
            }
            try {
                double parsedVal = Double.parseDouble(stringO);
                if (parsedVal == 1) {
                    return true;
                } else if (parsedVal == 0) {
                    return false;
                }
                throw new ClassCastException();
            } catch (NumberFormatException e) {
                throw new ClassCastException();
            }
        }

        if (Number.class.isAssignableFrom(o.getClass())) {
            Number numberO = (Number) o;

            switch (numberO.intValue()) {
                case 0:
                    return false;
                case 1:
                    return true;
                default:
                    throw new ClassCastException();
            }
        }

        return (Boolean) o;
    }

    public static Integer castToInteger(Object o) throws ClassCastException {
        if (o == null) {
            throw new ClassCastException();
        }

        if (o instanceof String) {
            try {
                return Integer.parseInt((String) o);
            } catch (NumberFormatException e) {
                throw new ClassCastException();
            }
        }

        return (Integer) o;
    }

    public static Number castToNumber(Object o) throws ClassCastException {
        if (o == null) {
            throw new ClassCastException();
        }

        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException e) {
                throw new ClassCastException();
            }
        }

        return (Number) o;
    }

    public static List castToList(Object o) throws ClassCastException {
        if (o == null) {
            throw new ClassCastException();
        }
        return (List) o;
    }

    public static Map castToMap(Object o) throws ClassCastException {
        if (o == null) {
            throw new ClassCastException();
        }
        return (Map) o;
    }

    /**
     * This casting function converts booleans and numbers in strings. Since every object can be cast to a string,
     * the behavior is limited to the aforementioned classes throwing in the other cases a ClassCastException.
     * @param o The object to be converted.
     * @return The object passed as parameter cast to a String.
     * @throws ClassCastException if cast fails.
     */
    public static String castToString(Object o) throws ClassCastException {
        if (o == null) {
            throw new ClassCastException();
        }

        if (String.class.isAssignableFrom(o.getClass()) ||
                Number.class.isAssignableFrom(o.getClass()) ||
                Boolean.class.isAssignableFrom(o.getClass())) {
            return o.toString();
        }

        throw new ClassCastException();
    }

    public static Number tryCastToStrictNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // ignore
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // ignore
        }

        return null;
    }


    public static void replaceMapValuesWithHarderConstraints(Map<String, Object> map) {
        map.forEach((key, value) -> {
            if (value instanceof String) {
                Number asNumber = tryCastToStrictNumber((String) value);
                if (asNumber != null) {
                    map.put(key, asNumber);
                }
            } else if (value instanceof Map) {
                replaceMapValuesWithHarderConstraints((Map) value);
            }
        });
    }

    public static Object parseToJavaObject(String value) {
        Number asNumber = tryCastToStrictNumber(value);
        if (asNumber != null) {
            return asNumber;
        }

        try {
            Object fromJsonObject = new Gson().fromJson(value, new TypeToken<>() {}.getType());
            // If it is a string, you have to check if it is actually an array, so skip the return here
            if (!(fromJsonObject instanceof String)) {
                if (fromJsonObject instanceof Map) {
                    replaceMapValuesWithHarderConstraints(
                            (Map<String, Object>) fromJsonObject);
                }

                return fromJsonObject;
            }
        } catch (JsonParseException e) {
            // ignore
        }

//        String[] splitValues = value.split("[ ,|]");
//        if (splitValues.length > 1) {
//            return Arrays.stream(splitValues)
//                    .map(ObjectHelper::parseToJavaObject)
//                    .collect(Collectors.toList());
//        }

        return value;
    }
}
