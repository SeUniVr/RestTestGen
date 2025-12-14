package io.resttestgen.implementation.strategy;

import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Strategy;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.operationsorter.OperationsSorter;
import io.resttestgen.implementation.fuzzer.ExperienceFuzzer;
import io.resttestgen.implementation.operationssorter.GraphBasedOperationsSorter;
import io.resttestgen.implementation.oracle.StatusCodeOracle;
import io.resttestgen.implementation.writer.ReportWriter;
import io.resttestgen.implementation.writer.RestAssuredWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("unused")
public class ExperienceNominalTestingStrategy extends Strategy {

    private static final Logger logger = LogManager.getLogger(ExperienceNominalTestingStrategy.class);

    public void start() {


        // According to the order provided by the graph, execute the nominal fuzzer
        OperationsSorter sorter = new GraphBasedOperationsSorter();
        while (!sorter.isEmpty()) {
            Operation operationToTest = sorter.getFirst();
            logger.debug("Testing operation " + operationToTest);

            for (int i = 0; i < 50; i++) {
                ExperienceFuzzer experienceFuzzer = new ExperienceFuzzer(operationToTest);
                List<TestSequence> nominalSequences = experienceFuzzer.generateTestSequences(1);

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
            }
            sorter.removeFirst();
        }

        Environment.getInstance().getExperience().printStats();

    }
}
