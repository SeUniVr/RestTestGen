package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ExclusiveMinimumRule extends Rule {

    private final boolean exclusiveMinimum;

    public ExclusiveMinimumRule(ParameterName parameterName, boolean exclusiveMinimum) {
        super(RuleType.EXCLUSIVE_MINIMUM, parameterName);
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public boolean isExclusiveMinimum() {
        return exclusiveMinimum;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        // FIXME: implement
        return true;
    }

    @Override
    public void apply(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof NumberParameter) {
                ((NumberParameter) parameter).setExclusiveMinimum(exclusiveMinimum);
            }
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof NumberParameter) {
                return ((NumberParameter) parameter).isExclusiveMaximum() == exclusiveMinimum;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence) {
        return new LinkedList<>();
    }

    @NotNull
    @Override
    public Set<Rule> performFineValidationRoutine(TestSequence coarseValidatedTestSequence, Set<Rule> currentRemovedRules) {
        return Set.of(this);
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(exclusiveMinimum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExclusiveMinimumRule that = (ExclusiveMinimumRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && exclusiveMinimum == that.exclusiveMinimum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, exclusiveMinimum);
    }
}