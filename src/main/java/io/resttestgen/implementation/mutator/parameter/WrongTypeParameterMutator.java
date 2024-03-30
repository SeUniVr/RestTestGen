package io.resttestgen.implementation.mutator.parameter;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.BooleanParameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.mutator.ParameterMutator;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WrongTypeParameterMutator extends ParameterMutator {

    private static final Logger logger = LogManager.getLogger(WrongTypeParameterMutator.class);

    private final ParameterValueProvider valueProvider = ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM);

    @Override
    public boolean isParameterMutable(Parameter parameter) {
        return parameter instanceof StringParameter || parameter instanceof NumberParameter ||
                parameter instanceof BooleanParameter;
    }

    @Override
    public Parameter mutate(Parameter parameter) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        LeafParameter mutatedParameter;

        if (parameter instanceof StringParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new NumberParameter(parameter);
            } else {
                mutatedParameter = new BooleanParameter(parameter);
            }
            mutatedParameter.setValueWithProvider(valueProvider);
            parameter.replace(mutatedParameter);
            return mutatedParameter;
        } else if (parameter instanceof NumberParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new StringParameter(parameter);
            } else {
                mutatedParameter = new BooleanParameter(parameter);
            }
            mutatedParameter.setValueWithProvider(valueProvider);
            parameter.replace(mutatedParameter);
            return mutatedParameter;
        } else if (parameter instanceof BooleanParameter) {
            if (random.nextBoolean()) {
                mutatedParameter = new StringParameter(parameter);
            } else {
                mutatedParameter = new NumberParameter(parameter);
            }
            mutatedParameter.setValueWithProvider(valueProvider);
            parameter.replace(mutatedParameter);
            return mutatedParameter;
        } else {
            logger.warn("Could not apply mutation. This parameter is not of a mutable type.");
            return parameter;
        }
    }

    @Override
    public boolean isErrorMutator() {
        return true;
    }
}
