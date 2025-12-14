package io.resttestgen.implementation.strategy;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.testing.Strategy;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ValueNotAvailableException;
import io.resttestgen.implementation.parametervalueprovider.single.LlmParameterValueProvider;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class InitLlmDictionaryStrategy extends Strategy {

    private static final Logger logger = LogManager.getLogger(InitLlmDictionaryStrategy.class);

    @Override
    public void start() {

        // Find all string and number parameters of the API
        List<LeafParameter> parameters = Environment.getInstance().getOpenAPI().getOperations().stream()
                .flatMap(o -> o.getReferenceLeaves().stream())
                .filter(p -> p instanceof StringParameter || p instanceof NumberParameter)
                .collect(Collectors.toList());

        int total = parameters.size();
        int count = 1;

        logger.info("There are {} parameters in the OpenAPI specification of {}.", total, Environment.getInstance().getApiUnderTest().getName());

        int str = 0;
        int num = 0;
        int bool = 0;
        int nul = 0;
        int gen = 0;

        for (LeafParameter leafParameter : parameters) {
            if (leafParameter instanceof StringParameter) {
                str++;
            } else if (leafParameter instanceof NumberParameter) {
                num++;
            } else if (leafParameter instanceof BooleanParameter) {
                bool++;
            } else if (leafParameter instanceof NullParameter) {
                nul++;
            } else if (leafParameter instanceof GenericParameter) {
                gen++;
            }
        }

        logger.info("Strings: {}, numbers: {}, booleans: {}, nulls: {}, generics: {}.", str, num, bool, nul, gen);

        // Initialize LLM parameter value provider
        LlmParameterValueProvider provider = new LlmParameterValueProvider();

        // Iterate on parameter and call LLM parameter value provider.
        for (LeafParameter p : parameters) {
            try {
                logger.info("[{}/{}] Generating values for parameter '{}' in operation {}", count, total, p.getName(), p.getOperation());
                Pair<ParameterValueProvider, Object> pair = provider.provideValueFor(p);
                logger.info("Completed. One of the generated value is: {}", pair.getSecond());
                count++;
            } catch (ValueNotAvailableException ignored) {
                logger.warn("Could not generate values for this parameter (not a string or number? did you enable the LLM in the value provider?)");
            }
        }
    }
}
