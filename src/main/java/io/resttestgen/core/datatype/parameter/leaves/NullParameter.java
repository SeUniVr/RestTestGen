package io.resttestgen.core.datatype.parameter.leaves;

import com.google.gson.JsonElement;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;

import java.util.Objects;

public class NullParameter extends LeafParameter {

    /*
    The value of the instances of this class is always the string "null". The use of the string instead of the
    null value from Java has two main reasons:
    - avoid matching a NullParameter instance as a non-initialized ParameterLeaf
    - avoid the crash in many methods and using okhttp caused by NullPointerExceptions
     */

    public NullParameter(NullParameter other) {
        super(other);
        this.type = ParameterType.UNKNOWN;
        this.value = "null";
    }

    public NullParameter(Parameter source) {
        super(source);
        this.type = ParameterType.UNKNOWN;
        this.value = "null";
    }

    public NullParameter(JsonElement jsonElement, String name) {
        super();
        this.type = ParameterType.UNKNOWN;
        setValue(null);

        this.name = new ParameterName(Objects.requireNonNullElse(name, ""));
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public NullParameter merge(Parameter other) {
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
}
