package io.resttestgen.core.datatype.parameter;

import com.google.gson.JsonPrimitive;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

public class StringParameter extends ParameterLeaf {

    private Integer maxLength; // MUST be >= 0
    private Integer minLength = 0;
    private String pattern; // TODO: add support for pattern

    private static final Logger logger = LogManager.getLogger(StringParameter.class);

    public StringParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceMap = parameterMap.containsKey("schema") ?
                (Map<String, Object>) parameterMap.get("schema") :
                parameterMap;

        if (sourceMap.containsKey("maxLength")) {
            int maxLength = (int) ((double) sourceMap.get("maxLength"));
            if (maxLength < 0) {
                logger.warn("Max length " + maxLength + " not valid for parameter '" + getName() + "' in operation '" +
                operation + "'. The value will be ignored.");
            } else {
                this.maxLength = maxLength;
            }
        }

        if (sourceMap.containsKey("minLength")) {
            int minLength = (int) ((double) sourceMap.get("minLength"));
            if (minLength < 0 || (this.maxLength != null && minLength > this.maxLength)) {
                logger.warn("Min length " + minLength + " not valid for parameter '" + getName() + "' in operation '" +
                        operation + "'. The value will be ignored.");
            } else {
                this.minLength = minLength;
            }
        }

        this.pattern = (String) sourceMap.get("pattern");
    }

    public StringParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation) {
        this(parent, parameterMap, operation, null);
    }

    public StringParameter(StringParameter other) {
        super(other);

        maxLength = other.maxLength;
        minLength = other.minLength;
        pattern = other.pattern;
    }

    public StringParameter(StringParameter other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);

        maxLength = other.maxLength;
        minLength = other.minLength;
        pattern = other.pattern;
    }

    public StringParameter(ParameterElement source) {
        super(source);
        this.value = null;
    }

    public StringParameter(JsonPrimitive jsonPrimitive, Operation operation, ParameterElement parent, String name) {
        super(operation, parent);

        setValue(jsonPrimitive.getAsString());

        this.name = new ParameterName(Objects.requireNonNullElse(name, ""));
        this.normalizedName = NormalizedParameterName.computeParameterNormalizedName(this);
        this.type = ParameterType.STRING;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public StringParameter merge(ParameterElement other) {
        if (!(other instanceof StringParameter)) {
            throw new IllegalArgumentException("Cannot merge a " + this.getClass() + " instance with a "
                    + other.getClass() + " one.");
        }

        StringParameter stringParameter = (StringParameter) other;
        StringParameter merged = new StringParameter(this);
        merged.maxLength = this.maxLength == null ?
                stringParameter.maxLength : stringParameter.maxLength != null ?
                Math.min(this.maxLength, stringParameter.maxLength) : null;
        merged.minLength = this.minLength == null ?
                stringParameter.minLength : stringParameter.minLength != null ?
                Math.max(this.minLength, stringParameter.minLength) : null;
        // TODO: when adding pattern support, concat the two patterns

        return merged;
    }

    @Override
    public boolean isValueCompliant(Object value) {
        if (value instanceof ParameterLeaf) {
            value = ((ParameterLeaf) value).getConcreteValue();
        }
        try {
            String stringValue = ObjectHelper.castToString(value);

            // Check if value is in enum set, if enum values are available
            if (getEnumValues().size() == 0 || getEnumValues().contains(stringValue)) {

                // Check if length is compliant with maxLength and minLength, if defined
                if ((maxLength == null || stringValue.length() <= maxLength) && (minLength == null || stringValue.length() >= minLength)) {
                    return true;
                }
            }
        } catch (ClassCastException ignored) {}
        return false;
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof StringParameter) {
            return true;
        }
        return String.class.isAssignableFrom(o.getClass());
    }

    /**
     * Infers a format from format, type, and name of the parameter.
     * @return the inferred format.
     */
    public ParameterTypeFormat inferFormat() {

        ExtendedRandom random = Environment.getInstance().getRandom();

        switch (format) {
            case BYTE:
                return ParameterTypeFormat.BYTE;
            case BINARY:
                return ParameterTypeFormat.BINARY;
            case DATE:
                return ParameterTypeFormat.DATE;
            case DATE_TIME:
                return ParameterTypeFormat.DATE_TIME;
            case TIME:
                return ParameterTypeFormat.TIME;
            case DURATION:
                return ParameterTypeFormat.DURATION;
            case PASSWORD:
                return ParameterTypeFormat.PASSWORD;
            case HOSTNAME:
                return ParameterTypeFormat.HOSTNAME;
            case URI:
                return ParameterTypeFormat.URI;
            case UUID:
                return ParameterTypeFormat.UUID;
            case IPV4:
                return ParameterTypeFormat.IPV4;
            case IPV6:
                return ParameterTypeFormat.IPV6;
            case HASH:
                return ParameterTypeFormat.HASH;
            case EMAIL:
                return ParameterTypeFormat.EMAIL;
            case PHONE:
                return ParameterTypeFormat.PHONE;
            case IBAN:
                return ParameterTypeFormat.IBAN;
            case FISCAL_CODE:
                return ParameterTypeFormat.FISCAL_CODE;
            case SSN:
                return ParameterTypeFormat.SSN;
            default:
                if (name.contains("email") || name.contains("e-mail")) {
                    return ParameterTypeFormat.EMAIL;
                } else if (name.contains("password")) {
                    return ParameterTypeFormat.PASSWORD;
                } else if (name.endsWith("time") || name.startsWith("time")) {
                    return ParameterTypeFormat.TIME;
                } else if (name.contains("duration")) {
                    return ParameterTypeFormat.DURATION;
                } else if (name.contains("iban")) {
                    return ParameterTypeFormat.IBAN;
                } else if ((name.contains("codice") && name.contains("fiscale")) || name.startsWith("cf") ||
                        name.endsWith("cf")) {
                    return ParameterTypeFormat.FISCAL_CODE;
                } else if ((name.contains("social") && name.contains("security") && name.contains("number")) ||
                        name.contains("ssn")) {
                    return ParameterTypeFormat.SSN;
                } else if (name.contains("uuid")) {
                    return ParameterTypeFormat.UUID;
                } else if (name.contains("phone")) {
                    return ParameterTypeFormat.PHONE;
                } else if (name.startsWith("uri") || name.endsWith("uri") ||
                        name.startsWith("url") || name.endsWith("url")) {
                    return ParameterTypeFormat.URI;
                } else if (name.contains("hostname")) {
                    return ParameterTypeFormat.HOSTNAME;
                } else if (name.contains("host")) {
                    if (random.nextBoolean()) {
                        return ParameterTypeFormat.HOSTNAME;
                    }
                    return ParameterTypeFormat.IPV4;
                } else if (name.endsWith("ip") || name.startsWith("ip")) {
                    if (random.nextInt(10) < 8) {
                        return ParameterTypeFormat.IPV4;
                    }
                    return ParameterTypeFormat.IPV6;
                } else if (name.startsWith("date") || name.endsWith("date")) {
                    if (random.nextBoolean()) {
                        return ParameterTypeFormat.DATE;
                    }
                    return ParameterTypeFormat.DATE_TIME;
                } else if (name.endsWith("file")) {
                    if (random.nextInt(10) < 8) {
                        return ParameterTypeFormat.BINARY;
                    }
                    return ParameterTypeFormat.BYTE;
                } else if (name.endsWith("time") || name.startsWith("time")) {
                    if (random.nextBoolean()) {
                        return ParameterTypeFormat.TIME;
                    }
                    return ParameterTypeFormat.DATE_TIME;
                } else if (name.contains("sha-1") || name.endsWith("hash") || name.contains("md5") ||
                        name.contains("sha-256")) {
                    return ParameterTypeFormat.HASH;
                } else if (name.endsWith("location")) {
                    return ParameterTypeFormat.LOCATION;
                }
                return ParameterTypeFormat.MISSING;
        }
    }

    @Override
    public StringParameter deepClone() {
        return new StringParameter(this);
    }

    @Override
    public StringParameter deepClone(Operation operation, ParameterElement parent) {
        return new StringParameter(this, operation, parent);
    }

    /**
     * No arrays are available at this level. No underlying parameters are available in leaves.
     * @return an empty list
     */
    @Override
    public Collection<ParameterArray> getArrays() {
        return new LinkedList<>();
    }

    @Override
    public String getJSONString() {

        // If leaf is inside an array, don't print the leaf name
        if (this.getParent() instanceof ParameterArray) {
            return "\"" + getConcreteValue() + "\"";
        } else {
            return getJSONHeading() + "\"" + getConcreteValue() + "\"";
        }
    }
}
