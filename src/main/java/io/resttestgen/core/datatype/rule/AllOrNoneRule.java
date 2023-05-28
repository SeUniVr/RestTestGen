package io.resttestgen.core.datatype.rule;

import com.google.common.collect.Sets;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AllOrNoneRule extends Rule {

    public AllOrNoneRule(HashSet<ParameterName> parameterNames) {
        super(RuleType.ALL_OR_NONE, parameterNames);
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    /**
     * This rule is applicable only if one AllOrNone rule is present in the combination. Moreover, other IPDs that
     * operate on set of parameters (such as OnlyOne, etc.) should not operate the same parameters.
     * @param operation the operation to which the rule should be applied.
     * @param combination the combination to which the rule belongs.
     * @return true if the rule is applicable.
     */
    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        if (combination.stream().filter(r -> r.getRuleType() == RuleType.ALL_OR_NONE).count() > 1) {
            return false;
        }

        Set<ParameterName> otherIpdsParameterNames = new HashSet<>();
        combination.stream().filter(r -> r.getRuleType() == RuleType.ONLY_ONE || r.getRuleType() == RuleType.OR ||
                r.getRuleType() == RuleType.ZERO_OR_ONE).forEach(r -> otherIpdsParameterNames.addAll(r.getParameterNames()));

        return Sets.intersection(parameterNames, otherIpdsParameterNames).size() == 0;
    }

    @Override
    public void apply(Operation operation) {
        operation.getAllOrNone().add(parameterNames);
    }

    @Override
    public boolean isApplied(Operation operation) {
        return operation.getAllOrNone().contains(parameterNames);
    }

    @NotNull
    @Override
    public List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence) {
        // TODO: implement
        return new LinkedList<>();
    }

    @NotNull
    @Override
    public Set<Rule> performFineValidationRoutine(TestSequence coarseValidatedTestSequence, Set<Rule> currentRemovedRules) {
        return new OrRule(parameterNames).performFineValidationRoutine(coarseValidatedTestSequence, currentRemovedRules);
    }

    @Override
    public String getValueAsString() {
        return parameterNames.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return Objects.equals(parameterNames, that.parameterNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames);
    }
}