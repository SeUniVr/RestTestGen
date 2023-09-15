package io.resttestgen.implementation.mutator;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.BooleanParameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.Mutator;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WrongTypeMutator extends Mutator {

    private static final Logger logger = LogManager.getLogger(WrongTypeMutator.class);

    private final ParameterValueProvider valueProvider = ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM);

    @Override
    public boolean isParameterMutable(LeafParameter parameter) {
        return parameter instanceof StringParameter || parameter instanceof NumberParameter ||
                parameter instanceof BooleanParameter;
    }

    @Override
    public LeafParameter mutate(LeafParameter parameter) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        LeafParameter mutatedParameter;

        if (parameter instanceof StringParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new NumberParameter(parameter);
            } else {
                mutatedParameter = new BooleanParameter(parameter);
            }
            mutatedParameter.setValueWithProvider(valueProvider);
            return mutatedParameter;
        } else if (parameter instanceof NumberParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new StringParameter(parameter);
            } else {
                mutatedParameter = new BooleanParameter(parameter);
            }
            mutatedParameter.setValueWithProvider(valueProvider);
            return mutatedParameter;
        } else if (parameter instanceof BooleanParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new StringParameter(parameter);
            } else {
                mutatedParameter = new NumberParameter(parameter);
            }
            mutatedParameter.setValueWithProvider(valueProvider);
            return mutatedParameter;
        } else {
            logger.warn("Could not apply mutation. This parameter is not of a mutable type.");
            return parameter;
        }
    }
}
