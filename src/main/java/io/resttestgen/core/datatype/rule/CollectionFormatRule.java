package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CollectionFormatRule extends Rule {

    private final String collectionFormat;

    public CollectionFormatRule(ParameterName parameterName, String collectionFormat) {
        super(RuleType.COLLECTION_FORMAT, parameterName);
        this.collectionFormat = collectionFormat;
    }

    @Override
    public String getSmtFormula() {
        return "";
    }

    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        Optional<Rule> typeRuleForParameter = combination.stream().filter(r -> r.ruleType == RuleType.TYPE && r.getParameterNames().containsAll(parameterNames)).findFirst();

        // If there is a type rule in the combination for this parameter, then it must say type == array
        if (typeRuleForParameter.isPresent()) {
            return typeRuleForParameter.get().getValueAsString().equalsIgnoreCase("array");
        }

        // If there is no type rule in combination, then the parameter bust be an array
        else {
            if (getParametersInOperation(operation).size() > 0) {
                Parameter parameter = getParametersInOperation(operation).get(0);
                return ParameterUtils.isArray(parameter);
            }
        }

        return false;
    }

    @Override
    public void apply(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            parameter.setStyle(ParameterStyle.FORM);
            parameter.setExplode(false);
        }
    }

    @Override
    public boolean isApplied(Operation operation) {
        if (getParametersInOperation(operation).size() > 0) {
            Parameter parameter = getParametersInOperation(operation).get(0);
            return parameter.getStyle() == ParameterStyle.FORM && !parameter.isExplode();
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
    public Set<Rule> performFineValidationRoutine(TestSequence coarseValidatedTestSequence, Set<Rule> currentRemovedRules) {
        return Set.of(this);
    }

    @Override
    public String getValueAsString() {
        return collectionFormat;
    }
}