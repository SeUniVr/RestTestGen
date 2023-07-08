package io.resttestgen.core.testing.coverage;

import io.resttestgen.core.datatype.parameter.Parameter;

import java.util.Objects;

public class ParameterElementWrapper {

    private Parameter parameter;

    public ParameterElementWrapper(Parameter parameter){
        this.parameter = parameter;
    }

    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public boolean equals(Object o) {
        if (this.parameter == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParameterElementWrapper parameterWrapper = (ParameterElementWrapper) o;

        return Objects.equals(this.parameter.getName(), parameterWrapper.parameter.getName()) &&
                Objects.equals(this.parameter.getLocation(), parameterWrapper.parameter.getLocation()) &&
                Objects.equals(this.parameter.getOperation(), parameterWrapper.parameter.getOperation()) &&
                // If even one of the parameters has null parent, then ignore normalized name. Else, consider it.
                // This behaviour is to restrict the most possible the use of normalizedName in equals
                (this.parameter.getParent() == null || parameterWrapper.parameter.getParent() == null || Objects.equals(this.parameter.getNormalizedName(), parameterWrapper.parameter.getNormalizedName()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameter.getName(), parameter.getLocation(), parameter.getOperation());
    }

    @Override
    public String toString() {
        return this.parameter.getName() + " (" + this.parameter.getNormalizedName() + ", " + this.parameter.getLocation() + ")";
    }
}
