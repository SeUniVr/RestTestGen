package io.resttestgen.core.testing;

public abstract class Oracle {

    @SuppressWarnings("UnusedReturnValue")
    public abstract TestResult assertTestSequence(TestSequence testSequence);

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
