package io.resttestgen.core.testing;

import io.resttestgen.boot.Configuration;
import io.resttestgen.core.Environment;

import java.io.IOException;

/**
 * This abstract class is used to write a test interaction or a test sequence to file. Different implementations provide
 * different formats (e.g. reports in JSON, JUnit test cases, Postman).
 */
public abstract class Writer {

    protected final Configuration configuration = Environment.getInstance().getConfiguration();
    protected final TestSequence testSequence;

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

    /**
     * Returns a suggested file name for the sequence to be written, based on the sequence name.
     * @param extension the extension for the file.
     * @return the suggested file name, including the extension (if provided)
     */
    public String getSuggestedFileName(String extension) {

        String finalExtension = "";

        // Compute actual extension based on the user input
        if (extension != null && !extension.equals("")) {
            if (extension.startsWith(".")) {
                finalExtension = extension;
            } else {
                finalExtension = "." + extension;
            }
        }

        return testSequence.getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + finalExtension;
    }
}
