package io.resttestgen.core.datatype;

import java.util.Objects;

public class ParameterName {

    private final String parameterName;

    public ParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    private ParameterName(ParameterName other) {
        parameterName = other.parameterName;
    }

    public ParameterName deepClone() {
        return new ParameterName(this);
    }

    public boolean contains(String s) {
        return parameterName.toLowerCase().contains(s.toLowerCase());
    }

    public boolean startsWith(String prefix) {
        return parameterName.toLowerCase().startsWith(prefix.toLowerCase());
    }

    public boolean endsWith(String suffix) {
        return parameterName.toLowerCase().endsWith(suffix.toLowerCase());
    }

    @Override
    public String toString() {
        return parameterName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterName)) return false;
        ParameterName that = (ParameterName) o;
        return Objects.equals(parameterName, that.parameterName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterName);
    }
}
