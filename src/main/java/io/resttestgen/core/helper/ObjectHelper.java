package io.resttestgen.core.helper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;

import java.util.List;
import java.util.Map;

// TODO: rename class
public class ObjectHelper {

    public static <T> T deepCloneObject(T o) {
        if (o instanceof Number) {
            return o;
        } else if (o instanceof LeafParameter) {
            return o;
        } else {
            String json = new Gson().toJson(o);
            return new Gson().fromJson(json, new TypeToken<>() {
            }.getType());
        }
    }

    public static Object castToParameterValueType(Object o, ParameterType type) throws ClassCastException {
        switch (type) {
            case INTEGER:
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
}
