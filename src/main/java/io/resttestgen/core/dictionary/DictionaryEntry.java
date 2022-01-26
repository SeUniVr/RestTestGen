package io.resttestgen.core.dictionary;

import io.resttestgen.core.datatype.parameter.ParameterType;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * An entry for the dictionary.
 */
public class DictionaryEntry {

    private ParameterName parameterName;
    private NormalizedParameterName normalizedParameterName;
    private ParameterType type;
    private Operation source; // The operation that generated that value (if null it is a default value)
    private Timestamp discoveryTime;
    private Object value;

    public DictionaryEntry(ParameterName parameterName, Operation source, String value) {
        this.parameterName = parameterName;
        this.normalizedParameterName = new NormalizedParameterName(parameterName);
        this.type = ParameterType.STRING;
        this.source = source;
        this.discoveryTime = Timestamp.from(Instant.now());
        setValue(value);
    }

    public DictionaryEntry(ParameterName parameterName, Operation source, Integer value) {
        this.parameterName = parameterName;
        this.normalizedParameterName = new NormalizedParameterName(parameterName);
        this.type = ParameterType.INTEGER;
        this.source = source;
        this.discoveryTime = Timestamp.from(Instant.now());
        setValue(value);
    }

    public DictionaryEntry(ParameterName parameterName, Operation source, Double value) {
        this.parameterName = parameterName;
        this.normalizedParameterName = new NormalizedParameterName(parameterName);
        this.type = ParameterType.NUMBER;
        this.source = source;
        this.discoveryTime = Timestamp.from(Instant.now());
        setValue(value);
    }

    public DictionaryEntry(ParameterName parameterName, Operation source, Boolean value) {
        this.parameterName = parameterName;
        this.normalizedParameterName = new NormalizedParameterName(parameterName);
        this.type = ParameterType.BOOLEAN;
        this.source = source;
        this.discoveryTime = Timestamp.from(Instant.now());
        setValue(value);
    }

    public ParameterName getParameterName() {
        return parameterName;
    }

    public void setParameterName(ParameterName parameterName) {
        this.parameterName = parameterName;
    }

    public NormalizedParameterName getNormalizedParameterName() {
        return normalizedParameterName;
    }

    public void setNormalizedParameterName(NormalizedParameterName normalizedParameterName) {
        this.normalizedParameterName = normalizedParameterName;
    }

    public ParameterType getParameterType() {
        return type;
    }

    public void setParameterType(ParameterType type) {
        this.type = type;
    }

    public Operation getSource() {
        return source;
    }

    public void setSource(Operation source) {
        this.source = source;
    }

    public Timestamp getDiscoveryTime() {
        return discoveryTime;
    }

    public void setDiscoveryTime(Timestamp discoveryTime) {
        this.discoveryTime = discoveryTime;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "[" + normalizedParameterName + " : " + value + "]";
    }
}
