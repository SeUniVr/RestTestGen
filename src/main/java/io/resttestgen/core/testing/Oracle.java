package io.resttestgen.core.testing;

public abstract class Oracle {

    public abstract TestResult assertTestSequence(TestSequence testSequence);

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
