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

public class NumberParameter extends ParameterLeaf {

    private Double maximum;
    private Double minimum;

    private boolean exclusiveMaximum = false;
    private boolean exclusiveMinimum = false;

    private static final Logger logger = LogManager.getLogger(NumberParameter.class);

    public NumberParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceMap = parameterMap.containsKey("schema") ?
                (Map<String, Object>) parameterMap.get("schema") :
                parameterMap;
        if (sourceMap.get("exclusiveMaximum") != null && sourceMap.get("exclusiveMaximum").toString().trim().equalsIgnoreCase("true")) {
            this.exclusiveMaximum = true;
        }
        if (sourceMap.get("exclusiveMinimum") != null && sourceMap.get("exclusiveMinimum").toString().trim().equalsIgnoreCase("true")) {
            this.exclusiveMinimum = true;
        }
        if (sourceMap.containsKey("maximum")) {
            this.maximum = (Double) sourceMap.get("maximum");
        }
        if (sourceMap.containsKey("minimum")) {
            this.minimum = (Double) sourceMap.get("minimum");
        }
    }

    public NumberParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation) {
        this(parent, parameterMap, operation, null);
    }

    private NumberParameter(NumberParameter other) {
        super(other);

        maximum = other.maximum;
        minimum = other.minimum;
        exclusiveMaximum = other.exclusiveMaximum;
        exclusiveMinimum = other.exclusiveMinimum;
    }

    public NumberParameter(NumberParameter other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);

        maximum = other.maximum;
        minimum = other.minimum;
        exclusiveMaximum = other.exclusiveMaximum;
        exclusiveMinimum = other.exclusiveMinimum;
    }

    public NumberParameter(ParameterElement source) {
        super(source);
        this.value = "null";
    }

    public NumberParameter(JsonPrimitive jsonPrimitive, Operation operation, ParameterElement parent, String name) {
        super(operation, parent);

        double doubleValue = jsonPrimitive.getAsDouble();
        int integerValue = jsonPrimitive.getAsInt();
        long longValue = jsonPrimitive.getAsLong();

        if (doubleValue % 1 == 0) {
            this.type = ParameterType.INTEGER;
            if (longValue == (long) integerValue) {
                this.format = ParameterTypeFormat.INT32;
                setValue(integerValue);
            } else {
                this.format = ParameterTypeFormat.INT64;
                setValue(longValue);
            }
        } else {
            this.type = ParameterType.NUMBER;
            this.format = ParameterTypeFormat.DOUBLE;
            setValue(doubleValue);
        }

        this.name = new ParameterName(Objects.requireNonNullElse(name, ""));
        this.normalizedName = NormalizedParameterName.computeParameterNormalizedName(this);
    }

    public NumberParameter merge(ParameterElement other) {
        if (!(other instanceof NumberParameter)) {
            throw new IllegalArgumentException("Cannot merge a " + this.getClass() + " instance with a "
                    + other.getClass() + " one.");
        }

        NumberParameter numberParameter = (NumberParameter) other;
        NumberParameter merged = new NumberParameter(this);
        merged.maximum = this.maximum == null ?
                numberParameter.maximum : numberParameter.maximum != null ? 
                        Math.min(this.maximum, numberParameter.maximum) : null;
        merged.minimum = this.minimum == null ?
                numberParameter.minimum : numberParameter.minimum != null ?
                Math.max(this.minimum, numberParameter.minimum) : null;
        merged.exclusiveMaximum = this.exclusiveMaximum || numberParameter.exclusiveMaximum;
        merged.exclusiveMinimum = this.exclusiveMinimum || numberParameter.exclusiveMinimum;

        return merged;
    }

    public boolean isMaximum() {
        return maximum != null;
    }

    public boolean isMinimum() {
        return minimum != null;
    }

    public void setMaximum(Double maximum) {
        this.maximum = maximum;
    }

    public Double getMaximum() {
        return maximum;
    }

    public void setMinimum(Double minimum) {
        this.minimum = minimum;
    }

    public Double getMinimum() {
        return minimum;
    }

    public void setExclusiveMaximum(boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public boolean isExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMinimum(boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public boolean isExclusiveMinimum() {
        return exclusiveMinimum;
    }

    @Override
    public boolean isValueCompliant(Object value) {
        if (value instanceof ParameterLeaf) {
            value = ((ParameterLeaf) value).getConcreteValue();
        }
        try {
            double doubleValue = ObjectHelper.castToNumber(value).doubleValue();

            // TODO: check format

            // Check if value is in enum set, if set is defined
            if (getEnumValues().size() == 0 || getEnumValues().contains(doubleValue)) {

                // Check if minimum and maximum constraints are met
                if (((maximum == null || doubleValue <= maximum) && (maximum == null || !exclusiveMaximum || doubleValue < maximum)) &&
                        ((minimum == null || doubleValue >= minimum) && (minimum == null || !exclusiveMinimum || doubleValue > minimum))) {
                    return true;
                }
            }
        } catch (ClassCastException ignored) {}
        return false;
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        // TODO: please test this method
        if (o == null) {
            return false;
        }
        if (o instanceof NumberParameter || Number.class.isAssignableFrom(o.getClass())) {
            return true;
        }
        try {
            ObjectHelper.castToNumber(o);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Infers a format from format, type, and name of the parameter.
     * @return the inferred format.
     */
    public ParameterTypeFormat inferFormat() {

        ExtendedRandom random = Environment.getInstance().getRandom();

        switch (format) {
            case INT8:
                return ParameterTypeFormat.INT8;
            case INT16:
                return ParameterTypeFormat.INT16;
            case INT32:
                return ParameterTypeFormat.INT32;
            case INT64:
                return ParameterTypeFormat.INT64;
            case UINT8:
                return ParameterTypeFormat.UINT8;
            case UINT16:
                return ParameterTypeFormat.UINT16;
            case UINT32:
                return ParameterTypeFormat.UINT32;
            case UINT64:
                return ParameterTypeFormat.UINT64;
            case FLOAT:
                return ParameterTypeFormat.FLOAT;
            case DOUBLE:
                return ParameterTypeFormat.DOUBLE;

            case DECIMAL:
                if (random.nextBoolean()) {
                    return ParameterTypeFormat.FLOAT;
                } else {
                    return ParameterTypeFormat.DOUBLE;
                }

            default:
                if (type == ParameterType.INTEGER) {
                    return ParameterTypeFormat.INT32;
                } else {
                    switch (random.nextInt(0, 3)) {
                        case 0:
                            return ParameterTypeFormat.INT32;
                        case 1:
                            return ParameterTypeFormat.INT64;
                        case 2:
                            return ParameterTypeFormat.FLOAT;
                        case 3:
                            return ParameterTypeFormat.DOUBLE;
                    }
                }

        }
        return ParameterTypeFormat.INT32;
    }

    @Override
    public NumberParameter deepClone() {
        return new NumberParameter(this);
    }

    @Override
    public NumberParameter deepClone(Operation operation, ParameterElement parent) {
        return new NumberParameter(this, operation, parent);
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
        String stringValue = getConcreteValue().toString();

        // If the numeric value is integer (mathematical meaning, i.e., no decimal digits), print it without the .0
        if (getConcreteValue() instanceof Double && ((Double) getConcreteValue()) % 1 == 0) {
            stringValue = Long.toString(((Double) getConcreteValue()).longValue());
        }

        return getJSONHeading() + stringValue;
    }

}
