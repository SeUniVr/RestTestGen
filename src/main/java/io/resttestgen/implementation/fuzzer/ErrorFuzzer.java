package io.resttestgen.implementation.fuzzer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Fuzzer;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.mutator.OperationMutator;
import io.resttestgen.implementation.mutator.operation.MutateRandomParameterWithParameterMutatorOperationMutator;
import io.resttestgen.implementation.mutator.parameter.ConstraintViolationParameterMutator;
import io.resttestgen.implementation.mutator.parameter.MissingRequiredParameterMutator;
import io.resttestgen.implementation.mutator.parameter.WrongTypeParameterMutator;
import io.resttestgen.implementation.oracle.ErrorStatusCodeOracle;
import io.resttestgen.implementation.writer.ReportWriter;
import io.resttestgen.implementation.writer.RestAssuredWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ErrorFuzzer extends Fuzzer {

    private static final Logger logger = LogManager.getLogger(ErrorFuzzer.class);

    private final TestSequence testSequenceToMutate;
    private final Set<OperationMutator> mutators;

    public ErrorFuzzer(TestSequence testSequenceToMutate) {
        this.testSequenceToMutate = testSequenceToMutate;
        mutators = new HashSet<>();
        mutators.add(new MutateRandomParameterWithParameterMutatorOperationMutator(new MissingRequiredParameterMutator()));
        mutators.add(new MutateRandomParameterWithParameterMutatorOperationMutator(new WrongTypeParameterMutator()));
        mutators.add(new MutateRandomParameterWithParameterMutatorOperationMutator(new ConstraintViolationParameterMutator()));
    }

    public List<TestSequence> generateTestSequences(int numberOfSequences) {

        List<TestSequence> testSequences = new LinkedList<>();

        // Iterate on interaction of test sequence
        for (TestInteraction interaction : testSequenceToMutate) {

            // For each sequence, we generate n mutants for the last interaction
            for (int j = 0; j < numberOfSequences; j++) {

                // Get original fuzzed operation
                Operation originalOperation = interaction.getFuzzedOperation();

                // Get set of applicable mutations to this operation
                Set<OperationMutator> applicableMutators =
                        mutators.stream().filter(m -> m.isOperationMutable(originalOperation)).collect(Collectors.toSet());

                // Break if no mutations are applicable
                if (applicableMutators.isEmpty()) {
                    logger.warn("There are no applicable mutations to this operation.");
                    break;
                }

                // Choose a random mutator
                OperationMutator mutator = Environment.getInstance().getRandom().nextElement(applicableMutators).get();

                // Apply mutation
                Operation mutatedOperation = mutator.mutate(originalOperation);

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

                // Evaluate sequence with oracles
                ErrorStatusCodeOracle errorStatusCodeOracle = new ErrorStatusCodeOracle();
                errorStatusCodeOracle.assertTestSequence(currentTestSequence);

                // Write report to file
                try {
                    ReportWriter reportWriter = new ReportWriter(currentTestSequence);
                    reportWriter.write();
                    RestAssuredWriter restAssuredWriter = new RestAssuredWriter(currentTestSequence);
                    restAssuredWriter.write();
                } catch (IOException e) {
                    logger.warn("Could not write report to file.");
                }

                testSequences.add(currentTestSequence);
            }
        }

        // Return the list of test sequences
        return testSequences;
    }
}
