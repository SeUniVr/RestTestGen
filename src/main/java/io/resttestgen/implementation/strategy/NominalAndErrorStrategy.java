package io.resttestgen.implementation.strategy;

import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Strategy;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.operationsorter.OperationsSorter;
import io.resttestgen.implementation.fuzzer.ErrorFuzzer;
import io.resttestgen.implementation.fuzzer.NominalFuzzer;
import io.resttestgen.implementation.operationssorter.GraphBasedOperationsSorter;
import io.resttestgen.implementation.oracle.StatusCodeOracle;
import io.resttestgen.implementation.writer.CoverageReportWriter;
import io.resttestgen.implementation.writer.ReportWriter;
import io.resttestgen.implementation.writer.RestAssuredWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

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
            List<TestSequence> nominalSequences = nominalFuzzer.generateTestSequences(20);

            for (TestSequence testSequence : nominalSequences) {

                // Run test sequence
                TestRunner testRunner = TestRunner.getInstance();
                testRunner.run(testSequence);
                // Evaluate sequence with oracles
                StatusCodeOracle statusCodeOracle = new StatusCodeOracle();
                statusCodeOracle.assertTestSequence(testSequence);

                // Write report to file
                try {
                    ReportWriter reportWriter = new ReportWriter(testSequence);
                    reportWriter.write();
                    RestAssuredWriter restAssuredWriter = new RestAssuredWriter(testSequence);
                    restAssuredWriter.write();
                } catch (IOException e) {
                    logger.warn("Could not write report to file.");
                    e.printStackTrace();
                }
            }
            globalNominalTestSequence.append(nominalSequences);
            sorter.removeFirst();
        }




        // Keep only successful test interactions in the sequence
        globalNominalTestSequence.filterBySuccessfulStatusCode();

        //GraphTestCase.generateGraph(globalNominalTestSequence);

        ErrorFuzzer errorFuzzer = new ErrorFuzzer(globalNominalTestSequence);
        errorFuzzer.generateTestSequences(10);

        try {
            CoverageReportWriter coverageReportWriter = new CoverageReportWriter(TestRunner.getInstance().getCoverage());
            coverageReportWriter.write();
        } catch (IOException e) {
            logger.warn("Could not write Coverage report to file.");
            e.printStackTrace();
        }
    }
}
