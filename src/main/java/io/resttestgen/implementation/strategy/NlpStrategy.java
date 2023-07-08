package io.resttestgen.implementation.strategy;

import com.google.common.collect.Sets;
import io.resttestgen.boot.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.rule.Rule;
import io.resttestgen.core.helper.RuleExtractorProxy;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Strategy;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.operationsorter.OperationsSorter;
import io.resttestgen.implementation.fuzzer.NominalFuzzer;
import io.resttestgen.implementation.helper.RulesCombinationHelper;
import io.resttestgen.implementation.operationssorter.GraphBasedOperationsSorter;
import io.resttestgen.implementation.writer.ReportWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class NlpStrategy extends Strategy {

    private static final Logger logger = LogManager.getLogger(NlpStrategy.class);

    private static final long MAX_SECONDS_PER_OPERATION = 180;
    private static final int MAX_ATTEMPTS_PER_COMBINATION = 500;

    // Validation results
    private static final Map<Operation, List<Rule>> rulesFromNlp = new HashMap<>();
    private static final Map<Operation, List<Rule>> coarseValidatedRules = new HashMap<>();
    private static final Map<Operation, List<Rule>> fineValidatedRules = new HashMap<>();

    private static final Map<Operation, List<Rule>> dontKnowRules = new HashMap<>();

    private final HashMap<String, Map<String, Set<Rule>>> cumulativeResultMap = new HashMap<>();


    @Override
    public void start() {

        // Check if the rule generator is online
        if (!RuleExtractorProxy.isOnline()) {
            logger.warn("Rule generator is not reachable. Make sure you ran it and it is reachable at the URL specified in the RuleGenerator class.");
            return;
        }

        // Process descriptions in the specification
        for (Operation operation : Environment.getInstance().getOpenAPI().getOperations()) {
            operation.getRulesToValidate().addAll(RuleExtractorProxy.extractRulesFromOperationText(operation));
            operation.getRulesToValidate().addAll(RuleExtractorProxy.extractRulesFromRequestBodyDescription(operation));
            operation.getAllRequestParameters().forEach(p -> operation.getRulesToValidate().addAll(RuleExtractorProxy.extractRulesFromParameterText(p)));
        }

        RuleExtractorProxy.printStatistics();

        logger.info("NLP finished. Starting validation.");

        // Validate operations according to the order provided by the ODG-based strategy
        OperationsSorter sorter = new GraphBasedOperationsSorter();

        while (!sorter.isEmpty()) {

            logger.info("Operations remaining: " + sorter.getQueueSize());

            Operation operation = sorter.getFirst();

            // Store all the rules from NLP
            rulesFromNlp.put(operation, new LinkedList<>(operation.getRulesToValidate()));
            coarseValidatedRules.put(operation, new LinkedList<>());
            fineValidatedRules.put(operation, new LinkedList<>());

            // Helper for generating rules combinations
            RulesCombinationHelper rulesCombinationHelper = new RulesCombinationHelper(operation);

            // Conditions to keep validating
            TestSequence successfulSequence = null;
            long operationValidationStartTime = Instant.now().getEpochSecond();
            List<Rule> combination = rulesCombinationHelper.getNextStaticallyValidCombination();

            // If there are no rules to validate, just execute the operation to possibly populate the API with test data
            if (combination.size() == 0) {
                runCoarseDynamicValidation(operation, new LinkedList<>());
            }

            // If there are rules to validate, perform validation
            else {

                // Continue validation till the operation time budget is over
                while (Instant.now().getEpochSecond() - operationValidationStartTime < MAX_SECONDS_PER_OPERATION) {

                    logger.info("Validating combination: " + combination);

                    successfulSequence = runCoarseDynamicValidation(operation, combination);

                    // If an empty combination has been reached, stop and set an empty combination
                    if (combination.size() == 0) {
                        coarseValidatedRules.put(operation, new LinkedList<>());
                        break;
                    }

                    // If coarse validation worked, store results and continue to fine validation
                    if (successfulSequence != null) {

                        // Includes all rules that have been previously excluded, but in practice they are applied (e.g., from specification)
                        combination.addAll(rulesFromNlp.get(operation).stream().filter(r -> r.isApplied(operation)).collect(Collectors.toList()));

                        coarseValidatedRules.put(operation, combination);
                        runFineDynamicValidation(operation, successfulSequence, combination);

                        logger.info(operation);
                        logger.info("From NLP:" + rulesFromNlp.get(operation));
                        logger.info("Coarse validated: " + coarseValidatedRules.get(operation));
                        logger.info("Fine validated: " + fineValidatedRules.get(operation));

                        Set<Rule> discardedByCoarseValidation = Sets.difference(new HashSet<>(rulesFromNlp.get(operation)), new HashSet<>(coarseValidatedRules.get(operation)));
                        Set<Rule> discardedByFineValidation = Sets.difference(new HashSet<>(coarseValidatedRules.get(operation)), new HashSet<>(fineValidatedRules.get(operation)));

                        logger.info("Discarded by coarse validation: " + discardedByCoarseValidation);
                        logger.info("Discarded by fine validation: " + discardedByFineValidation);



                        Map<String, Set<Rule>> operationMap = new TreeMap<>();
                        operationMap.put("rulesFromSpecification", new HashSet<>(rulesFromNlp.get(operation)));

                        operationMap.put("rulesDiscardedByCoarseValidation", new HashSet<>(discardedByCoarseValidation));
                        operationMap.put("rulesDiscardedByFineValidation", new HashSet<>(discardedByFineValidation));
                        //operationMap.put("dontKnowRules", new HashSet<>(dontKnowRules.get(operation)));
                        operationMap.put("validatedRules", new HashSet<>(fineValidatedRules.get(operation)));

                        cumulativeResultMap.put(operation.toString(), operationMap);

                        break;
                    }

                    combination = rulesCombinationHelper.getNextStaticallyValidCombination();
                }
            }

            /*try {
                Gson gson = new Gson();
                Configuration configuration = Environment.getInstance().getConfiguration();
                String outputPath = configuration.getOutputPath() + configuration.getTestingSessionName() + "/Validation/";
                File file = new File(outputPath);
                file.mkdirs();
                FileWriter writer = new FileWriter(outputPath + "validationResult.json");
                writer.write(gson.toJson(cumulativeResultMap));
                writer.close();
            } catch (IOException ignored) {}

             */

            try {
                Set<Rule> allRules = new HashSet<>(rulesFromNlp.get(operation));
                allRules.addAll(coarseValidatedRules.get(operation));
                allRules.addAll(fineValidatedRules.get(operation));
                Configuration configuration = Environment.getInstance().getConfiguration();
                String outputPath = configuration.getOutputPath() + configuration.getTestingSessionName() + "/Validation/";
                File file = new File(outputPath);
                file.mkdirs();
                FileWriter writer = new FileWriter(outputPath + operation.toString().replaceAll(" ", "_").replaceAll("/", "_") + ".csv");
                StringBuilder content = new StringBuilder();
                content.append("\"rule\",\"nlp\",\"coarse\",\"fine\"\n");
                for (Rule rule : allRules) {
                    content.append("\"").append(rule.toString().replaceAll("\"", "'")).append("\",");
                    if (rulesFromNlp.get(operation).contains(rule)) {
                        content.append("1");
                    } else {
                        content.append("0");
                    }
                    content.append(",");
                    if (coarseValidatedRules.get(operation).contains(rule)) {
                        content.append("1");
                    } else {
                        content.append("0");
                    }
                    content.append(",");
                    if (fineValidatedRules.get(operation).contains(rule)) {
                        content.append("1");
                    } else {
                        content.append("0");
                    }
                    content.append("\n");
                }
                writer.write(content.toString());
                writer.close();
            } catch (IOException ignored) {}

            sorter.removeFirst();
        }

        // Apply rules to specification and export it
        for (Operation operation : Environment.getInstance().getOpenAPI().getOperations()) {
            List<Rule> rulesToApply = fineValidatedRules.get(operation);
            logger.info("Applying rules to operation " + operation + ": " + rulesToApply);
            operation.applyRules(rulesToApply);
        }
        try {
            Environment.getInstance().getOpenAPI().exportAsJsonOpenApiSpecification("enhanced_spec.json");
        } catch (IOException e) {
            logger.warn("Could not write specification to file.");
        }
    }

    /**
     * Run coarse dynamic validation for a given rules combination.
     * @param operation the operation on which rules are applied.
     * @param combination the rule combination to dynamically validate.
     * @return the successful test sequence with the combination applied, or null.
     */
    private TestSequence runCoarseDynamicValidation(Operation operation, List<Rule> combination) {

        for (int i = 0; i < MAX_ATTEMPTS_PER_COMBINATION; i++) {

            // Get clone of operation
            Operation currentAttemptOperation = operation.deepClone();

            // Apply all non value rules
            combination.forEach(r -> r.apply(currentAttemptOperation));

            // Fuzz the operations with nominal fuzzer
            NominalFuzzer nominalFuzzer = new NominalFuzzer(currentAttemptOperation);
            nominalFuzzer.setStrict(true);
            TestSequence testSequence = nominalFuzzer.generateTestSequences(1).get(0);

            // Run test sequence
            TestRunner.getInstance().run(testSequence);

            try {
                new ReportWriter(testSequence).write();
            } catch (IOException ignored) {}

            // If the test sequence was successful, return
            if (testSequence.isExecuted() && testSequence.get(0).getResponseStatusCode().isSuccessful()) {
                return testSequence;
            }
        }

        // In case no successful sequence is found, return null
        return null;
    }

    /**
     * Runs fine dynamic validation of a given rule combination, by repeating a successful test sequence.
     * @param successfulTestSequence the successful test sequence to replay.
     * @param combination the combination to fine validate.
     */
    @NotNull
    private void runFineDynamicValidation(Operation operation, TestSequence successfulTestSequence, List<Rule> combination) {

        Set<Rule> currentRemovedRules = new HashSet<>();
        Set<Rule> currentFineValidatedRules = new HashSet<>();

        // Perform fine validation, for each rule in the combination
        for (Rule rule : combination) {

            Set<Rule> fineValidationResult = rule.fineValidate(successfulTestSequence, currentRemovedRules);
            currentFineValidatedRules.addAll(fineValidationResult);

            if (fineValidationResult.size() == 1 && !currentFineValidatedRules.contains(rule)) {
                currentRemovedRules.add(rule);
            }
        }

        // Includes all rules that have been previously excluded, but in practice they are applied (e.g., from specification)
        currentFineValidatedRules.addAll(rulesFromNlp.get(operation).stream().filter(r -> r.isApplied(operation)).collect(Collectors.toSet()));

        fineValidatedRules.put(operation, currentFineValidatedRules.stream()
                .sorted(Comparator.comparing(Rule::getRuleType))
                .collect(Collectors.toList()));
    }
}
