package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

public class GenericParameter extends ParameterLeaf {

    private static final Logger logger = LogManager.getLogger(GenericParameter.class);


    public GenericParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);
    }

    public GenericParameter(ParameterElement parent, Map<String, Object> parameterMap, Operation operation) {
        super(parent, parameterMap, operation);
    }

    protected GenericParameter(ParameterLeaf other) {
        super(other);
    }

    protected GenericParameter(ParameterLeaf other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
    }

    protected GenericParameter(ParameterElement other) {
        super(other);
    }

    @Override
    public ParameterElement merge(ParameterElement other) {
        logger.warn("Cannot merge " + GenericParameter.class + " instances.");
        return this;
    }


    @Override
    public boolean isObjectTypeCompliant(Object o) {
        return true;
    }

    @Override
    public String getJSONString() {
        return getJSONHeading() + value.toString();
    }

    @Override
    public ParameterElement deepClone() {
        return new GenericParameter(this);
    }

    @Override
    public ParameterElement deepClone(Operation operation, ParameterElement parent) {
        return new GenericParameter(this, operation, parent);
    }

    // FIXME: return all types
    @Override
    public Object getValueFromDictionary(Dictionary dictionary) {
        return null;
    }

    // FIXME: return all types
    @Override
    public Object getValueFromNormalizedDictionary(Dictionary dictionary) {
        return null;
    }

    @Override
    public Object generateCompliantValue() {
        return null;
    }

    /**
     * No arrays are available at this level. No underlying parameters are available in leaves.
     * @return an empty list
     */
    @Override
    public Collection<ParameterArray> getArrays() {
        return new LinkedList<ParameterArray>();
    }
}
