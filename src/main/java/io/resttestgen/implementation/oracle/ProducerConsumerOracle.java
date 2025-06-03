package io.resttestgen.implementation.oracle;

import io.resttestgen.core.testing.Oracle;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestResult;
import io.resttestgen.core.testing.TestSequence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Given a test sequence composed by two interactions, determines if the interactions operate on the same "object" in a
 * producer/consumer fashion, i.e., if the output of the first interaction is used as input of the second.
 */
public class ProducerConsumerOracle extends Oracle {

    private static final Logger logger = LogManager.getLogger(ProducerConsumerOracle.class);

    @Override
    public TestResult assertTestSequence(TestSequence testSequence) {
        TestResult testResult = new TestResult();

        // This oracle only supports test sequences of size 2.
        if (testSequence.size() == 2) {

            // Get the producer interaction and check that it actually produced something
            TestInteraction producerInteraction = testSequence.getFirst();
            if (producerInteraction.getResponseBody() == null ||
                    producerInteraction.getResponseBody().trim().isEmpty()) {
                testResult.setFail("The first operation returned an empty body.");
            } else {
                TestInteraction consumerInteraction = testSequence.getLast();
                String compressedBody = consumerInteraction.getResponseBody()
                        .replaceAll("\\s", "")
                        .replaceAll("\\r\\n", "")
                        .replaceAll("\\n", "");

                AtomicInteger found = new AtomicInteger();
                AtomicInteger notFound = new AtomicInteger();

                consumerInteraction.getFuzzedOperation().getLeaves().forEach(parameterLeaf -> {
                    String compressedJSONString = parameterLeaf.getJSONString()
                            .replaceAll("\\s", "")
                            .replaceAll("\\r\\n", "")
                            .replaceAll("\\n", "");
                    if (compressedBody.contains(compressedJSONString)) {
                        found.getAndIncrement();
                    } else {
                        notFound.getAndIncrement();
                    }
                });

                double ratio = found.doubleValue() / (found.doubleValue() + notFound.doubleValue());

                logger.debug("Found {} producer/consumer relations ({}%). Not found: {}.", found, ratio * 100, notFound);

                if (ratio >= 0.7) {
                    testResult.setPass("Producer/consumer relation found for more than 70% of the parameters.");
                } else {
                    testResult.setFail("Producer/consumer relation not found.");
                }
            }
        } else {
            testResult.setError("The test sequence consists of more than two interactions.");
        }

        return testResult;
    }
}
