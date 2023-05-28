package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ArithmeticRelationalRule extends Rule {

    private final String expression;

    public ArithmeticRelationalRule(HashSet<ParameterName> parameterNames, String expression) {
        super(RuleType.ARITHMETIC_RELATIONAL, parameterNames);
        this.expression = expression;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        // This rule is not supported at the moment, so it can not be applied
        return false;
    }

    @Override
    public void apply(Operation operation) {
        // Not supported at the moment
    }

    @Override
    public boolean isApplied(Operation operation) {
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
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArithmeticRelationalRule that = (ArithmeticRelationalRule) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, expression);
    }
}