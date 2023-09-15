package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestResult;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.implementation.oracle.StatusCodeOracle;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Rule {
    private final Logger logger = LogManager.getLogger(Rule.class);
    protected final RuleType ruleType;
    protected final HashSet<ParameterName> parameterNames = new HashSet<>();
    protected static final TestRunner testRunner = TestRunner.getInstance();
    protected static final StatusCodeOracle statusCodeOracle = new StatusCodeOracle();

    public Rule(RuleType type, ParameterName parameterName) {
        this.ruleType = type;
        this.parameterNames.add(parameterName);
    }

    public Rule(RuleType type, HashSet<ParameterName> parameterNames) {
        this.ruleType = type;
        this.parameterNames.addAll(parameterNames);
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public HashSet<ParameterName> getParameterNames() {
        return parameterNames;
    }

    public boolean isPresenceRule() {
        return ruleType == RuleType.REMOVE || ruleType == RuleType.REQUIRED;
    }

    public boolean isConstraintRule() {
        return ruleType == RuleType.TYPE || ruleType == RuleType.FORMAT ||
                ruleType == RuleType.MAXIMUM || ruleType == RuleType.MINIMUM || ruleType == RuleType.EXCLUSIVE_MAXIMUM ||
                ruleType == RuleType.EXCLUSIVE_MINIMUM;
    }

    public boolean isSingleRule() {
        return ruleType == RuleType.DEFAULT || ruleType == RuleType.COLLECTION_FORMAT;
    }

    public boolean isSetIpdRule() {
        return ruleType == RuleType.ALL_OR_NONE || ruleType == RuleType.ONLY_ONE || ruleType == RuleType.OR || ruleType == RuleType.ZERO_OR_ONE;
    }

    public boolean requiresCombinatorialValidation() {
        return ruleType == RuleType.REQUIRES;
    }

    public boolean isAlwaysApplicable() {
        return ruleType == RuleType.EXAMPLE || ruleType == RuleType.ENUM;
    }

    public boolean canBeFineValidated() {
        // FIXME: support also max, min, exclMax, exclMin
        return ruleType == RuleType.DEFAULT || ruleType == RuleType.ENUM || ruleType == RuleType.EXAMPLE ||
                ruleType == RuleType.REQUIRED;
    }

    public abstract String getSmtFormula();

    /**
     * Checks if a rule is applicable to an operation, within a given combination.
     *
     * @param operation   the operation to which the rule should be applied.
     * @param combination the combination to which the rule belongs.
     * @return true if the rule is applicable.
     */
    public abstract boolean isApplicable(Operation operation, List<Rule> combination);

    /**
     * Applies the rule to an operation.
     *
     * @param operation to which apply the rule.
     */
    public abstract void apply(Operation operation);

    /**
     * Some rules might require to be applied differently for coarse validation. These rules, like the default rule,
     * can override this method.
     *
     * @param operation to which apply the rule.
     */
    public void applyForCoarseValidation(Operation operation) {
        this.apply(operation);
    }

    /**
     * Checks if a rule is applied to an operation.
     *
     * @param operation the operation on which the check is performed.
     * @return true if the rule is applied to the operation.
     */
    public abstract boolean isApplied(Operation operation);

    @NotNull
    public abstract List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence);

    @NotNull
    public final Set<Rule> fineValidate(TestSequence sequence, Set<Rule> removedRules) {
        if (!playSequence(sequence).isPass()) {
            logger.warn("Sequence did not pass pre-run tests.");
            return Set.of();
        }

        Set<Rule> matchedRules = performFineValidationRoutine(sequence, removedRules);

        if (!playSequence(sequence).isPass()) {
            logger.warn("Sequence did not pass post-run tests.");
            return Set.of();
        }

        return matchedRules;
    }

    protected abstract Set<Rule> performFineValidationRoutine(TestSequence sequence, Set<Rule> removedRules);

    public abstract String getValueAsString();

    protected TestResult playSequence(TestSequence sequence) {
        TestSequence clonedSequence = sequence.deepClone().reset();
        testRunner.run(clonedSequence);
        return statusCodeOracle.assertTestSequence(clonedSequence);
    }

    @Override
    public String toString() {
        return parameterNames + " --> " + ruleType + ": " + getValueAsString();
    }

    @NotNull
    public List<Parameter> getParametersInOperation(Operation operation) {
        List<Parameter> parameters = new LinkedList<>();
        for (ParameterName parameterName : parameterNames) {
            for (Parameter element : operation.getAllRequestParameters().stream().filter(p -> !(p.getParent() instanceof ArrayParameter)).collect(Collectors.toList())) {
                if (element.getName().equals(parameterName)) {
                    parameters.add(element);
                    break;
                }
            }
        }
        return parameters;
    }

    protected boolean executeFineValidationTestSequence(TestSequence fineValidationTestSequence) {
        testRunner.run(fineValidationTestSequence);
        return fineValidationTestSequence.isExecuted() && (fineValidationTestSequence.getFirst().getResponseStatusCode().isSuccessful() ||
                fineValidationTestSequence.getFirst().getResponseStatusCode().isServerError());
    }
}