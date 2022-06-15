package io.resttestgen.core.operationdependencygraph;

import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import org.jgrapht.graph.DefaultEdge;

public class DependencyEdge extends DefaultEdge {

    private DependencyType dependencyType;
    private NormalizedParameterName normalizedName;
    private ParameterElement producedParameter;
    private ParameterElement consumedParameter;
    private boolean satisfied;

    public DependencyEdge(ParameterElement producedParameter, ParameterElement consumedParameter) {
        if (producedParameter.getNormalizedName().equals(consumedParameter.getNormalizedName())) {
            this.normalizedName = producedParameter.getNormalizedName();
            this.producedParameter = producedParameter;
            this.consumedParameter = consumedParameter;
            this.satisfied = false;
        }
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public NormalizedParameterName getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(NormalizedParameterName normalizedName) {
        this.normalizedName = normalizedName;
    }

    public ParameterElement getProducedParameter() {
        return producedParameter;
    }

    public void setProducedParameter(ParameterElement producedParameter) {
        this.producedParameter = producedParameter;
    }

    public ParameterElement getConsumedParameter() {
        return consumedParameter;
    }

    public void setConsumedParameter(ParameterElement consumedParameter) {
        this.consumedParameter = consumedParameter;
    }

    public void setAsSatisfied() {
        satisfied = true;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    @Override
    public String toString() {
        return this.normalizedName.toString();
    }
}
