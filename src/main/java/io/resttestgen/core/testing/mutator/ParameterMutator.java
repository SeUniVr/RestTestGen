package io.resttestgen.core.testing.mutator;

import io.resttestgen.core.datatype.parameter.Parameter;

import java.util.Collection;
import java.util.stream.Collectors;

public abstract class ParameterMutator extends Mutator {

    /**
     * Given a list of parameters, returns a sublist containing the mutable parameters only.
     * @param parameters the list of parameters to check
     * @return the list of mutable parameters
     */
    public Collection<Parameter> getMutableParameters(Collection<Parameter> parameters) {
        return parameters.stream().filter(this::isParameterMutable).collect(Collectors.toList());
    }

    /**
     * Check if a parameter is mutable.
     * @param parameter the parameter to check.
     * @return true if the passed parameter is mutable.
     */
    public abstract boolean isParameterMutable(Parameter parameter);

    /**
     * Apply the mutation to a parameter
     * @param parameter the parameter to mutate
     * @return the mutated parameter
     */
    public abstract Parameter mutate(Parameter parameter);
}
