package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.implementation.parametervalueprovider.single.RandomParameterValueProvider;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FormatRule extends Rule {

    private final ParameterTypeFormat format;
    private static final RandomParameterValueProvider randomValueProvider = new RandomParameterValueProvider();

    public FormatRule(ParameterName parameterName, ParameterTypeFormat format) {
        super(RuleType.FORMAT, parameterName);
        this.format = format;
    }

    public ParameterTypeFormat getFormat() {
        return format;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        List<Parameter> parameters = getParametersInOperation(operation);
        return parameters.size() > 0
                && parameters.stream().allMatch(p -> format.isCompatibleWithType(p.getType()));
    }

    @Override
    public void apply(Operation operation) {
        List<Parameter> parameters = getParametersInOperation(operation);

        if (parameters.size() > 0) {
            Parameter parameter = parameters.get(0);
            if (format.isCompatibleWithType(parameter.getType())) {
                parameter.setFormat(format);
            }
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            return parameter.getFormat() == format;
        }
        return false;
    }

    @NotNull
    @Override
    public List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence) {
        return new LinkedList<>();
    }

    @Override
    protected Set<Rule> performFineValidationRoutine(TestSequence sequence, Set<Rule> removedRules) {
        TestSequence clonedSequence = sequence.deepClone().reset();
        Operation operation  = clonedSequence.getFirst().getFuzzedOperation();
        List<Parameter> parameters = getParametersInOperation(operation);

        if (parameters.size() > 0) {
            Parameter firstOne = parameters.get(0);
            if (!format.isCompatibleWithType(firstOne.getType()) || !(firstOne instanceof LeafParameter)) {
                return Set.of();
            }

            firstOne.setFormat(format);
            Object value = randomValueProvider.provideValueFor((LeafParameter) firstOne);
            ((LeafParameter) firstOne).setValue(value);
            if (playSequence(clonedSequence).isPass()) {
                return Set.of(this);
            }
        }

        return Set.of();
    }

    @Override
    public String getValueAsString() {
        return format.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormatRule that = (FormatRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && format == that.format;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, format);
    }
}