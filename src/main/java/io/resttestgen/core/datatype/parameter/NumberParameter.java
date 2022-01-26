package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

public class NumberParameter extends ParameterLeaf {

    private Double maximum;
    private Double minimum;

    private boolean exclusiveMaximum;
    private boolean exclusiveMinimum;

    private static final Logger logger = LogManager.getLogger(NumberParameter.class);

    public NumberParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);

        Map<String, Object> sourceMap = parameterMap.containsKey("schema") ?
                (Map<String, Object>) parameterMap.get("schema") :
                parameterMap;
        this.exclusiveMaximum = Boolean.parseBoolean((String) sourceMap.get("exclusiveMaximum"));
        this.exclusiveMinimum = Boolean.parseBoolean((String) sourceMap.get("exclusiveMinimum"));
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

    public Double getMaximum() {
        return maximum;
    }

    public Double getMinimum() {
        return minimum;
    }

    public boolean isExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public boolean isExclusiveMinimum() {
        return exclusiveMinimum;
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        if (o == null) {
            return false;
        }
        return Number.class.isAssignableFrom(o.getClass());
    }

    @Override
    public Object generateCompliantValue() {

        // Initialize value container
        Object generatedValue = null;

        // Generate numeric value base on the (inferred) format. In some cases, with 0.5 probability, we generate a
        // small number (< 10).
        boolean halfProb = random.nextBoolean();
        switch (inferFormat()) {
            case INT8:
                if (halfProb) {
                    generatedValue = random.nextLength(0, 120);
                    break;
                }
                generatedValue = random.nextIntBetween(-128, 127);
                break;
            case INT16:
                if (halfProb) {
                    generatedValue = random.nextLength(0, 120);
                    break;
                }
                generatedValue = random.nextIntBetween(-32768, 32767);
                break;
            case INT32:
                if (halfProb) {
                    generatedValue = random.nextLength(0, 120);
                    break;
                }
                generatedValue = random.nextInt();
                break;
            case INT64:
                if (halfProb) {
                    generatedValue = Long.valueOf(random.nextLength(0, 120));
                    break;
                }
                generatedValue = random.nextLong();
                break;
            case UINT8:
                if (halfProb) {
                    generatedValue = random.nextLength(0, 120);
                    break;
                }
                generatedValue = random.nextIntBetween(0, 255);
                break;
            case UINT16:
                if (halfProb) {
                    generatedValue = random.nextLength(0, 120);
                    break;
                }
                generatedValue = random.nextIntBetween(0, 65535);
                break;
            case UINT32:
                if (halfProb) {
                    generatedValue = Long.valueOf(random.nextLength(0, 120));
                    break;
                }
                generatedValue = random.nextLong(0, 4294967295L);
                break;
            case UINT64:
                if (halfProb) {
                    generatedValue = Long.valueOf(random.nextLength(0, 120));
                    break;
                }
                generatedValue = random.nextLong(0, 9223372036854775807L);
                break;
            case FLOAT:
                generatedValue = random.nextFloat();
                break;
            case DOUBLE:
                generatedValue = random.nextDouble();
                break;
        }

        logger.debug("Generated numeric value for parameter " + normalizedName + " (" + name + "): " + generatedValue);

        return generatedValue;
    }

    /**
     * Infers a format from format, type, and name of the parameter.
     * @return the inferred format.
     */
    private ParameterTypeFormat inferFormat() {
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
                    switch (random.nextIntBetween(0, 3)) {
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
        if (value instanceof Integer) {
            return getJSONHeading() + ((Integer) value).toString();
        }
        return getJSONHeading() + value.toString();
    }

}
