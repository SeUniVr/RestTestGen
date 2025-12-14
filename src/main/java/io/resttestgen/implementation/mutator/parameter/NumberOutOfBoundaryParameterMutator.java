package io.resttestgen.implementation.mutator.parameter;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.testing.mutator.ParameterMutator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NumberOutOfBoundaryParameterMutator extends ParameterMutator {

    private static final Logger logger = LogManager.getLogger(NumberOutOfBoundaryParameterMutator.class);

    @Override
    public boolean isParameterMutable(Parameter parameter) {
        return parameter instanceof NumberParameter && ((NumberParameter) parameter).getConcreteValue() != null;
    }

    @Override
    public Parameter mutate(Parameter parameter) {

        NumberParameter numberParameter = (NumberParameter) parameter;

        if (isParameterMutable(numberParameter)) {
            if (Environment.getInstance().getRandom().nextBoolean()) {
                numberParameter.setValueManually(numberParameter.getMinimumRepresentableValue() - 1);
            } else {
                numberParameter.setValueManually(numberParameter.getMaximumRepresentableValue() + 1);
            }
        } else {
            logger.warn("The provided parameter cannot be mutated because it does not provide constraints to violate.");
        }
        return parameter;
    }

    @Override
    public boolean isErrorMutator() {
        return true;
    }
}
