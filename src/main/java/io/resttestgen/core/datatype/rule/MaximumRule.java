package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.helper.DomainExplorer;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.implementation.parametervalueprovider.multi.EnumAndExamplePriorityParameterValueProvider;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MaximumRule extends Rule {
    private final double maximum;

    public MaximumRule(ParameterName parameterName, double maximum) {
        super(RuleType.MAXIMUM, parameterName);
        this.maximum = maximum;
    }

    public double getMaximum() {
        return maximum;
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
            ((NumberParameter) parameter).setMaximum(maximum);
        } else if (parameter instanceof StringParameter) {
            ((StringParameter) parameter).setMaxLength((int) maximum);
        } else if (parameter instanceof ArrayParameter) {
            ((ArrayParameter) parameter).setMaxItems((int) maximum);
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
                    ((NumberParameter) parameter).getMaximum() != null &&
                    ((NumberParameter) parameter).getMaximum() == maximum) ||
                (parameter instanceof StringParameter &&
                        ((StringParameter) parameter).getMaxLength() != null &&
                        ((StringParameter) parameter).getMaxLength() == (int) maximum) ||
                (parameter instanceof ArrayParameter &&
                        ((ArrayParameter) parameter).getMaxItems() != null &&
                        ((ArrayParameter) parameter).getMaxItems() == (int) maximum);
    }

    @NotNull
    @Override
    public List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence) {

        List<Pair<TestSequence, Boolean>> fineValidationData = new LinkedList<>();

        TestSequence maxValueTestSequence = coarseValidatedTestSequence.deepClone();
        maxValueTestSequence.reset();
        Operation maxValueOperation = maxValueTestSequence.getFirst().getFuzzedOperation();

        List<Parameter> parameters = getParametersInOperation(maxValueOperation);

        // At the moment, only number parameter are supported
        if (parameters.size() > 0 && parameters.get(0) instanceof NumberParameter) {

            NumberParameter parameter = (NumberParameter) parameters.get(0);
            parameter.setValue(maximum - 1);
            fineValidationData.add(new Pair<>(maxValueTestSequence, true));

            TestSequence maxValuePlusOneTestSequence = maxValueTestSequence.deepClone();
            Operation maxValuePlusOneOperation = maxValuePlusOneTestSequence.getFirst().getFuzzedOperation();
            NumberParameter maxPlusOneParameter = (NumberParameter) getParametersInOperation(maxValuePlusOneOperation).get(0);
            maxPlusOneParameter.setValue(maximum + 1);
            fineValidationData.add(new Pair<>(maxValuePlusOneTestSequence, false));
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
            asNumber.setValue(maximum + 1);
            if (playSequence(clonedSequence).isPass()) {
                Number inferredMaximum = DomainExplorer.getMaximumFromDomainExploration(asNumber, maximum, clonedSequence);
                if (inferredMaximum == null) {
                    return Set.of();
                }
                return Set.of(new MaximumRule(parameter.getName(), inferredMaximum.doubleValue()));
            }

            asNumber.setValue(maximum);
            if (playSequence(clonedSequence).isPass()) {
                return Set.of(this);
            }
        } else if (parameter instanceof StringParameter) {
            StringParameter asString = (StringParameter) parameter;
            ExtendedRandom random = Environment.getInstance().getRandom();

            asString.setValue(random.nextString((int) maximum + 1));
            if (playSequence(clonedSequence).isPass()) {
                return Set.of();
            }

            asString.setValue(random.nextString((int) maximum));
            if (playSequence(clonedSequence).isPass()) {
                return Set.of(this);
            }
        } else if (parameter instanceof ArrayParameter) {
            ArrayParameter asArray = (ArrayParameter) parameter;
            asArray.clearElements();
            asArray.addReferenceElements((int) maximum + 1);

            if (playSequence(clonedSequence).isPass()) {
                return Set.of();
            }

            asArray.clearElements();
            asArray.addReferenceElements((int) maximum);
            if (playSequence(clonedSequence).isPass()) {
                return Set.of(this);
            }
        }

        return Set.of();
    }

    private long getMiddleValue(long lowerBound, long upperBound) {
        if ((lowerBound <= 0 && upperBound <= 0) || (lowerBound >= 0 && upperBound >= 0)) {
            return lowerBound + (upperBound - lowerBound) / 2;
        }

        return (upperBound + lowerBound) / 2;
    }



    @Override
    public String getValueAsString() {
        return String.valueOf(maximum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaximumRule that = (MaximumRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && Double.compare(that.maximum, maximum) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, maximum);
    }
}