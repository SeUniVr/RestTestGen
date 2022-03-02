package io.resttestgen.core.testing;

import io.resttestgen.core.Configuration;
import io.resttestgen.core.Environment;

import java.io.IOException;

/**
 * This abstract class is used to write a test interaction or a test sequence to file. Different implementations provide
 * different formats (e.g. reports in JSON, JUnit test cases, Postman).
 */
public abstract class Writer {

    protected Configuration configuration = Environment.getInstance().getConfiguration();
    protected TestSequence testSequence;

    public Writer(TestSequence testSequence) {
        this.testSequence = testSequence;
    }

    public abstract String getOutputFormatName();

    /**
     * Writes a test sequence to file.
     */
    public abstract void write() throws IOException;

    // FIXME: make path compatible with Windows
    public String getOutputPath() {
        return configuration.getOutputPath() + configuration.getTestingSessionName() + "/" + getOutputFormatName() +
                "/" + testSequence.getGenerator() + "/";
    }
}
