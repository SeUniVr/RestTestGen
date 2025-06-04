package io.resttestgen.implementation.strategy.configuration;

import io.resttestgen.core.testing.StrategyConfiguration;

public class NominalAndErrorStrategyConfiguration extends StrategyConfiguration {

    private long timeBudget = 0;
    private int numberOfSequences = 20;

    public long getTimeBudget() {
        return timeBudget;
    }

    public void setTimeBudget(long timeBudget) {
        this.timeBudget = timeBudget;
    }

    public int getNumberOfSequences() {
        return numberOfSequences;
    }

    public void setNumberOfSequences(int numberOfSequences) {
        this.numberOfSequences = numberOfSequences;
    }
}
