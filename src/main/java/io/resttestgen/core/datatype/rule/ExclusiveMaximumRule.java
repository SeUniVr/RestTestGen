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

public class ExclusiveMaximumRule extends Rule {

    private final boolean exclusiveMaximum;

    public ExclusiveMaximumRule(ParameterName parameterName, boolean exclusiveMaximum) {
        super(RuleType.EXCLUSIVE_MAXIMUM, parameterName);
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public boolean isExclusiveMaximum() {
        return exclusiveMaximum;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    /**
     * This rule is applicable to number parameters, or if in the combination there is a rule to set the parameter as
     * number parameter. Moreover, the parameter must have the maximum value set. Of course a parameter with the given
     * name must be present in the operation.
     * @param operation the operation to which the rule should be applied.
     * @param combination the combination to which the rule belongs.
     * @return true if the rule is applicable.
     */
    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        // FIXME: implement
        return true;
    }

    @Override
    public void apply(Operation operation) {
        if (!getParametersInOperation(operation).isEmpty()) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof NumberParameter) {
                ((NumberParameter) parameter).setExclusiveMaximum(exclusiveMaximum);
            }
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (!getParametersInOperation(operation).isEmpty()) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof NumberParameter) {
                return ((NumberParameter) parameter).isExclusiveMaximum() == exclusiveMaximum;
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
        return String.valueOf(exclusiveMaximum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExclusiveMaximumRule that = (ExclusiveMaximumRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && exclusiveMaximum == that.exclusiveMaximum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, exclusiveMaximum);
    }
}