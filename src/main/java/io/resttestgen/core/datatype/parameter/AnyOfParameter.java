package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class AnyOfParameter extends CombinedSchemaParameter {

    private static final Logger logger = LogManager.getLogger(AnyOfParameter.class);

    public AnyOfParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);
    }

    public AnyOfParameter(Map<String, Object> parameterMap, Operation operation, String name) {
        super(parameterMap, operation, name);
    }

    protected AnyOfParameter(ParameterElement other) {
        super(other);
    }

    protected AnyOfParameter(AnyOfParameter other) {
        super(other);
    }

    protected AnyOfParameter(AnyOfParameter other, Operation operation, ParameterElement parent) {
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
        return "anyOf";
    }

    @Override
    // TODO: implement
    public boolean isObjectTypeCompliant(Object o) {
        return false;
    }

    @Override
    public AnyOfParameter deepClone() {
        return new AnyOfParameter(this);
    }

    @Override
    public AnyOfParameter deepClone(Operation operation, ParameterElement parent) {
        return new AnyOfParameter(this, operation, parent);
    }
}
