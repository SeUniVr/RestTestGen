package io.resttestgen.implementation.mutator;

import io.resttestgen.core.testing.Mutator;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MissingRequiredMutator extends Mutator {

    private static final Logger logger = LogManager.getLogger(MissingRequiredMutator.class);

    /**
     * In order to be mutable, a parameter must be required
     * @param parameter the parameter to check
     * @return true if the parameter is mutable, false otherwise
     */
    @Override
    public boolean isParameterMutable(ParameterLeaf parameter) {
        return parameter.isRequired();
    }

    /**
     * Removes the value from the mandatory parameter. The request manager will
     * @param parameter the parameter to mutate.
     * @return the leaf with mutated value.
     */
    @Override
    public ParameterLeaf mutate(ParameterLeaf parameter) {
        if (isParameterMutable(parameter)) {
            parameter.removeValue();
        } else {
            logger.warn("Cannot apply mutation. This parameter is not mandatory.");
        }
        return parameter;
    }
}
