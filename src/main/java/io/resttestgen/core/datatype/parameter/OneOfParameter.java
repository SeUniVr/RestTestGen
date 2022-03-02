package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class OneOfParameter extends CombinedSchemaParameter {

    private static final Logger logger = LogManager.getLogger(OneOfParameter.class);

    public OneOfParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);
    }

    public OneOfParameter(Map<String, Object> parameterMap, Operation operation, String name) {
        super(parameterMap, operation, name);
    }

    protected OneOfParameter(ParameterElement other) {
        super(other);
    }

    protected OneOfParameter(OneOfParameter other) {
        super(other);
    }

    protected OneOfParameter(OneOfParameter other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
    }

    @Override
    public ParameterElement merge() {
        // TODO: pick randomly if needed
        ParameterElement merged = this.parametersSchemas.stream().findFirst().get();

        return merged;
    }

    @Override
    protected String getKeyFiledName() {
        return "oneOf";
    }

    @Override
    // TODO: implement
    public boolean isObjectTypeCompliant(Object o) {
        return false;
    }

    @Override
    public OneOfParameter deepClone() {
        return new OneOfParameter(this);
    }

    @Override
    public OneOfParameter deepClone(Operation operation, ParameterElement parent) {
        return new OneOfParameter(this, operation, parent);
    }
}
