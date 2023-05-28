package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.implementation.helper.InterParameterDependenciesHelper;
import io.resttestgen.implementation.parametervalueprovider.multi.EnumAndExamplePriorityParameterValueProvider;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RequiresRule extends Rule {

    private final String condition;
    private final String statement;

    public RequiresRule(String condition, String statement) {
        super(RuleType.REQUIRES, new HashSet<>());
        this.condition = condition;
        this.statement = statement;

        // Add the parameter name in the condition to the set of parameter names
        if (condition.contains("==")) {
            parameterNames.add(new ParameterName(condition.split("==")[0].trim()));
        } else {
            parameterNames.add(new ParameterName(condition.trim()));
        }

        // Add the parameter name in the statement to the set of parameter names
        if (statement.contains("==")) {
            parameterNames.add(new ParameterName(statement.split("==")[0].trim()));
        } else {
            parameterNames.add(new ParameterName(statement.trim()));
        }
    }

    public String getCondition() {
        return condition;
    }

    public String getStatement() {
        return statement;
    }

    public boolean isSupportedByNominalFuzzer() {
        return !condition.contains("Or(") && !condition.contains("OnlyOne(") && !condition.contains("ZeroOrOne(") && !condition.contains("AllOrNone(") &&
                !statement.contains("Or(") && !statement.contains("OnlyOne(") && !statement.contains("ZeroOrOne(") && !statement.contains("AllOrNone(");
    }

    @Override
    public String getSmtFormula() {
        return null;
    }

    /**
     * With the current implementation we can not check if the rule is applicable, so we apply it in any case.
     * @param operation the operation to which the rule should be applied.
     * @param combination the combination to which the rule belongs.
     * @return true.
     */
    @Override
    public boolean isApplicable(Operation operation, List<Rule> combination) {
        return true;
    }

    @Override
    public void apply(Operation operation) {
        operation.getRequires().add(new Pair<>(condition, statement));
    }

    @Override
    public boolean isApplied(Operation operation) {
        return false;
    }

    @NotNull
    @Override
    public List<Pair<TestSequence, Boolean>> getFineValidationData(TestSequence coarseValidatedTestSequence) {

        List<Pair<TestSequence, Boolean>> fineValidationData = new LinkedList<>();

        TestSequence fineValidationTestSequence = coarseValidatedTestSequence.deepClone();
        fineValidationTestSequence.reset();
        Operation fineValidationOperation = fineValidationTestSequence.getFirst().getFuzzedOperation();

        InterParameterDependenciesHelper ipdHelper = new InterParameterDependenciesHelper(fineValidationOperation, new EnumAndExamplePriorityParameterValueProvider());
        ipdHelper.applyStatementToOperation(condition);
        ipdHelper.applyStatementToOperation(statement);
        fineValidationData.add(new Pair<>(fineValidationTestSequence, true));

        return fineValidationData;
    }

    @NotNull
    @Override
    public Set<Rule> performFineValidationRoutine(TestSequence coarseValidatedTestSequence, Set<Rule> currentRemovedRules) {

        TestSequence fineValidationStepOneSequence = coarseValidatedTestSequence.deepClone();
        fineValidationStepOneSequence.reset();
        Operation fineValidationStepOneOperation = fineValidationStepOneSequence.getFirst().getFuzzedOperation();
        InterParameterDependenciesHelper stepOneIpdHelper = new InterParameterDependenciesHelper(fineValidationStepOneOperation, new EnumAndExamplePriorityParameterValueProvider());
        stepOneIpdHelper.applyStatementToOperation(condition);
        stepOneIpdHelper.applyStatementToOperation(statement);

        TestSequence fineValidationStepTwoSequence = coarseValidatedTestSequence.deepClone();
        fineValidationStepTwoSequence.reset();
        Operation fineValidationStepTwoOperation = fineValidationStepTwoSequence.getFirst().getFuzzedOperation();
        InterParameterDependenciesHelper stepTwoIpdHelper = new InterParameterDependenciesHelper(fineValidationStepTwoOperation, new EnumAndExamplePriorityParameterValueProvider());
        stepTwoIpdHelper.applyStatementToOperation(condition);
        stepTwoIpdHelper.applyNegationOfStatementToOperation(statement);

        testRunner.run(fineValidationStepOneSequence);
        testRunner.run(fineValidationStepTwoSequence);

        boolean validated = fineValidationStepOneSequence.getFirst().getResponseStatusCode().isSuccessful() &&
                !fineValidationStepTwoSequence.getFirst().getResponseStatusCode().isSuccessful();

        return validated ? Set.of(this) : Set.of();
    }

    @Override
    public String getValueAsString() {
        return "IF " + condition + " THEN " + statement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequiresRule that = (RequiresRule) o;
        return Objects.equals(condition, that.condition) && Objects.equals(statement, that.statement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleType, condition, statement);
    }
}