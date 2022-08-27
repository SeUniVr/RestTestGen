package io.resttestgen.implementation.oracle;

import io.resttestgen.core.testing.Oracle;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestResult;
import io.resttestgen.core.testing.TestSequence;

/**
 * Evaluates a test sequence as passed if all the included test interactions got a successful (2XX) status code. A test
 * sequence is evaluated as failed if at least one interaction got a 5XX status code. Other status codes are ignored.
 */
public class StatusCodeOracle extends Oracle {

    @Override
    public TestResult assertTestSequence(TestSequence testSequence) {

        TestResult testResult = new TestResult();

        if (!testSequence.isExecuted()) {
            return testResult.setError("One or more interaction in the sequence have not been executed.");
        }

        for (TestInteraction testInteraction : testSequence) {
            if (testInteraction.getResponseStatusCode().isSuccessful()) {
                testResult.setPass("The test sequence was executed successfully.");
            } else if (testInteraction.getResponseStatusCode().isServerError()) {
                testResult.setFail("A server error occurred during the execution of the sequence.");
                break;
            } else if (testResult.isPending() && testInteraction.getResponseStatusCode().isClientError()) {
                testResult.setUnknown("The obtained status code is not informative enough to determine the outcome " +
                        "of the test sequence.");
            }
        }
        testSequence.addTestResult(this, testResult);
        return testResult;
    }
}
