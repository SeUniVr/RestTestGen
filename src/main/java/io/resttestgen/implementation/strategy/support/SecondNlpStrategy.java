package io.resttestgen.implementation.strategy.support;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import io.resttestgen.boot.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.rule.*;
import io.resttestgen.core.helper.RuleExtractorProxy;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Strategy;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.operationsorter.OperationsSorter;
import io.resttestgen.implementation.fuzzer.NominalFuzzer;
import io.resttestgen.implementation.operationssorter.GraphBasedOperationsSorter;
import io.resttestgen.implementation.interactionprocessor.NlpInteractionProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SecondNlpStrategy extends Strategy {

    private static final TestRunner testRunner = TestRunner.getInstance();
    private static final NlpInteractionProcessor NLP_INTERACTION_PROCESSOR = new NlpInteractionProcessor();
    private static final Gson gson = new Gson();

    private static final long maxIntervalPerOperation = 360; // Max 120 seconds to validate each operation

    private final HashMap<Operation, Set<Rule>> specificationRules = new HashMap<>();
    private final HashMap<Operation, Set<Rule>> serverMessageRules = new HashMap<>();
    private final HashMap<Operation, Set<Rule>> coarseValidationDiscardedRules = new HashMap<>();
    private final HashMap<Operation, Set<Rule>> fineValidationDiscardedRules = new HashMap<>();
    private final HashMap<String, Map<String, Set<Rule>>> cumulativeResultMap = new HashMap<>();

    @Override
    public void start() {

        for (Operation operation : Environment.getInstance().getOpenAPI().getOperations()) {
            operation.getRulesToValidate().addAll(RuleExtractorProxy.extractRulesFromOperationText(operation));
            operation.getRulesToValidate().addAll(RuleExtractorProxy.extractRulesFromRequestBodyDescription(operation));
            operation.getAllRequestParameters().forEach(p -> operation.getRulesToValidate().addAll(RuleExtractorProxy.extractRulesFromParameterText(p)));
        }

        int totalOperationsCount = Environment.getInstance().getOpenAPI().getOperations().size();
        int validatedOperationsCount = 0;

        System.out.println("NLP finished. Starting validation.");

        RuleExtractorProxy.printStatistics();

        // NLP response processor will identify text in error responses and send it to NLPRestTest
        testRunner.addInteractionProcessor(NLP_INTERACTION_PROCESSOR);

        OperationsSorter sorter = new GraphBasedOperationsSorter();

        nextOperation:
        while (!sorter.isEmpty()) {

            long startTime = Instant.now().getEpochSecond();

            // Get an operation to test
            Operation operation = sorter.getFirst();

            System.out.println("FOUND " + operation.getRulesToValidate().size() + " RULES TO VALIDATE IN OPERATION " + operation);

            // Stores the successful sequence for the operation, initially empty
            TestSequence successfulSequence = null;

            // Initialise sets in maps
            specificationRules.put(operation, new HashSet<>(operation.getRulesToValidate()));
            coarseValidationDiscardedRules.put(operation, new HashSet<>());
            fineValidationDiscardedRules.put(operation, new HashSet<>());

            // Set to store server message rules for this operation
            Set<Rule> serverMessageRulesForThisOperation = new HashSet<>();

            // Boolean variable to report the discovery of new rules from server messages, initially set to true to let
            // the cycle begin
            boolean newServerMessageRulesFound = true;

            restart:
            while (newServerMessageRulesFound) {

                Set<Rule> rulesToValidate = new HashSet<>(operation.getRulesToValidate());
                rulesToValidate.addAll(serverMessageRulesForThisOperation);

                newServerMessageRulesFound = false;

                // Split rules into subset of possibly clashing rules
                Set<Rule> presenceRules = rulesToValidate.stream().filter(Rule::isPresenceRule).collect(Collectors.toSet());
                Set<Rule> setIpdRules = rulesToValidate.stream().filter(Rule::isSetIpdRule).collect(Collectors.toSet());
                Set<Rule> singleRules = rulesToValidate.stream().filter(Rule::isSingleRule).collect(Collectors.toSet());
                Set<Rule> constraintRules = rulesToValidate.stream().filter(Rule::isConstraintRule).collect(Collectors.toSet());
                Set<Rule> requiresCombinatorialValidation = rulesToValidate.stream().filter(Rule::requiresCombinatorialValidation).collect(Collectors.toSet());
                Set<Rule> alwaysApplicableRules = rulesToValidate.stream().filter(Rule::isAlwaysApplicable).collect(Collectors.toSet());

                for (int i = presenceRules.size(); i >= 0 && successfulSequence == null; i--) {

                    Set<Set<Rule>> combinationsOfPresenceRules = Sets.combinations(presenceRules, i);
                    for (Set<Rule> combinationOfPresenceRules : combinationsOfPresenceRules) {

                        // If the combination of presence rule is valid
                        if (isCombinationOfPresenceRulesValid(combinationOfPresenceRules)) {

                            // Remove set IPDs rule that work on removed parameters, or that have invalid parameter names
                            setIpdRules = removeSetIpdRulesWithInvalidParameters(setIpdRules, operation, combinationOfPresenceRules);

                            for (int j = setIpdRules.size(); j >= 0 && successfulSequence == null; j--) {

                                Set<Set<Rule>> combinationsOfSetIpdRules = Sets.combinations(setIpdRules, j);
                                for (Set<Rule> combinationOfSetIpdRules : combinationsOfSetIpdRules) {

                                    // If the combination of set inter-parameter dependencies rules is valid
                                    if (isCombinationOfSetIpdRulesValid(combinationOfSetIpdRules)) {

                                        singleRules = filterOutRulesOfUnavailableParameters(singleRules, operation, combinationOfPresenceRules);

                                        for (int k = singleRules.size(); k >= 0 && successfulSequence == null; k--) {

                                            Set<Set<Rule>> combinationsOfSingleRules = Sets.combinations(singleRules, k);
                                            for (Set<Rule> combinationOfSingleRules : combinationsOfSingleRules) {

                                                // TODO: If the combination of single rules is valid
                                                // FIXME: will be supporter later on, because at the moment nlp rest test does not extract default values
                                                if (true) {

                                                    constraintRules = filterOutRulesOfUnavailableParameters(constraintRules, operation, combinationOfPresenceRules);

                                                    for (int l = constraintRules.size(); l >= 0 && successfulSequence == null; l--) {

                                                        Set<Set<Rule>> combinationsOfConstraintRules = Sets.combinations(constraintRules, l);
                                                        for (Set<Rule> combinationOfConstraintRules : combinationsOfConstraintRules) {

                                                            // If combination of constraint rules is valid
                                                            if (isCombinationOfConstraintRulesValid(combinationOfConstraintRules)) {

                                                                // Filter out requires rules not supported by RTG
                                                                requiresCombinatorialValidation = requiresCombinatorialValidation.stream().filter(r -> !(r.getRuleType() == RuleType.REQUIRES) || ((RequiresRule) r).isSupportedByNominalFuzzer()).collect(Collectors.toSet());

                                                                for (int m = requiresCombinatorialValidation.size(); m >= 0 && successfulSequence == null; m--) {

                                                                    Set<Set<Rule>> combinationsOfRequiresCombinatorialValidationRules = Sets.combinations(requiresCombinatorialValidation, m);
                                                                    for (Set<Rule> combinationOfRequiresCombinatorialValidationRules : combinationsOfRequiresCombinatorialValidationRules) {

                                                                        long timeDiff = Instant.now().getEpochSecond() - startTime;

                                                                        if (timeDiff > maxIntervalPerOperation) {

                                                                            System.out.println("ABORTED: validation of operation exceeded time of " + maxIntervalPerOperation + "s.");
                                                                            sorter.removeFirst();
                                                                            continue nextOperation;

                                                                        }

                                                                        Set<Rule> staticallyValidatedRules = new HashSet<>(combinationOfSetIpdRules);
                                                                        staticallyValidatedRules.addAll(combinationOfPresenceRules);
                                                                        staticallyValidatedRules.addAll(combinationOfSingleRules);
                                                                        staticallyValidatedRules.addAll(combinationOfConstraintRules);
                                                                        staticallyValidatedRules.addAll(combinationOfRequiresCombinatorialValidationRules);

                                                                        for (Rule alwaysApplicableRule : alwaysApplicableRules) {
                                                                            if (alwaysApplicableRule.isApplicable(operation, new LinkedList<>(staticallyValidatedRules))) {
                                                                                staticallyValidatedRules.add(alwaysApplicableRule);
                                                                            }
                                                                        }
                                                                        staticallyValidatedRules.addAll(alwaysApplicableRules);

                                                                        // Added this set for debug purposes only. TODO: remove
                                                                        Set<Rule> tempDiscardedRules = Sets.difference(rulesToValidate, staticallyValidatedRules);

                                                                        System.out.println("STATICALLY VALIDATED: " + staticallyValidatedRules);

                                                                        Operation operationWithRulesApplied = operation.deepClone();
                                                                        staticallyValidatedRules.forEach(r -> r.apply(operationWithRulesApplied));

                                                                        successfulSequence = runDynamicValidation(operationWithRulesApplied);

                                                                        RuleExtractorProxy.printStatistics();

                                                                        // If no successful sequence was generated
                                                                        if (successfulSequence == null) {

                                                                            // Get rules from server message
                                                                            Set<Rule> newServerMessageRules = NLP_INTERACTION_PROCESSOR.getRulesAndReset();

                                                                            // If new rules are available, restart combinatorial validation
                                                                            if (!newServerMessageRules.isEmpty() && !Sets.difference(newServerMessageRules, rulesToValidate).isEmpty()) {
                                                                                serverMessageRulesForThisOperation.addAll(newServerMessageRules);
                                                                                newServerMessageRulesFound = true;
                                                                                System.out.println("FOUND NEW SERVER RULES: RESETTING STATIC VALIDATION");
                                                                                continue restart;
                                                                            }
                                                                        } else {
                                                                            System.out.println("DYNAMICALLY VALIDATED!!!!!");
                                                                            coarseValidationDiscardedRules.put(operation, Sets.difference(rulesToValidate, staticallyValidatedRules));
                                                                            break;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            serverMessageRules.put(operation, new HashSet<>(serverMessageRulesForThisOperation));

            // Perform fine validation of rules that can be fine-validated, only if a valid combination of rules is available, of course
            if (successfulSequence != null) {

                // Stop processing responses during fine validation
                NLP_INTERACTION_PROCESSOR.setProcess(false);

                // Keep only rules that can be fine-validated
                Set<Rule> rulesToFineValidate = Sets.difference(Sets.union(specificationRules.get(operation), serverMessageRules.get(operation)), coarseValidationDiscardedRules.get(operation)).stream().filter(Rule::canBeFineValidated).collect(Collectors.toSet());

                System.out.println("FINE VALIDATING THE FOLLOWING RULES: " + rulesToFineValidate);

                Set<Rule> fineValidationDiscardedRulesForOperation = new HashSet<>();

                // For each rule to fine validate
                /*for (Rule rule : rulesToFineValidate) {

                    List<Operation> fineValidationOperations = rule.getFineValidationOperations(successfulSequence.get(0).getOperation());

                    // Continue only if there are operations for fine validation
                    if (fineValidationOperations.size() > 0) {

                        // Build a new sequence, by repeating the successful sequence before and after to check replayability
                        TestSequence fineValidationSequence = new TestSequence();
                        fineValidationSequence.append(successfulSequence);
                        fineValidationOperations.forEach(o -> fineValidationSequence.append(new TestInteraction(o)));
                        fineValidationSequence.append(successfulSequence);

                        testRunner.run(fineValidationSequence);

                        // If fine validation sequences could be executed
                        if (fineValidationSequence.isExecuted()) {
                            int fineValidationSequenceSize = fineValidationSequence.size();
                            if (fineValidationSequence.get(0).getResponseStatusCode().isSuccessful() &&
                                    fineValidationSequence.get(fineValidationSequenceSize - 1).getResponseStatusCode().isSuccessful()) {
                                if (fineValidationSequence.stream().anyMatch(i -> !i.getResponseStatusCode().isSuccessful())) {
                                    fineValidationDiscardedRulesForOperation.add(rule);
                                }
                            }
                        }

                        fineValidationDiscardedRules.put(operation, fineValidationDiscardedRulesForOperation);
                    }
                }*/

                NLP_INTERACTION_PROCESSOR.setProcess(true);
            }

            Map<String, Set<Rule>> operationMap = new TreeMap<>();
            operationMap.put("rulesFromSpecification", new HashSet<>(specificationRules.get(operation)));
            operationMap.put("rulesFromServerMessages", new HashSet<>(serverMessageRules.get(operation)));
            operationMap.put("rulesDiscardedByCoarseValidation", new HashSet<>(coarseValidationDiscardedRules.get(operation)));
            operationMap.put("rulesDiscardedByFineValidation", new HashSet<>(fineValidationDiscardedRules.get(operation)));
            operationMap.put("validatedRules", new HashSet<>(Sets.difference(Sets.difference(Sets.union(specificationRules.get(operation),
                            serverMessageRules.get(operation)), coarseValidationDiscardedRules.get(operation)),
                    fineValidationDiscardedRules.get(operation))));

            cumulativeResultMap.put(operation.toString(), operationMap);

            try {
                Configuration configuration = Environment.getInstance().getConfiguration();
                String outputPath = configuration.getOutputPath() + configuration.getTestingSessionName() + "/validation/";
                File file = new File(outputPath);
                file.mkdirs();
                FileWriter writer = new FileWriter(outputPath + "validationResult.json");
                writer.write(gson.toJson(cumulativeResultMap));
                writer.close();
            } catch (IOException ignored) {}

            validatedOperationsCount++;
            System.out.println("VALIDATED OPERATION " + operation + " (" + validatedOperationsCount + "/" + totalOperationsCount + ")");

            sorter.removeFirst();
        }

        System.out.println("FINISHED VALIDATION.");
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

    /**
     *
     * @param combination the combination to evaluate.
     * @return true if the combination is valid.
     */
    private boolean isCombinationOfPresenceRulesValid(Set<Rule> combination) {
        Set<ParameterName> removedParameterNames =  combination.stream()
                .filter(r -> r.getRuleType() == RuleType.REMOVE && ((RemoveRule) r).getRemove())
                .map(r -> r.getParameterNames().stream().findFirst().get()).collect(Collectors.toSet());
        if (!removedParameterNames.isEmpty()) {
            Set<ParameterName> requiredParameterNames = combination.stream()
                    .filter(r -> r.getRuleType() == RuleType.REQUIRED && ((RequiredRule) r).getRequired())
                    .map(r -> r.getParameterNames().stream().findFirst().get()).collect(Collectors.toSet());
            return Sets.intersection(removedParameterNames, requiredParameterNames).isEmpty();
        }
        return true;
    }

    private Set<Rule> removeSetIpdRulesWithInvalidParameters(Set<Rule> combination, Operation operation, Set<Rule> presenceRules) {

        // Get all parameter names in operation
        Set<ParameterName> parametersInOperation = operation.getAllRequestParameters().stream()
                .map(Parameter::getName).collect(Collectors.toSet());

        // Remove parameter names which appear in remove rules
        presenceRules.stream().filter(r -> r.getRuleType() == RuleType.REMOVE && ((RemoveRule) r).getRemove())
                .forEach(r -> parametersInOperation.remove(r.getParameterNames().stream().findFirst().get()));

        // Return rules that only have the correct parameter names
        return combination.stream().filter(r -> parametersInOperation.containsAll(r.getParameterNames())).collect(Collectors.toSet());
    }

    /**
     * Validates a combination of set inter-parameter dependencies rules. Checks that the same parameters are not used
     * in different set IPDs.
     * @param combination the combination to evaluate.
     * @return true if the combination is valid.
     */
    private boolean isCombinationOfSetIpdRulesValid(Set<Rule> combination) {
        Set<ParameterName> usedParameterNames = new HashSet<>();
        for (Rule rule : combination) {

            // If a new rule uses parameter that have been already used by other rules, the combination is invalid
            if (!Sets.intersection(usedParameterNames, rule.getParameterNames()).isEmpty()) {
                return false;
            }

            usedParameterNames.addAll(rule.getParameterNames());
        }
        return true;
    }

    /**
     * Combination is valid if:
     * - for each parameter only one rule for type is permitted
     * - max and min are only applicable to numbers, strings, and arrays
     * - for each parameter, the format is compatible with the type
     * - exclusive maximum and exclusive minimum are only applicable to numbers for which max and mini are specified
     * @param combination the combination to evaluate.
     * @return true if the combination is valid.
     */
    public boolean isCombinationOfConstraintRulesValid(Set<Rule> combination) {

        // Initialize map of parameter names and rules
        Map<ParameterName, Set<Rule>> rulesForParameterMap = new HashMap<>();
        for (Rule rule : combination) {
            ParameterName parameterName = rule.getParameterNames().stream().findFirst().get();
            rulesForParameterMap.computeIfAbsent(parameterName, k -> new HashSet<>());
            rulesForParameterMap.get(parameterName).add(rule);
        }

        // Check validity
        for (ParameterName parameterName : rulesForParameterMap.keySet()) {

            // Get all the rules for a parameter
            Set<Rule> rulesForParameter = rulesForParameterMap.get(parameterName);

            // Get type rules
            Set<Rule> typeRules = rulesForParameter.stream().filter(r -> r.getRuleType() == RuleType.TYPE).collect(Collectors.toSet());
            if (typeRules.size() > 1) {
                return false;
            }

            // Get format rules
            Set<Rule> formatRules = rulesForParameter.stream().filter(r -> r.getRuleType() == RuleType.FORMAT).collect(Collectors.toSet());
            if (formatRules.size() > 1) {
                return false;
            }

            // Get collection format rules
            Set<Rule> collectionFormatRules = rulesForParameter.stream().filter(r -> r.getRuleType() == RuleType.COLLECTION_FORMAT).collect(Collectors.toSet());
            if (collectionFormatRules.size() > 1) {
                return false;
            }

            // Get maximum rules
            Set<Rule> maximumRules = rulesForParameter.stream().filter(r -> r.getRuleType() == RuleType.MAXIMUM).collect(Collectors.toSet());
            if (maximumRules.size() > 1) {
                return false;
            }

            // Get minimum rules
            Set<Rule> minimumRules = rulesForParameter.stream().filter(r -> r.getRuleType() == RuleType.MINIMUM).collect(Collectors.toSet());
            if (minimumRules.size() > 1) {
                return false;
            }

            // Get exclusive maximum rules
            Set<Rule> exclusiveMaximumRules = rulesForParameter.stream().filter(r -> r.getRuleType() == RuleType.EXCLUSIVE_MAXIMUM).collect(Collectors.toSet());
            if (exclusiveMaximumRules.size() > 1) {
                return false;
            }

            // Get exclusive minimum rules
            Set<Rule> exclusiveMinimumRules = rulesForParameter.stream().filter(r -> r.getRuleType() == RuleType.EXCLUSIVE_MINIMUM).collect(Collectors.toSet());
            if (exclusiveMinimumRules.size() > 1) {
                return false;
            }


            // TODO: check if single rules are compatible
        }

        return true;
    }

    public Set<Rule> filterOutRulesOfUnavailableParameters(Set<Rule> rules, Operation operation, Set<Rule> presenceRules) {

        // Get all parameter names in operation
        Set<ParameterName> parametersInOperation = operation.getAllRequestParameters().stream()
                .map(Parameter::getName).collect(Collectors.toSet());

        // Remove parameter names which appear in remove rules
        presenceRules.stream().filter(r -> r.getRuleType() == RuleType.REMOVE && ((RemoveRule) r).getRemove())
                .forEach(r -> parametersInOperation.remove(r.getParameterNames().stream().findFirst().get()));

        // Return rules that only have an available parameter name
        return rules.stream().filter(r -> parametersInOperation.contains(r.getParameterNames().stream().findFirst().get()))
                .collect(Collectors.toSet());
    }

    private void oldStrategy() {
        /*
        // While no successful sequence has been discovered, or a combination of size greater than 0 can be produced
        while (successfulSequence == null && globalCombinationSize > 0) {

            // Restart the combinatorial exploration if new rules have been added by server message processing
            if (rulesToValidateMaxCount.get(operation) < updatedRulesToValidateCount) {
                rulesToValidateMaxCount.put(operation, updatedRulesToValidateCount);
                combinationSize = updatedRulesToValidateCount;
                System.out.println("FOUND NEW RULE(S) IN SERVER MESSAGE. TOTAL RULES #: " + updatedRulesToValidateCount);
            }

            // Split rules into subset of possibly clashing rules
            Set<Rule> alwaysApplicableRules = operation.getRulesToValidate().stream()
                    .filter(Rule::isAlwaysApplicable).collect(Collectors.toSet());
            Set<Rule> setIpdRules = operation.getRulesToValidate().stream()
                    .filter(Rule::isSetIpdRule).collect(Collectors.toSet());
            Set<Rule> constraintRules = operation.getRulesToValidate().stream()
                    .filter(Rule::isConstraintRule).collect(Collectors.toSet());
            Set<Rule> presenceRules = operation.getRulesToValidate().stream()
                    .filter(Rule::isPresenceRule).collect(Collectors.toSet());
            Set<Rule> singleRules = operation.getRulesToValidate().stream()
                    .filter(Rule::isSingleRule).collect(Collectors.toSet());


            // Get combinations with n = combinationSize rules
            Set<Set<Rule>> combinationsWithSize = Sets.combinations(operation.getRulesToValidate(), (int) combinationSize);

            // For each combination
            for (Set<Rule> combination : combinationsWithSize) {

                // Continue only if the current combination has not been already tested
                if (!testedCombinations.get(operation).contains(combination)) {

                    // Set the combination as tested
                    testedCombinations.get(operation).add(combination);

                    // Get combination as list, where rules are ordered
                    List<Rule> combinationAsList = combination.stream()
                            .sorted(Comparator.comparing(Rule::getRuleType)).collect(Collectors.toList());

                    //System.out.println("NOW TESTING COMBINATION: " + combination);

                    // Static validation of combination
                    boolean staticallyValidRule = true;
                    for (Rule rule : combinationAsList) {
                        if (!rule.isApplicable(operation, combinationAsList)) {
                            staticallyValidRule = false;
                            //System.out.println("RULE COMBINATION STATICALLY DISCARDED. RULE NOT APPLICABLE: " + rule);
                            break;
                        }
                    }

                    // Continue only if the combination is statically valid
                    if (staticallyValidRule) {

                        // Get a clone of the operation to which apply the rules
                        Operation operationWithRulesApplied = operation.deepClone();

                        // Apply all the rules in the combination
                        combinationAsList.forEach(r -> r.apply(operationWithRulesApplied));

                        // Apply rules not in combination
                        operationWithRulesApplied.getRulesToValidate().stream()
                                .filter(r -> !r.requiresCombinatorialValidation())
                                .forEach(r -> r.apply(operationWithRulesApplied));

                        // Run dynamic validation and store result
                        successfulSequence = runDynamicValidation(operationWithRulesApplied);

                        if (successfulSequence != null) {
                            validatedCombinations.put(operation, combination);
                            System.out.println("VALIDATED!!!! COMBINATION: " + validatedCombinations.get(operation));
                            break;
                        }
                    }
                }
            }

            combinationSize--;

            System.out.println("COMBINATION SIZE REDUCED TO: " + combinationSize);
        }*/
    }
}
