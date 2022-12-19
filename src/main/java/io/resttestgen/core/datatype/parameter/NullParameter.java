package io.resttestgen.core.datatype.parameter;

import com.google.gson.JsonElement;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.openapi.Operation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

public class NullParameter extends ParameterLeaf {

    /*
    The value of the instances of this class is always the string "null". The use of the string instead of the
    null value from Java has two main reasons:
    - avoid matching a NullParameter instance as a non-initialized ParameterLeaf
    - avoid the crash in many methods and using okhttp caused by NullPointerExceptions
     */

    public NullParameter(NullParameter other) {
        super(other);
        this.value = "null";
    }

    public NullParameter(NullParameter other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
        this.value = "null";
    }

    public NullParameter(ParameterElement source) {
        super(source);
        this.value = "null";
    }

    public NullParameter(JsonElement jsonElement, Operation operation, ParameterElement parent, String name) {
        super(operation, parent);

        setValue(null);

        this.name = new ParameterName(Objects.requireNonNullElse(name, ""));
        this.normalizedName = NormalizedParameterName.computeParameterNormalizedName(this);
        this.type = ParameterType.UNKNOWN;
    }

    public NullParameter merge(ParameterElement other) {
        // No additional behavior/constraints in null parameter
        return this;
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        return o == null;
    }

    @Override
    public boolean isValueCompliant(Object value) {
        return value == null || value.toString().equals("null") || value instanceof NullParameter;
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
        return getJSONHeading() + "null";
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public NullParameter deepClone() {
        return new NullParameter(this);
    }

    @Override
    public NullParameter deepClone(Operation operation, ParameterElement parent) {
        return new NullParameter(this, operation, parent);
    }

}
