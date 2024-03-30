package io.resttestgen.implementation.mutator.parameter;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.testing.mutator.ParameterMutator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MissingRequiredParameterMutator extends ParameterMutator {

    private static final Logger logger = LogManager.getLogger(MissingRequiredParameterMutator.class);

    /**
     * In order to be mutable, a parameter must be required
     * @param parameter the parameter to check
     * @return true if the parameter is mutable, false otherwise
     */
    @Override
    public boolean isParameterMutable(Parameter parameter) {
        return parameter.isRequired();
    }

    /**
     * Removes the value from the mandatory parameter. The request manager will
     * @param parameter the parameter to mutate.
     * @return the leaf with mutated value.
     */
    @Override
    public Parameter mutate(Parameter parameter) {
        if (isParameterMutable(parameter)) {
            if (parameter instanceof LeafParameter) {
                ((LeafParameter) parameter).removeValue();
            }

            // FIXME: the following are non-reversible, because parameter are removed from operation. Find reversible way.
            else if (parameter instanceof ArrayParameter) {
                ((ArrayParameter) parameter).clearElements();
            } else {
                parameter.remove();
            }
        } else {
            logger.warn("Cannot apply mutation. This parameter is not mandatory.");
        }
        return parameter;
    }

    @Override
    public boolean isErrorMutator() {
        return true;
    }
}
