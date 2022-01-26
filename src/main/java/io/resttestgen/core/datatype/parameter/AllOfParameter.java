package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class AllOfParameter extends CombinedSchemaParameter {

    private static final Logger logger = LogManager.getLogger(AllOfParameter.class);

    public AllOfParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);
    }

    public AllOfParameter(Map<String, Object> parameterMap, Operation operation, String name) {
        super(parameterMap, operation, name);
    }

    protected AllOfParameter(ParameterElement other) {
        super(other);
    }

    protected AllOfParameter(AllOfParameter other) {
        super(other);
    }

    protected AllOfParameter(AllOfParameter other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
    }

    @Override
    public ParameterElement merge() {
        ParameterElement merged = this.parametersSchemas.stream().findFirst().get();
        for (ParameterElement schema : this.parametersSchemas) {
            merged = merged.merge(schema);
        }

        return merged;
    }

    @Override
    protected String getKeyFiledName() {
        return "allOf";
    }

    @Override
    // TODO: implement
    public boolean isObjectTypeCompliant(Object o) {
        return false;
    }

    @Override
    public AllOfParameter deepClone() {
        return new AllOfParameter(this);
    }

    @Override
    public AllOfParameter deepClone(Operation operation, ParameterElement parent) {
        return new AllOfParameter(this, operation, parent);
    }

}
