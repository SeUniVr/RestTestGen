package io.resttestgen.core.datatype.rule;

import com.google.common.collect.Sets;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class OrRule extends Rule {

    private final static Logger logger = LogManager.getLogger(OrRule.class);

    public OrRule(HashSet<ParameterName> parameterNames) {
        super(RuleType.OR, parameterNames);
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    /**
     * This rule is applicable only if one Or rule is present in the combination. Moreover, other IPDs that operate on
     * set of parameters (such as OnlyOne, etc.) should not operate the same parameters.
     * @param operation the operation to which the rule should be applied.
     * @param combination the combination to which the rule belongs.
     * @return true if the rule is applicable.
     */
    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        if (combination.stream().filter(r -> r.getRuleType() == RuleType.OR).count() > 1) {
            return false;
        }

        Set<ParameterName> otherIpdsParameterNames = new HashSet<>();
        combination.stream().filter(r -> r.getRuleType() == RuleType.ONLY_ONE || r.getRuleType() == RuleType.ALL_OR_NONE ||
                r.getRuleType() == RuleType.ZERO_OR_ONE).forEach(r -> otherIpdsParameterNames.addAll(r.getParameterNames()));

        return Sets.intersection(parameterNames, otherIpdsParameterNames).isEmpty();
    }

    @Override
    public void apply(Operation operation) {
        operation.getOr().add(parameterNames);
    }

    @Override
    public boolean isApplied(Operation operation) {
        return operation.getOr().contains(parameterNames);
    }

    @NotNull
    @Override
    public List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence) {
        return new LinkedList<>();
    }

    @NotNull
    @Override
    public Set<Rule> performFineValidationRoutine(TestSequence coarseValidatedTestSequence, Set<Rule> currentRemovedRules) {

        // At the moment, only rules with two parameters are considered
        if (parameterNames.size() == 2) {

            TestSequence fineValidationTestSequenceAB = coarseValidatedTestSequence.deepClone();
            fineValidationTestSequenceAB.reset();
            Operation fineValidationOperationAB = fineValidationTestSequenceAB.getFirst().getFuzzedOperation();
            List<Parameter> parameters = getParametersInOperation(fineValidationOperationAB);
            Parameter a = parameters.get(0);
            Parameter b = parameters.get(parameters.size() - 1);
            ParameterValueProvider parameterValueProvider = ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM_AND_EXAMPLE_PRIORITY);
            if (!a.isSet()) {
                if (a instanceof LeafParameter) {
                    ((LeafParameter) a).setValueWithProvider(parameterValueProvider);
                } else if (ParameterUtils.isArrayOfLeaves(a)) {
                    ArrayParameter array = (ArrayParameter) a;
                    LeafParameter newElement = (LeafParameter) array.getReferenceElement().deepClone();
                    newElement.setValueWithProvider(parameterValueProvider);
                    array.addElement(newElement);
                }
            }
            if (!b.isSet()) {
                if (b instanceof LeafParameter) {
                    ((LeafParameter) b).setValueWithProvider(parameterValueProvider);
                } else if (ParameterUtils.isArrayOfLeaves(b)) {
                    ArrayParameter array = (ArrayParameter) b;
                    LeafParameter newElement = (LeafParameter) array.getReferenceElement().deepClone();
                    newElement.setValueWithProvider(parameterValueProvider);
                    array.addElement(newElement);
                }
            }

            TestSequence fineValidationTestSequenceANotB = fineValidationTestSequenceAB.deepClone();
            fineValidationTestSequenceANotB.reset();
            Operation fineValidationOperationANotB = fineValidationTestSequenceANotB.getFirst().getFuzzedOperation();
            parameters = getParametersInOperation(fineValidationOperationANotB);
            b = parameters.get(parameters.size() - 1);
            if (b instanceof LeafParameter) {
                ((LeafParameter) b).removeValue();
            } else if (ParameterUtils.isArrayOfLeaves(b)) {
                ((ArrayParameter) b).clearElements();
            }

            TestSequence fineValidationTestSequenceNotAB = fineValidationTestSequenceAB.deepClone();
            fineValidationTestSequenceNotAB.reset();
            Operation fineValidationOperationNotAB = fineValidationTestSequenceNotAB.getFirst().getFuzzedOperation();
            parameters = getParametersInOperation(fineValidationOperationNotAB);
            a = parameters.get(0);
            if (a instanceof LeafParameter) {
                ((LeafParameter) a).removeValue();
            } else if (ParameterUtils.isArrayOfLeaves(a)) {
                ((ArrayParameter) a).clearElements();
            }

            TestSequence fineValidationTestSequenceNotANotB = fineValidationTestSequenceAB.deepClone();
            fineValidationTestSequenceNotANotB.reset();
            Operation fineValidationOperationNotANotB = fineValidationTestSequenceNotANotB.getFirst().getFuzzedOperation();
            parameters = getParametersInOperation(fineValidationOperationNotANotB);
            a = parameters.get(0);
            if (a instanceof LeafParameter) {
                ((LeafParameter) a).removeValue();
            } else if (ParameterUtils.isArrayOfLeaves(a)) {
                ((ArrayParameter) a).clearElements();
            }
            b = parameters.get(parameters.size() - 1);
            if (b instanceof LeafParameter) {
                ((LeafParameter) b).removeValue();
            } else if (ParameterUtils.isArrayOfLeaves(b)) {
                ((ArrayParameter) b).clearElements();
            }

            boolean[] results = new boolean[4];
            results[0] = executeFineValidationTestSequence(fineValidationTestSequenceAB);
            results[1] = executeFineValidationTestSequence(fineValidationTestSequenceANotB);
            results[2] = executeFineValidationTestSequence(fineValidationTestSequenceNotAB);
            results[3] = executeFineValidationTestSequence(fineValidationTestSequenceNotANotB);

            boolean[] or = {true, true, true, false};
            boolean[] onlyOne = {false, true, true, false};
            boolean[] allOneNone = {true, false, false, true};
            boolean[] zeroOrOne = {false, true, true, true};
            boolean[] aRequired = {false, true, false, false};
            boolean[] bRequired = {false, false, true, false};

            if (Arrays.equals(results, or)) {
                return Set.of(new OrRule(parameterNames));
            } else if (Arrays.equals(results, onlyOne)) {
                return Set.of(new OnlyOneRule(parameterNames));
            } else if (Arrays.equals(results, allOneNone)) {
                return Set.of(new AllOrNoneRule(parameterNames));
            } else if (Arrays.equals(results, zeroOrOne)) {
                return Set.of(new ZeroOrOneRule(parameterNames));
            } else if (Arrays.equals(results, aRequired)) {
                HashSet<Rule> result = new HashSet<>();
                result.add(new RequiredRule(a.getName(), true));
                result.add(new RequiredRule(b.getName(), false));
                return result;
            } else if (Arrays.equals(results, bRequired)) {
                HashSet<Rule> result = new HashSet<>();
                result.add(new RequiredRule(a.getName(), false));
                result.add(new RequiredRule(b.getName(), true));
                return result;
            } else {
                return new HashSet<>();
            }
        }

        return Set.of(this);
    }

    @Override
    public String getValueAsString() {
        return parameterNames.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return Objects.equals(parameterNames, that.parameterNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, parameterNames);
    }
}