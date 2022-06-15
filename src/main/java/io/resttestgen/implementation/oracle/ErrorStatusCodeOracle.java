package io.resttestgen.implementation.oracle;

import io.resttestgen.core.testing.Oracle;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestResult;
import io.resttestgen.core.testing.TestSequence;

/**
 * Evaluates an erroneous sequence whose last test interaction is mutated by the error fuzzer.
 */
public class ErrorStatusCodeOracle extends Oracle {

    @Override
    public TestResult assertTestSequence(TestSequence testSequence) {

        TestResult testResult = new TestResult();

        if (!testSequence.isExecuted()) {
            return testResult.setError("One or more interaction in the sequence have not been executed.");
        }

        if (testSequence.size() > 0) {
            TestInteraction testInteraction = testSequence.getLast();
            if (testInteraction.getResponseStatusCode().isClientError()) {
                testResult.setPass("The erroneous test sequence was rejected by the server.");
            } else if (testInteraction.getResponseStatusCode().isSuccessful()) {
                testResult.setFail("The erroneous test sequence was accepted as valid by the server.");
            } else if (testInteraction.getResponseStatusCode().isServerError()) {
                testResult.setFail("A server error occurred during the execution of the sequence.");
            }
        }
        testSequence.addTestResult(this, testResult);
        return testResult;
    }
}
