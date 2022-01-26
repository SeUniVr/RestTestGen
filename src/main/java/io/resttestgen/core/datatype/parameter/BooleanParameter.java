package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

public class BooleanParameter extends ParameterLeaf {

    private static final Logger logger = LogManager.getLogger(BooleanParameter.class);

    public BooleanParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);
    }

    public BooleanParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation) {
        this(parent, parameterMap, operation, null);
    }

    public BooleanParameter(BooleanParameter other) {
        super(other);
    }

    public BooleanParameter(BooleanParameter other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
    }

    public BooleanParameter(ParameterLeaf source) {
        super(source);
        this.value = "null";
    }

    public BooleanParameter(ParameterElement source) {
        super(source);
    }

    public BooleanParameter merge(ParameterElement other) {
        // No additional behavior/constraints in boolean parameter
        return this;
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        if (o == null) {
            return false;
        }
        return Boolean.class.isAssignableFrom(o.getClass());
    }

    @Override
    public Object generateCompliantValue() {

        // Generate random value
        Object generatedValue = random.nextBoolean();

        // Log the generated value
        logger.debug("Generated boolean value for parameter " + normalizedName + " (" + name + "): " + generatedValue);

        // Return generated value
        return generatedValue;
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
        return getJSONHeading() + value.toString();
    }

    @Override
    public BooleanParameter deepClone() {
        return new BooleanParameter(this);
    }

    @Override
    public BooleanParameter deepClone(Operation operation, ParameterElement parent) {
        return new BooleanParameter(this, operation, parent);
    }

}
