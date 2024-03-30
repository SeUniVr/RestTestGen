package io.resttestgen.core.testing.mutator;

public abstract class Mutator {

    /**
     * Methods that implementations have to implement by returning true if the mutator in an "error" mutator, meaning
     * that it will mutate against the specification.
     * @return true if the applied mutation will go against the specification.
     */
    public abstract boolean isErrorMutator();

    public String toString() {
        return this.getClass().getSimpleName();
    }
}
