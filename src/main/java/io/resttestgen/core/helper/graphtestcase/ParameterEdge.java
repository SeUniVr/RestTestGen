package io.resttestgen.core.helper.graphtestcase;

import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;

public class ParameterEdge {

    private static final Long operationOrder = 1468164518671654465L;
    ParameterName parameterName;
    NormalizedParameterName normalizedParameterName;
    Object value;
    int weight;
    String style;

    public ParameterEdge(LeafParameter producedLeaf, LeafParameter consumedLeaf) {

        if (producedLeaf.getName().equals(consumedLeaf.getName())) {
            this.parameterName = producedLeaf.getName();
        } else {
            if (consumedLeaf.getName() != null) {
                this.parameterName = consumedLeaf.getName();
            } else if (producedLeaf.getName() != null) {
                this.parameterName = producedLeaf.getName();
            } else {
                throw new RuntimeException("No normalized name available");
            }
        }

        if (producedLeaf.getNormalizedName().equals(consumedLeaf.getNormalizedName())) {
            this.normalizedParameterName = producedLeaf.getNormalizedName();
        } else {
            if (consumedLeaf.getNormalizedName() != null) {
                this.normalizedParameterName = consumedLeaf.getNormalizedName();
            } else if (producedLeaf.getNormalizedName() != null) {
                this.normalizedParameterName = producedLeaf.getNormalizedName();
            } else {
                throw new RuntimeException("No normalized name available");
            }
        }

        this.value = producedLeaf.getValue();

        this.weight = 1;

        this.style = "solid";
    }

    public ParameterEdge() {
        this.parameterName = new ParameterName("");
        this.normalizedParameterName = new NormalizedParameterName("");
        this.value = operationOrder;
        this.weight = 500;
        this.style = "dashed";
    }

    @Override
    public String toString() {
        if (value == operationOrder) {
            return "";
        }
        return normalizedParameterName + " = " + value;
    }
}
