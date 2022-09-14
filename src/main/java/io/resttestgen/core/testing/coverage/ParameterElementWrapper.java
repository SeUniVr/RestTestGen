package io.resttestgen.core.testing.coverage;

import io.resttestgen.core.datatype.parameter.ParameterElement;

import java.util.Objects;

public class ParameterElementWrapper {

    private ParameterElement parameterElement;

    public ParameterElementWrapper(ParameterElement parameterElement){
        this.parameterElement = parameterElement;
    }

    public ParameterElement getParameterElement() {
        return parameterElement;
    }

    @Override
    public boolean equals(Object o) {
        if (this.parameterElement == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParameterElementWrapper parameterWrapper = (ParameterElementWrapper) o;

        return Objects.equals(this.parameterElement.getName(), parameterWrapper.parameterElement.getName()) &&
                Objects.equals(this.parameterElement.getLocation(), parameterWrapper.parameterElement.getLocation()) &&
                Objects.equals(this.parameterElement.getOperation(), parameterWrapper.parameterElement.getOperation()) &&
                // If even one of the parameters has null parent, then ignore normalized name. Else, consider it.
                // This behaviour is to restrict the most possible the use of normalizedName in equals
                (this.parameterElement.getParent() == null || parameterWrapper.parameterElement.getParent() == null || Objects.equals(this.parameterElement.getNormalizedName(), parameterWrapper.parameterElement.getNormalizedName()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterElement.getName(), parameterElement.getLocation(), parameterElement.getOperation());
    }

    @Override
    public String toString() {
        return this.parameterElement.getName() + " (" + this.parameterElement.getNormalizedName() + ", " + this.parameterElement.getLocation() + ")";
    }
}
