package io.resttestgen.core.operationdependencygraph;

import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import org.jgrapht.graph.DefaultEdge;

public class DependencyEdge extends DefaultEdge {

    private DependencyType dependencyType;
    private NormalizedParameterName normalizedName;
    private Parameter producedParameter;
    private Parameter consumedParameter;
    private boolean satisfied;

    public DependencyEdge(Parameter producedParameter, Parameter consumedParameter) {
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

    public Parameter getProducedParameter() {
        return producedParameter;
    }

    public void setProducedParameter(Parameter producedParameter) {
        this.producedParameter = producedParameter;
    }

    public Parameter getConsumedParameter() {
        return consumedParameter;
    }

    public void setConsumedParameter(Parameter consumedParameter) {
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
