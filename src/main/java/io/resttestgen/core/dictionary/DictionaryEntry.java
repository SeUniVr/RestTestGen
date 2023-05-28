package io.resttestgen.core.dictionary;

import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * An entry for the dictionary.
 */
public class DictionaryEntry {

    private ParameterName parameterName;
    private NormalizedParameterName normalizedParameterName;
    private ParameterType type;
    private LeafParameter source;
    private Timestamp discoveryTime;
    private Object value;

    public DictionaryEntry(LeafParameter leaf) {
        if (leaf.getName() != null && leaf.getNormalizedName() != null && leaf.getType() != null &&
                leaf.getOperation() != null && leaf.getValue() != null) {
            this.parameterName = leaf.getName();
            this.normalizedParameterName = leaf.getNormalizedName();
            this.type = leaf.getType();
            this.source = leaf;
            this.discoveryTime = Timestamp.from(Instant.now());
            this.value = leaf.getValue();
        } else {
            throw new RuntimeException("Can not create dictionary entry from leaf with some null values.");
        }
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

    public LeafParameter getSource() {
        return source;
    }

    public void setSource(LeafParameter source) {
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
