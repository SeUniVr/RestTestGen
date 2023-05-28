package io.resttestgen.core.datatype.parameter.combined;

import io.resttestgen.core.datatype.parameter.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class AnyOfParameter extends CombinedSchemaParameter {

    private static final Logger logger = LogManager.getLogger(AnyOfParameter.class);

    public AnyOfParameter(Map<String, Object> parameterMap, String name) {
        super(parameterMap, name);
    }

    protected AnyOfParameter(Parameter other) {
        super(other);
    }

    @Override
    public Parameter merge() {
        // TODO: pick randomly if needed
        Parameter merged = this.parametersSchemas.stream().findFirst().get();

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
}
