package io.resttestgen.implementation.writer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.Writer;

public class JUnitWriter extends Writer {

    public JUnitWriter(Environment environment, TestSequence testSequence) {
        super(environment, testSequence);
    }

    @Override
    public String getOutputFormatName() {
        return "junit";
    }

    @Override
    public void write() {
        // TODO: implement JUnit writer
    }
}
