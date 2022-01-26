package io.resttestgen.core.testing;

import io.resttestgen.core.Environment;

public abstract class Strategy {

    protected Environment environment;

    public Strategy(Environment environment) {
        this.environment = environment;
    }

    public abstract void start();
}
