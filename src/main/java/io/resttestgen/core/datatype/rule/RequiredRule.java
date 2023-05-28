package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RequiredRule extends Rule {

    private final boolean required;

    public RequiredRule(ParameterName parameterName, boolean required) {
        super(RuleType.REQUIRED, parameterName);
        this.required = required;
    }

    public boolean getRequired() {
        return required;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        return getParametersInOperation(operation).size() > 0 &&
                combination.stream().noneMatch(r -> r.getRuleType() == RuleType.REMOVE && r.getParameterNames().containsAll(parameterNames));
    }

    @Override
    public void apply(Operation operation) {
        List<Parameter> parameters = getParametersInOperation(operation);
        if (parameters.size() > 0) {
            Parameter parameter = parameters.get(0);
            parameter.setRequired(required);
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            return parameter.isRequired() == required;
        }
        return false;
    }

    @NotNull
    @Override
    public List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence) {
        List<Pair<TestSequence, Boolean>> fineValidationData = new LinkedList<>();

        TestSequence fineValidationTestSequence = coarseValidatedTestSequence.deepClone();
        fineValidationTestSequence.reset();
        Operation fineValidationOperation = fineValidationTestSequence.getFirst().getFuzzedOperation();

        List<Parameter> parameters = getParametersInOperation(fineValidationOperation);
        if (parameters.size() > 0) {
            parameters.get(0).remove();
            fineValidationData.add(new Pair<>(fineValidationTestSequence, false));
        }

        return fineValidationData;
    }

    @Override
    protected Set<Rule> performFineValidationRoutine(TestSequence sequence, Set<Rule> removedRules) {
        TestSequence clonedSequence = sequence.deepClone().reset();
        Operation operation = clonedSequence.getFirst().getFuzzedOperation();
        List<Parameter> parameters = getParametersInOperation(operation);

        if (parameters.size() > 0) {
            parameters.forEach(Parameter::remove);

            if (playSequence(clonedSequence).isFail()) {
                if (required) {
                    return Set.of(this);
                }

                ParameterName parameterName = parameterNames.stream().findFirst().orElse(null);
                RequiredRule requiredRule = new RequiredRule(parameterName, true);
                return Set.of(requiredRule);
            }
        }

        return Set.of();
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(required);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequiredRule that = (RequiredRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && required == that.required;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, required);
    }
}