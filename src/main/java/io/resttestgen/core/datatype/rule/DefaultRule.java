package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultRule extends Rule {

    private final Object defaultValue;

    public DefaultRule(ParameterName parameterName, Object defaultValue) {
        super(RuleType.DEFAULT, parameterName);
        this.defaultValue = defaultValue;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    /**
     * This rule is applicable only if in the combination this is the only default rule for the parameter, and the
     * provided value if compliant with the parameter type.
     * @param operation the operation to which the rule should be applied.
     * @param combination the combination to which the rule belongs.
     * @return true if the rule is applicable.
     */
    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        if (combination.stream().filter(r -> r.getRuleType() == RuleType.DEFAULT &&
                r.getParameterNames().containsAll(parameterNames)).count() == 1) {
            if (getParametersInOperation(operation).size() > 0) {
                Parameter parameter = getParametersInOperation(operation).get(0);
                if (parameter instanceof LeafParameter) {
                    return parameter.isObjectTypeCompliant(defaultValue);
                }
            }
        }
        return false;
    }

    @Override
    public void apply(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof LeafParameter) {
                parameter.setDefaultValue(defaultValue);
            }

            // In case the rule refers to an array, apply default value to the reference element
            else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                ((ArrayParameter) parameter).getReferenceElement().setDefaultValue(defaultValue);
            }
        }
    }

    @Override
    public void applyForCoarseValidation(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof LeafParameter) {
                parameter.addExample(defaultValue);
            }

            // In case the rule refers to an array, apply default value to the reference element
            else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                ((ArrayParameter) parameter).getReferenceElement().addExample(defaultValue);
            }
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof LeafParameter) {
                return parameter.getDefaultValue() != null && parameter.getDefaultValue().toString().equals(defaultValue.toString());
            }

            // In case the rule refers to an array, check if the default value is applied to the reference element
            else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                return ((ArrayParameter) parameter).getReferenceElement().getDefaultValue() != null &&
                        ((ArrayParameter) parameter).getReferenceElement().getDefaultValue().toString().equals(defaultValue.toString());
            }
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

        if (getParametersInOperation(fineValidationOperation).size() > 0) {
            Parameter parameter = getParametersInOperation(fineValidationOperation).get(0);
            if (parameter instanceof LeafParameter) {
                try {
                    Object castedValue = ObjectHelper.castToParameterValueType(defaultValue, parameter.getType());
                    ((LeafParameter) parameter).setValue(castedValue);
                    fineValidationData.add(new Pair<>(fineValidationTestSequence, true));
                } catch (ClassCastException ignored) {}
            } else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                try {
                    LeafParameter newElement = (LeafParameter) ((ArrayParameter) parameter).getReferenceElement();
                    Object castedValue = ObjectHelper.castToParameterValueType(defaultValue, newElement.getType());
                    newElement.setValue(castedValue);
                    ((ArrayParameter) parameter).clearElements();
                    ((ArrayParameter) parameter).addElement(newElement);
                    fineValidationData.add(new Pair<>(fineValidationTestSequence, true));
                } catch (ClassCastException ignored) {}
            }
        }

        return fineValidationData;
    }

    @NotNull
    @Override
    public Set<Rule> performFineValidationRoutine(TestSequence coarseValidatedTestSequence, Set<Rule> currentRemovedRules) {
        return Set.of(this);
    }

    @Override
    public String getValueAsString() {
        return defaultValue.toString().replace("\n", "\\n");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultRule that = (DefaultRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, defaultValue);
    }
}