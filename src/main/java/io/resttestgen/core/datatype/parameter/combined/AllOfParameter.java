package io.resttestgen.core.datatype.parameter.combined;

import io.resttestgen.core.datatype.parameter.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class AllOfParameter extends CombinedSchemaParameter {

    private static final Logger logger = LogManager.getLogger(AllOfParameter.class);

    public AllOfParameter(Map<String, Object> parameterMap, String name) {
        super(parameterMap, name);
    }

    protected AllOfParameter(Parameter other) {
        super(other);
    }

    @Override
    public Parameter merge() {
        Parameter merged = this.parametersSchemas.stream().findFirst().get();
        for (Parameter schema : this.parametersSchemas) {
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
}
