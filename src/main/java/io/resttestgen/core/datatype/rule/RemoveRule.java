package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RemoveRule extends Rule {

    private final boolean remove;

    public RemoveRule(ParameterName parameterName, boolean remove) {
        super(RuleType.REMOVE, parameterName);
        this.remove = remove;
    }

    public boolean getRemove() {
        return remove;
    }

    @Override
    public String getSmtFormula() {
        return "";
    }

    /**
     * This rule is only applicable if the operation has a parameter with the provided name, and if in the combination
     * there are not required rules for the same parameter.
     * @param operation the operation to which the rule should be applied.
     * @param combination the combination to which the rule belongs.
     * @return true if the rule is applicable.
     */
    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        return !getParametersInOperation(operation).isEmpty() &&
                combination.stream().noneMatch(r -> r.getRuleType() == RuleType.REQUIRED && r.getParameterNames().containsAll(parameterNames));
    }

    @Override
    public void apply(Operation operation) {
        if (!getParametersInOperation(operation).isEmpty()) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (!parameter.isRequired()) {
                parameter.remove();
            }
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (getParametersInOperation(operation).isEmpty()) {
            return true;
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
        return String.valueOf(remove);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoveRule that = (RemoveRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && remove == that.remove;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, remove);
    }
}