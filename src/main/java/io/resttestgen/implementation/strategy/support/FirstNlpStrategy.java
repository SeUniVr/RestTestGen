package io.resttestgen.implementation.strategy.support;

import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Strategy;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.implementation.fuzzer.NominalFuzzer;

@SuppressWarnings("unused")
public class FirstNlpStrategy extends Strategy {

    private final TestRunner testRunner = TestRunner.getInstance();

    @Override
    public void start() {
        /*
        System.out.println("NLP finished. Starting validation.");

        // For each operation, perform dynamic validation of NLP extracted rules
        for (Operation operation : Environment.getInstance().getOpenAPI().getOperations()) {

            TestSequence successfulSequence = null;
            Set<Rule> rulesToValidate = operation.getRulesToValidate();
            Set<Rule> presenceAndConstraintRulesToValidate = rulesToValidate.stream()
                    .filter(r -> r.isPresenceRule() || r.isConstraintRule()).collect(Collectors.toSet());
            Set<Rule> valueRulesToValidate = rulesToValidate.stream().filter(Rule::isValueRule)
                    .collect(Collectors.toSet());
            int combinationSize = presenceAndConstraintRulesToValidate.size();

            // While there exists a new combination with at least one rule
            while (successfulSequence == null && combinationSize > 0) {

                Set<Set<Rule>> rulesCombinationsToValidate =
                        Sets.combinations(presenceAndConstraintRulesToValidate, combinationSize);

                for (Set<Rule> rules : rulesCombinationsToValidate) {

                    Operation clonedOperation = operation.deepClone();

                    List<Rule> orderedCombination = rules.stream()
                            .sorted(Comparator.comparing(Rule::getRuleType))
                            .collect(Collectors.toList());

                    System.out.println("TESTING COMBINATION: " + orderedCombination);

                    boolean runDynamicValidation = true;
                    for (Rule rule : rules) {
                        /*if (!rule.apply(clonedOperation)) {
                            runDynamicValidation = false;
                            System.out.println("COMBINATION DISCARDED STATICALLY");
                            break;
                        }
                    }
                    if (runDynamicValidation) {
                        successfulSequence = runDynamicValidation(clonedOperation);

                        if (successfulSequence != null) {
                            System.out.println("FOUND VALID COMBINATION:" + orderedCombination);

                            // Apply the rules to the original operation
                            operation.applyRules(orderedCombination);
                            break;
                        } else {
                            System.out.println("COMBINATION DISCARDED DYNAMICALLY: " + orderedCombination);
                        }
                    }
                }

                combinationSize--;
            }

            if (successfulSequence != null) {

                // Finally, test value rules individually for fine validation
                List<Rule> fineValidatedRules = new LinkedList<>();

                for (Rule rule : valueRulesToValidate) {

                    boolean fineValidated = false;

                    // Statically remove rules that are not valid
                    List<Operation> fineValidationOperations =
                            rule.getFineValidationOperations(successfulSequence.getFirst().getOperation());

                    if (fineValidationOperations.size() > 0) {
                        fineValidated = true;
                    }

                    for (Operation fineValidationOperation : fineValidationOperations) {
                        // Dynamically fine validate rules
                        if (!runFineValidation(fineValidationOperation)) {
                            fineValidated = false;
                            break;
                        }
                    }

                    if (fineValidated) {
                        fineValidatedRules.add(rule);
                        System.out.println("FINE VALIDATED RULE: " + rule);
                    } else {
                        System.out.println("FINE DISCARDED RULE: " + rule);
                    }

                }
                operation.applyRules(fineValidatedRules);
            }
        }*/

        //System.out.println("NOW RUNNING NOMINAL AND ERROR TESTING.");

        // Finally, launch nominal and error testing strategy with the augmented specification
        //NominalAndErrorStrategy nominalAndErrorStrategy = new NominalAndErrorStrategy();
        //nominalAndErrorStrategy.start();
    }

    /**
     * Executes dynamic validation for an operation with a combination of rules applied.
     * @param operation the operation to fuzz.
     * @return true if the fuzzed operation could obtain a successful status code within a limited number of attempts.
     */
    private TestSequence runDynamicValidation(Operation operation) {

        int dynamicValidationAttempts = 20;

        for (int i = 0; i < dynamicValidationAttempts; i++) {
            NominalFuzzer nominalFuzzer = new NominalFuzzer(operation);
            nominalFuzzer.setStrict(true);
            TestSequence validationSequence = nominalFuzzer.generateTestSequences(1).get(0);
            testRunner.run(validationSequence);
            if (validationSequence.isExecuted() && validationSequence.get(0).getResponseStatusCode().isSuccessful()) {
                return validationSequence;
            }
        }
        return null;
    }

    private boolean runFineValidation(Operation operation) {
        TestSequence testSequence = new TestSequence();
        testSequence.add(new TestInteraction(operation));
        testRunner.run(testSequence);
        return testSequence.isExecuted() && testSequence.get(0).getResponseStatusCode().isSuccessful();
    }
}
