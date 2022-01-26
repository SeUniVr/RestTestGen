package io.resttestgen.core.testing;

import io.resttestgen.core.Environment;

/**
 * This abstract class is used to write a test interaction or a test sequence to file. Different implementations provide
 * different formats (e.g. reports in JSON, JUnit test cases, Postman).
 */
public abstract class Writer {

    protected Environment environment;
    protected TestSequence testSequence;

    public Writer(Environment environment, TestSequence testSequence) {
        this.environment = environment;
        this.testSequence = testSequence;
    }

    public abstract String getOutputFormatName();

    /**
     * Writes a test sequence to file.
     */
    public abstract void write();

    public String getOutputPath() {
        return environment.configuration.getOutputPath() + environment.configuration.getTestingSessionName() + "/" +
                getOutputFormatName() + "/" + testSequence.getGenerator() + "/";
    }
}
