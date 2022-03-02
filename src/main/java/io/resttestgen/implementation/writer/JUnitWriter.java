package io.resttestgen.implementation.writer;

import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.Writer;

import java.io.IOException;

public class JUnitWriter extends Writer {

    public JUnitWriter(TestSequence testSequence) {
        super(testSequence);
    }

    @Override
    public String getOutputFormatName() {
        return "junit";
    }

    @Override
    public void write() throws IOException {
        throw new InternalError("The JUnitWriter is not available yet.");
    }
}
