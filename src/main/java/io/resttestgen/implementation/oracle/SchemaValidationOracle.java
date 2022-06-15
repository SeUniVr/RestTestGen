package io.resttestgen.implementation.oracle;

import io.resttestgen.core.testing.Oracle;
import io.resttestgen.core.testing.TestResult;
import io.resttestgen.core.testing.TestSequence;

@SuppressWarnings("unused")
public class SchemaValidationOracle extends Oracle {

    @Override
    public TestResult assertTestSequence(TestSequence testSequence) {
        throw new InternalError("The SchemaValidationOracle is not available yet.");
    }
}
