package io.resttestgen.implementation.strategy.support;

import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Strategy;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.operationsorter.OperationsSorter;
import io.resttestgen.implementation.fuzzer.NominalFuzzer;
import io.resttestgen.implementation.operationssorter.RandomOperationsSorter;
import io.resttestgen.implementation.strategy.NominalAndErrorStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class TestOperationsStrategy extends Strategy {
    private static final Logger logger = LogManager.getLogger(NominalAndErrorStrategy.class);

    private static final List<Operation> testedOperations = new LinkedList<>();
    private static final List<Operation> untestedOperations = new LinkedList<>();

    public void start() {

        // According to the order provided by the graph, execute the nominal fuzzer
        OperationsSorter sorter = new RandomOperationsSorter();
        while (!sorter.isEmpty()) {
            Operation operationToTest = sorter.getFirst();
            logger.debug("Testing operation " + operationToTest);
            NominalFuzzer nominalFuzzer = new NominalFuzzer(operationToTest);
            List<TestSequence> nominalSequences = nominalFuzzer.generateTestSequences(8);

            for (TestSequence testSequence : nominalSequences) {
                // Run test sequence
                TestRunner testRunner = TestRunner.getInstance();
                testRunner.run(testSequence);
            }

            boolean tested = false;
            for (TestSequence testSequence : nominalSequences) {
                if (testSequence.isExecuted() && testSequence.get(0).getResponseStatusCode().isSuccessful()) {
                    tested = true;
                    break;
                }
            }

            if (tested) {
                testedOperations.add(operationToTest);
            } else {
                untestedOperations.add(operationToTest);
            }

            sorter.removeFirst();
        }

        System.out.println("TESTED: " + testedOperations);
        System.out.println("NOT TESTED: " + untestedOperations);
    }
}
