package io.resttestgen.core.datatype.parameter.leaves;

import com.google.gson.JsonPrimitive;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Objects;

public class StringParameter extends LeafParameter {

    private Integer maxLength; // MUST be >= 0
    private Integer minLength = 0;
    private String pattern; // TODO: complete support for pattern

    private static final Logger logger = LogManager.getLogger(StringParameter.class);

    public StringParameter(Map<String, Object> parameterMap, String name) {
        super(parameterMap, name);

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceMap = parameterMap.containsKey("schema") ?
                (Map<String, Object>) parameterMap.get("schema") :
                parameterMap;

        if (sourceMap.containsKey("maxLength")) {
            int maxLength = (int) ((double) sourceMap.get("maxLength"));
            if (maxLength < 0) {
                logger.warn("Max length {} not valid for parameter '{}'. The value will be ignored.", maxLength, getName());
            } else {
                this.maxLength = maxLength;
            }
        }

        if (sourceMap.containsKey("minLength")) {
            int minLength = (int) ((double) sourceMap.get("minLength"));
            if (minLength < 0 || (this.maxLength != null && minLength > this.maxLength)) {
                logger.warn("Min length {} not valid for parameter '{}'. The value will be ignored.", minLength, getName());
            } else {
                this.minLength = minLength;
            }
        }

        this.pattern = (String) sourceMap.get("pattern");
    }

    public StringParameter(Map<String, Object> parameterMap) {
        this(parameterMap, null);
    }

    public StringParameter(StringParameter other) {
        super(other);

        maxLength = other.maxLength;
        minLength = other.minLength;
        pattern = other.pattern;
    }

    public StringParameter(Parameter source) {
        super(source);
        this.type = ParameterType.STRING;
        this.value = null;
    }

    public StringParameter(JsonPrimitive jsonPrimitive, String name) {
        super();
        this.type = ParameterType.STRING;
        setValueManually(jsonPrimitive.getAsString());

        this.name = new ParameterName(Objects.requireNonNullElse(name, ""));
    }

    public StringParameter() { super(); this.type = ParameterType.STRING; }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public void setMinLength(Integer minLength) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.minLength = minLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMaxLength(Integer maxLength) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.maxLength = maxLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public StringParameter merge(Parameter other) {
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
        if (value instanceof LeafParameter) {
            value = ((LeafParameter) value).getConcreteValue();
        }
        try {
            String stringValue = ObjectHelper.castToString(value);

            // Check if value is in enum set, if enum values are available
            if (getEnumValues().isEmpty() || getEnumValues().contains(stringValue)) {

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
        return o != null && (o instanceof StringParameter || String.class.isAssignableFrom(o.getClass()));
    }

    /**
     * Infers a format from format, type, and name of the parameter.
     * @return the inferred format.
     */
    public ParameterTypeFormat inferFormat() {

        ExtendedRandom random = Environment.getInstance().getRandom();

        switch (format) {
            case BYTE:
            case BINARY:
            case DATE:
            case DATE_TIME:
            case TIME:
            case DURATION:
            case PASSWORD:
            case HOSTNAME:
            case URI:
            case UUID:
            case IPV4:
            case IPV6:
            case HASH:
            case EMAIL:
            case PHONE:
            case IBAN:
            case FISCAL_CODE:
            case SSN:
                return format;
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
    public String getJSONString() {

        // If leaf is inside an array, don't print the leaf name
        if (this.getParent() instanceof ArrayParameter) {
            return "\"" + getConcreteValue() + "\"";
        } else {
            return getJSONHeading() + "\"" + getConcreteValue() + "\"";
        }
    }
}
