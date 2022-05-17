package io.resttestgen.implementation.mutator;

import io.resttestgen.core.Environment;
import io.resttestgen.core.testing.Mutator;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.helper.ExtendedRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WrongTypeMutator extends Mutator {

    private static final Logger logger = LogManager.getLogger(WrongTypeMutator.class);

    @Override
    public boolean isParameterMutable(ParameterLeaf parameter) {
        return parameter instanceof StringParameter || parameter instanceof NumberParameter ||
                parameter instanceof BooleanParameter;
    }

    @Override
    public ParameterLeaf mutate(ParameterLeaf parameter) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        ParameterLeaf mutatedParameter;

        if (parameter instanceof StringParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new NumberParameter(parameter);
            } else {
                mutatedParameter = new BooleanParameter(parameter);
            }
            mutatedParameter.generateCompliantValue();
            return mutatedParameter;
        } else if (parameter instanceof NumberParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new StringParameter(parameter);
            } else {
                mutatedParameter = new BooleanParameter(parameter);
            }
            mutatedParameter.generateCompliantValue();
            return mutatedParameter;
        } else if (parameter instanceof BooleanParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new StringParameter(parameter);
            } else {
                mutatedParameter = new NumberParameter(parameter);
            }
            mutatedParameter.generateCompliantValue();
            return mutatedParameter;
        } else {
            logger.warn("Could not apply mutation. This parameter is not of a mutable type.");
            return parameter;
        }
    }
}
