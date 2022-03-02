package io.resttestgen.implementation.strategy;

import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.operationsorter.OperationsSorter;
import io.resttestgen.core.testing.Strategy;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.implementation.fuzzer.ErrorFuzzer;
import io.resttestgen.implementation.fuzzer.NominalFuzzer;
import io.resttestgen.implementation.operationssorter.GraphBasedOperationsSorter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
public class NominalAndErrorStrategy extends Strategy {

    private static final Logger logger = LogManager.getLogger(NominalAndErrorStrategy.class);

    private final TestSequence globalNominalTestSequence = new TestSequence();

    public void start() {

        // According to the order provided by the graph, execute the nominal fuzzer
        OperationsSorter sorter = new GraphBasedOperationsSorter();
        while (!sorter.isEmpty()) {
            Operation operationToTest = sorter.getFirst();
            logger.debug("Testing operation " + operationToTest);
            NominalFuzzer nominalFuzzer = new NominalFuzzer(operationToTest);
            globalNominalTestSequence.append(nominalFuzzer.generateTestSequences(5));
            sorter.removeFirst();
        }

        // Keep only successful test interactions in the sequence
        globalNominalTestSequence.filterBySuccessfulStatusCode();

        ErrorFuzzer errorFuzzer = new ErrorFuzzer(globalNominalTestSequence);
        errorFuzzer.generateTestSequences(5);
    }
}
