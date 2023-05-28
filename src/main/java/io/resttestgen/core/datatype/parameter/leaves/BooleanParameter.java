package io.resttestgen.core.datatype.parameter.leaves;

import com.google.gson.JsonPrimitive;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.helper.ObjectHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Objects;

public class BooleanParameter extends LeafParameter {

    private static final Logger logger = LogManager.getLogger(BooleanParameter.class);

    public BooleanParameter(Map<String, Object> parameterMap, String name) {
        super(parameterMap, name);
    }

    public BooleanParameter(Map<String, Object> parameterMap) {
        this(parameterMap, null);
    }

    public BooleanParameter(BooleanParameter other) {
        super(other);
    }

    public BooleanParameter(LeafParameter source) {
        super(source);
        this.type = ParameterType.BOOLEAN;
        this.value = "null";
    }

    public BooleanParameter(Parameter source) {
        super(source);
    }

    public BooleanParameter(JsonPrimitive jsonPrimitive, String name) {
        super();
        setValue(jsonPrimitive.getAsBoolean());

        this.name = new ParameterName(Objects.requireNonNullElse(name, ""));
    }

    public BooleanParameter() {
        super();
        this.type = ParameterType.BOOLEAN;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public BooleanParameter merge(Parameter other) {
        // No additional behavior/constraints in boolean parameter
        return new BooleanParameter(this);
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        return o != null && (o instanceof BooleanParameter || Boolean.class.isAssignableFrom(o.getClass()));
    }

    @Override
    public boolean isValueCompliant(Object value) {
        if (value instanceof LeafParameter) {
            value = ((LeafParameter) value).getConcreteValue();
        }
        try {
            boolean booleanValue = ObjectHelper.castToBoolean(value);
            if (getEnumValues().size() == 0 || getEnumValues().contains(booleanValue)) {
                return true;
            }
        } catch (ClassCastException ignored) {}
        return false;
    }

    @Override
    public String getJSONString() {
        return getJSONHeading() + getConcreteValue().toString();
    }

    @Override
    public BooleanParameter deepClone() {
        return new BooleanParameter(this);
    }
}
