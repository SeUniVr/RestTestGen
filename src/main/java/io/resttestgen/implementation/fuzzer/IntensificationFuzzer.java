package io.resttestgen.implementation.fuzzer;

import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Fuzzer;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.mutator.OperationMutator;
import io.resttestgen.implementation.mutator.operation.*;
import io.resttestgen.implementation.mutator.parameter.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IntensificationFuzzer extends Fuzzer {

    private static final Logger logger = LogManager.getLogger(IntensificationFuzzer.class);

    private final TestSequence testSequenceToMutate;
    private final HashMap<OperationMutator, Integer> mutators = new HashMap<>();

    public IntensificationFuzzer(TestSequence testSequenceToMutate) {
        this.testSequenceToMutate = testSequenceToMutate;
        mutators.put(new AddParameterOperationMutator(), 5);
        mutators.put(new AddInvalidParameterOperationMutator(new MissingRequiredParameterMutator()), 5);
        mutators.put(new AddInvalidParameterOperationMutator(new WrongTypeParameterMutator()), 5);
        mutators.put(new AddInvalidParameterOperationMutator(new ConstraintViolationParameterMutator()), 4);
        mutators.put(new AddInvalidParameterOperationMutator(new NumberOutOfBoundaryParameterMutator()), 4);
        mutators.put(new RemoveParameterOperationMutator(), 3);
        mutators.put(new RefillValueOperationMutator(), 10);
        mutators.put(new HttpMethodOperationMutator(), 3);
        mutators.put(new MutateRandomParameterWithParameterMutatorOperationMutator(new MissingRequiredParameterMutator()), 5);
        mutators.put(new MutateRandomParameterWithParameterMutatorOperationMutator(new WrongTypeParameterMutator()), 5);
        mutators.put(new MutateRandomParameterWithParameterMutatorOperationMutator(new ConstraintViolationParameterMutator()), 5);
        mutators.put(new MutateRandomParameterWithParameterMutatorOperationMutator(new NumberBoundaryParameterMutator()), 4);
        mutators.put(new MutateRandomParameterWithParameterMutatorOperationMutator(new NumberOutOfBoundaryParameterMutator()), 4);
    }

    public List<TestSequence> generateTestSequences(int numberOfSequences) {

        List<TestSequence> testSequences = new LinkedList<>();

        // Use only interaction of test sequence (test sequence should be of length 1)
        TestInteraction interaction = testSequenceToMutate.getFirst();

        // Get original fuzzed operation
        Operation originalOperation = interaction.getFuzzedOperation();

        // Get set of applicable mutations to this operation
        Set<OperationMutator> applicableMutators =
                mutators.keySet().stream().filter(m -> m.isOperationMutable(originalOperation)).collect(Collectors.toSet());

        for (OperationMutator operationMutator : applicableMutators) {
            for (int i = 0; i < mutators.get(operationMutator); i++) {

                logger.info("Applying mutator: {}", operationMutator);

                // Apply mutation
                Operation mutatedOperation = operationMutator.mutate(originalOperation);

                // Create test interaction from operation
                TestInteraction testInteraction = new TestInteraction(mutatedOperation);
                testInteraction.addTag("mutated");

                // Create test sequence from mutated operation
                TestSequence currentTestSequence = new TestSequence(this, testInteraction);

                String sequenceName = !testInteraction.getFuzzedOperation().getOperationId().isEmpty() ?
                        testInteraction.getFuzzedOperation().getOperationId() :
                        testInteraction.getFuzzedOperation().getMethod().toString() + "-" +
                                testInteraction.getFuzzedOperation().getEndpoint();
                currentTestSequence.setName(sequenceName);
                currentTestSequence.appendGeneratedAtTimestampToSequenceName();

                // Execute test sequence
                TestRunner testRunner = TestRunner.getInstance();
                testRunner.run(currentTestSequence);

                // Evaluate sequence with relevant status code oracle
                /*if (operationMutator.isErrorMutator()) {
                    ErrorStatusCodeOracle errorStatusCodeOracle = new ErrorStatusCodeOracle();
                    errorStatusCodeOracle.assertTestSequence(currentTestSequence);
                } else {
                    StatusCodeOracle statusCodeOracle = new StatusCodeOracle();
                    statusCodeOracle.assertTestSequence(currentTestSequence);
                }

                // Write report to file
                try {
                    ReportWriter reportWriter = new ReportWriter(currentTestSequence);
                    reportWriter.write();
                    RestAssuredWriter restAssuredWriter = new RestAssuredWriter(currentTestSequence);
                    restAssuredWriter.write();
                } catch (IOException e) {
                    logger.warn("Could not write report to file.");
                }*/

                testSequences.add(currentTestSequence);
            }
        }

        // Return the list of test sequences
        return testSequences;
    }
}
