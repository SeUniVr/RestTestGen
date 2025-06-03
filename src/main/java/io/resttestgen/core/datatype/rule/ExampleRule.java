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

public class ExampleRule extends Rule {

    public final Object exampleValue;

    public ExampleRule(ParameterName parameterName, Object exampleValue) {
        super(RuleType.EXAMPLE, parameterName);
        this.exampleValue = exampleValue;
    }

    public Object getExampleValue() {
        return exampleValue;
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
        if (!getParametersInOperation(operation).isEmpty()) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof LeafParameter) {
                return parameter.isObjectTypeCompliant(exampleValue);
            }
        }
        return false;
    }

    @Override
    public void apply(Operation operation) {
        if (!getParametersInOperation(operation).isEmpty()) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof LeafParameter) {
                parameter.addExample(exampleValue);
            }

            // In case the rule refers to an array, apply example to the reference element
            else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                ((ArrayParameter) parameter).getReferenceElement().addExample(exampleValue);
            }
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (!getParametersInOperation(operation).isEmpty()) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            if (parameter instanceof LeafParameter) {
                return parameter.getExamples().stream().anyMatch(v -> v.toString().equals(exampleValue.toString()));
            }

            // In case the rule refers to an array, check if the example is applied to the reference element
            else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                return ((ArrayParameter) parameter).getReferenceElement().getExamples().stream().anyMatch(v -> v.toString().equals(exampleValue.toString()));
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

        if (!getParametersInOperation(fineValidationOperation).isEmpty()) {
            Parameter parameter = getParametersInOperation(fineValidationOperation).get(0);
            if (parameter instanceof LeafParameter) {
                try {
                    Object castedValue = ObjectHelper.castToParameterValueType(exampleValue, parameter.getType());
                    ((LeafParameter) parameter).setValueManually(castedValue);
                    fineValidationData.add(new Pair<>(fineValidationTestSequence, true));
                } catch (ClassCastException ignored) {}
            } else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                try {
                    LeafParameter newElement = (LeafParameter) ((ArrayParameter) parameter).getReferenceElement();
                    Object castedValue = ObjectHelper.castToParameterValueType(exampleValue, newElement.getType());
                    newElement.setValueManually(castedValue);
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

        if (!getParametersInOperation(fineValidationOperation).isEmpty()) {
            Parameter parameter = getParametersInOperation(fineValidationOperation).get(0);
            if (parameter instanceof LeafParameter) {
                try {
                    Object castedValue = ObjectHelper.castToParameterValueType(exampleValue, parameter.getType());
                    ((LeafParameter) parameter).setValueManually(castedValue);
                } catch (ClassCastException ignored) {}
            } else if (ParameterUtils.isArrayOfLeaves(parameter)) {
                try {
                    LeafParameter newElement = (LeafParameter) ((ArrayParameter) parameter).getReferenceElement();
                    Object castedValue = ObjectHelper.castToParameterValueType(exampleValue, newElement.getType());
                    newElement.setValueManually(castedValue);
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
        return exampleValue.toString().replace("\n", "\\n");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExampleRule that = (ExampleRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && Objects.equals(exampleValue, that.exampleValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, exampleValue);
    }
}