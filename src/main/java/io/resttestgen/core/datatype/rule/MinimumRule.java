package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.helper.DomainExplorer;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MinimumRule extends Rule {

    private final double minimum;

    public MinimumRule(ParameterName parameterName, double minimum) {
        super(RuleType.MAXIMUM, parameterName);
        this.minimum = minimum;
    }

    public double getMinimum() {
        return minimum;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        List<Parameter> parameters = getParametersInOperation(operation);
        return parameters.size() > 0 && parameters.stream()
                .allMatch(p -> p instanceof NumberParameter || p instanceof StringParameter || p instanceof ArrayParameter);
    }

    @Override
    public void apply(Operation operation) {
        getParametersInOperation(operation).forEach(this::applyToParameter);
    }

    public void applyToParameter(Parameter parameter) {
        if (parameter instanceof NumberParameter) {
            ((NumberParameter) parameter).setMinimum(minimum);
        } else if (parameter instanceof StringParameter) {
            ((StringParameter) parameter).setMinLength((int) minimum);
        } else if (parameter instanceof ArrayParameter) {
            ((ArrayParameter) parameter).setMinItems((int) minimum);
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        List<Parameter> parameters = getParametersInOperation(operation);
        if (parameters.size() > 0) {
            Parameter parameter = parameters.get(0);
            return isAppliedToParameter(parameter);
        }
        return false;
    }

    private boolean isAppliedToParameter(Parameter parameter) {
        return (parameter instanceof NumberParameter &&
                    ((NumberParameter) parameter).getMinimum() != null &&
                    ((NumberParameter) parameter).getMinimum() == minimum) ||
                (parameter instanceof StringParameter &&
                        ((StringParameter) parameter).getMinLength() != null &&
                        ((StringParameter) parameter).getMinLength() == (int) minimum) ||
                (parameter instanceof ArrayParameter &&
                        ((ArrayParameter) parameter).getMinItems() != null &&
                        ((ArrayParameter) parameter).getMinItems() == (int) minimum);
    }

    @NotNull
    @Override
    public List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence) {

        List<Pair<TestSequence, Boolean>> fineValidationData = new LinkedList<>();

        TestSequence minValueTestSequence = coarseValidatedTestSequence.deepClone();
        minValueTestSequence.reset();
        Operation minValueOperation = minValueTestSequence.getFirst().getFuzzedOperation();

        List<Parameter> parameters = getParametersInOperation(minValueOperation);

        // At the moment, only number parameter are supported
        if (parameters.size() > 0 && parameters.get(0) instanceof NumberParameter) {

            NumberParameter parameter = (NumberParameter) parameters.get(0);
            parameter.setValue(minimum + 1);
            fineValidationData.add(new Pair<>(minValueTestSequence, true));

            TestSequence minValueMinusOneTestSequence = minValueTestSequence.deepClone();
            Operation minValueMinusOneOperation = minValueMinusOneTestSequence.getFirst().getFuzzedOperation();
            NumberParameter minMinusOneParameter = (NumberParameter) getParametersInOperation(minValueMinusOneOperation).get(0);
            minMinusOneParameter.setValue(minimum - 1);
            fineValidationData.add(new Pair<>(minValueMinusOneTestSequence, false));
        }

        return fineValidationData;
    }

    @Override
    protected Set<Rule> performFineValidationRoutine(TestSequence sequence, Set<Rule> removedRules) {
        TestSequence clonedSequence = sequence.deepClone().reset();
        Operation operation = sequence.getFirst().getFuzzedOperation();
        List<Parameter> parameters = getParametersInOperation(operation);
        if (parameters.size() == 0) {
            return Set.of();
        }

        Parameter parameter = parameters.get(0);
        if (parameter instanceof NumberParameter) {
            NumberParameter asNumber = (NumberParameter) parameter;
            asNumber.setValue(minimum - 1);
            if (playSequence(clonedSequence).isPass()) {
                Number inferredMinimum = DomainExplorer.getMinimumFromDomainExploration(asNumber, minimum, sequence);
                if (inferredMinimum == null) {
                    return Set.of();
                }

                return Set.of(new MinimumRule(parameter.getName(), inferredMinimum.doubleValue()));
            }

            asNumber.setValue(minimum);
            if (playSequence(clonedSequence).isPass()) {
                return Set.of(this);
            }
        } else if (parameter instanceof StringParameter) {
            StringParameter asString = (StringParameter) parameter;
            ExtendedRandom random = Environment.getInstance().getRandom();

            asString.setValue(random.nextString((int) minimum - 1));
            if (playSequence(clonedSequence).isPass()) {
                return Set.of();
            }

            asString.setValue(random.nextString((int) minimum));
            if (playSequence(clonedSequence).isPass()) {
                return Set.of(this);
            }
        } else if (parameter instanceof ArrayParameter) {
            ArrayParameter asArray = (ArrayParameter) parameter;
            asArray.clearElements();
            asArray.addReferenceElements((int) minimum - 1);

            if (playSequence(clonedSequence).isPass()) {
                return Set.of();
            }

            asArray.clearElements();
            asArray.addReferenceElements((int) minimum);
            if (playSequence(clonedSequence).isPass()) {
                return Set.of(this);
            }
        }

        return Set.of();
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(minimum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinimumRule that = (MinimumRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && Double.compare(that.minimum, minimum) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, minimum);
    }
}