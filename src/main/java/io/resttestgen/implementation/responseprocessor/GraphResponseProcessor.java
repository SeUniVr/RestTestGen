package io.resttestgen.implementation.responseprocessor;

import io.resttestgen.core.testing.ResponseProcessor;
import io.resttestgen.core.testing.TestInteraction;

public class GraphResponseProcessor implements ResponseProcessor {

    @Override
    public void process(TestInteraction testInteraction) {
        throw new InternalError("The GraphResponseProcessor is not available yet.");
    }
}
