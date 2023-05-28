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

import java.util.*;

public class EnumRule extends Rule {

    private final Object enumValue;

    public EnumRule(ParameterName parameterName, Object enumValue) {
        super(RuleType.ENUM, parameterName);
        this.enumValue = enumValue;
    }

    public Object getEnumValue() {
        return enumValue;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    /**
     * This rule is only applicable is operation has a parameter with the given name, and if the provided value is
     * compatible with the parameter type.
     * @param operation the operation to which the rule should be applied.
     * @param combination the combination to which the rule belongs.
     * @return true if the rule is applicable.
     */
    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof LeafParameter) {
                return parameter.isObjectTypeCompliant(enumValue);
            }
        }
        return false;
    }

    @Override
    public void apply(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {

            Parameter parameter = getParametersInOperation(operation).get(0);

            if (parameter instanceof LeafParameter) {
                parameter.addEnumValue(enumValue);
            }

            // In case the rule refers to an array, apply example to the reference element
            else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                ((ArrayParameter) parameter).getReferenceElement().addEnumValue(enumValue);
            }
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {

            Parameter parameter = getParametersInOperation(operation).get(0);

            if (parameter instanceof LeafParameter) {
                return parameter.getEnumValues().stream().anyMatch(v -> v.toString().equals(enumValue.toString()));
            }

            // In case the rule refers to an array, check if the example is applied to the reference element
            else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                return ((ArrayParameter) parameter).getReferenceElement().getEnumValues().stream().anyMatch(v -> v.toString().equals(enumValue.toString()));
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
                    Object castedValue = ObjectHelper.castToParameterValueType(enumValue, parameter.getType());
                    ((LeafParameter) parameter).setValue(castedValue);
                    fineValidationData.add(new Pair<>(fineValidationTestSequence, true));
                } catch (ClassCastException ignored) {}
            } else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                try {
                    LeafParameter newElement = (LeafParameter) ((ArrayParameter) parameter).getReferenceElement();
                    Object castedValue = ObjectHelper.castToParameterValueType(enumValue, newElement.getType());
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

        TestSequence fineValidationTestSequence = coarseValidatedTestSequence.deepClone();
        fineValidationTestSequence.reset();
        Operation fineValidationOperation = fineValidationTestSequence.getFirst().getFuzzedOperation();

        if (getParametersInOperation(fineValidationOperation).size() > 0) {
            Parameter parameter = getParametersInOperation(fineValidationOperation).get(0);
            if (parameter instanceof LeafParameter) {
                try {
                    Object castedValue = ObjectHelper.castToParameterValueType(enumValue, parameter.getType());
                    ((LeafParameter) parameter).setValue(castedValue);
                } catch (ClassCastException ignored) {}
            } else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                try {
                    LeafParameter newElement = (LeafParameter) ((ArrayParameter) parameter).getReferenceElement();
                    Object castedValue = ObjectHelper.castToParameterValueType(enumValue, newElement.getType());
                    newElement.setValue(castedValue);
                    ((ArrayParameter) parameter).clearElements();
                    ((ArrayParameter) parameter).addElement(newElement);
                } catch (ClassCastException ignored) {}
            }
            // TODO: apply IPDs
            if (parameter instanceof LeafParameter || ParameterUtils.isArrayOfLeaves(parameter)) {
                testRunner.run(fineValidationTestSequence);
                if (fineValidationTestSequence.isExecuted() && (fineValidationTestSequence.getFirst().getResponseStatusCode().isSuccessful() ||
                        fineValidationTestSequence.getFirst().getResponseStatusCode().isServerError())) {
                    return Set.of(this);
                }
            }
        }

        return new HashSet<>();
    }

    @Override
    public String getValueAsString() {
        return enumValue.toString().replace("\n", "\\n");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumRule enumRule = (EnumRule) o;
        return Objects.equals(parameterNames, enumRule.parameterNames) && Objects.equals(enumValue, enumRule.enumValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, enumValue);
    }
}