package io.resttestgen.core.datatype.parameter.leaves;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class GenericParameter extends LeafParameter {

    private static final Logger logger = LogManager.getLogger(GenericParameter.class);


    public GenericParameter(Map<String, Object> parameterMap, String name) {
        super(parameterMap, name);
    }

    protected GenericParameter(LeafParameter other) {
        super(other);
    }

    @Override
    public ParameterType getType() {
        return ParameterType.UNKNOWN;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Parameter merge(Parameter other) {
        logger.warn("Cannot merge " + GenericParameter.class + " instances.");
        return this;
    }

    @Override
    public boolean isValueCompliant(Object value) {
        return true;
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        return true;
    }

    @Override
    public String getJSONString() {
        return getJSONHeading() + getConcreteValue().toString();
    }

    @Override
    public Parameter deepClone() {
        return new GenericParameter(this);
    }
}
