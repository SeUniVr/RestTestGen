package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterFactory;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TypeRule extends Rule {

    private final ParameterType parameterType;
    private final ParameterValueProvider valueProvider = ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM);

    public TypeRule(ParameterName parameterName, ParameterType parameterType) {
        super(RuleType.TYPE, parameterName);
        this.parameterType = parameterType;
    }

    public ParameterType getParameterType() {
        return parameterType;
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        return true;
    }

    // TODO: test this method
    @Override
    public void apply(Operation operation) {

        // "type: object" is not applicable, because we don't have information about object's properties
        if (parameterType == ParameterType.OBJECT) {
            return;
        }

        if (!getParametersInOperation(operation).isEmpty()) {
            Parameter parameter = getParametersInOperation(operation).get(0);

            // If the parameter is of the same type already, return
            if (parameter.getType() == parameterType ||
                    parameter.getType() == ParameterType.INTEGER && parameterType == ParameterType.NUMBER ||
                    parameter.getType() == ParameterType.NUMBER && parameterType == ParameterType.INTEGER) {
                return;
            }

            // Create new parameter with new type
            Parameter newParameter = ParameterFactory.getParameterOfType(parameterType);
            if (newParameter == null) {
                return;
            }

            newParameter.setName(parameter.getName());


            // Replace the parameter
            parameter.replace(newParameter);

            // In case of arrays, set the array reference element to the old parameter
            if (parameterType == ParameterType.ARRAY) {
                ((ArrayParameter) newParameter).setReferenceElement(parameter);
            }
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (!getParametersInOperation(operation).isEmpty()) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            return parameter.getType() == parameterType;
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
    protected Set<Rule> performFineValidationRoutine(TestSequence sequence, Set<Rule> removedRules) {
        TestSequence clonedSequence = sequence.deepClone().reset();
        Operation operation = clonedSequence.getFirst().getFuzzedOperation();
        List<Parameter> parameters = getParametersInOperation(operation);
        if (!parameters.isEmpty()) {
            Parameter parameter = parameters.get(0);

            // Object type can be only applied to objects
            // FIXME: move this in coarse static validation
            if (parameter.getType() != ParameterType.OBJECT && parameterType == ParameterType.OBJECT) {
                return Set.of();
            }

            if (parameter.getType() == parameterType) {
                return Set.of(this);
            }

            Parameter thisTypeParameter = ParameterFactory.getParameterOfType(parameterType);
            if (thisTypeParameter == null) {
                return Set.of();
            }

            thisTypeParameter.setName(parameter.getName());
            // support for leaves only at the moment
            if (thisTypeParameter instanceof LeafParameter) {
                ((LeafParameter) thisTypeParameter).setValueWithProvider(valueProvider);
                parameter.replace(thisTypeParameter);

                if (playSequence(clonedSequence).isPass()) {
                    return Set.of(this);
                }
            }
        }

        return Set.of();
    }

    @Override
    public String getValueAsString() {
        return parameterType.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeRule that = (TypeRule) o;
        return Objects.equals(parameterNames, that.parameterNames) && parameterType == that.parameterType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames, parameterType);
    }
}