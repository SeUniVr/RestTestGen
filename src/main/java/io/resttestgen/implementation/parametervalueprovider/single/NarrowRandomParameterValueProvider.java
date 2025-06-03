package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import kotlin.Pair;

/**
 * Generates a random value for the given parameter in a narrower bound. For examples, numbers are not picked from the
 * range (-MAX, +MAX), but rather from something narrower like (0, 120).
 */
public class NarrowRandomParameterValueProvider extends ParameterValueProvider {

    private static final ExtendedRandom random = Environment.getInstance().getRandom();

    private static final double NUMBER_LOWER_BOUND = 0.0;
    private static final double NUMBER_UPPER_BOUND = 120.0;

    @Override
    public Pair<ParameterValueProvider, Object> provideValueFor(LeafParameter leafParameter) {
        return new Pair<>(this, generateValueFor(leafParameter));
    }

    private Object generateValueFor(LeafParameter leafParameter) {

        // Narrow random provider can only be applied to numbers
        if (leafParameter instanceof NumberParameter) {
            return generateCompliantNumber((NumberParameter) leafParameter);
        } else {
            RandomParameterValueProvider randomParameterValueProvider = new RandomParameterValueProvider();
            return randomParameterValueProvider.provideValueFor(leafParameter).getSecond();
        }
    }

    private Number generateCompliantNumber(NumberParameter parameter) {

        // Get the actual format, or infer it
        ParameterTypeFormat format = parameter.getOrInferFormat();

        // User restrictive values, unless defined min and max are more restrictive
        Double min = parameter.getMinimum() == null ? NUMBER_LOWER_BOUND : Math.max(NUMBER_LOWER_BOUND, parameter.getMinimum());
        Double max = parameter.getMaximum() == null ? NUMBER_UPPER_BOUND : Math.min(NUMBER_UPPER_BOUND, parameter.getMaximum());

        // If min is not less than max, reset one of the two variables randomly
        if (min > max) {
            if (random.nextBoolean()) {
                min = NUMBER_LOWER_BOUND;
            } else {
                max = NUMBER_UPPER_BOUND;
            }
        }

        // If the parameter is a double
        if (format == ParameterTypeFormat.DOUBLE) {

            // Exclude values if minimum or maximum are exclusive
            min = parameter.isExclusiveMinimum() ? min + Double.MIN_VALUE : min;
            max = parameter.isExclusiveMaximum() ? max - Double.MIN_VALUE : max;

            // Generate and return the value
            return random.nextDouble(min, max);
        }

        // If the parameter is a float
        else if (format == ParameterTypeFormat.FLOAT) {

            // Exclude values if minimum or maximum are exclusive
            min = parameter.isExclusiveMinimum() ? min + Float.MIN_VALUE : min;
            max = parameter.isExclusiveMaximum() ? max - Float.MIN_VALUE : max;

            // Restrict the boundaries
            min = Math.max((float) NUMBER_LOWER_BOUND, min);
            max = Math.min((float) NUMBER_UPPER_BOUND, max);

            Double value = random.nextDouble(min, max);

            // Cut decimal digits with 50% probability
            if (random.nextBoolean()) {
                int decimalDigits = random.nextLength(0, 5);
                value = Math.floor(value * Math.pow(10, decimalDigits)) / Math.pow(10, decimalDigits);
            }

            return value.floatValue();
        }

        // If the parameter is an integer or long
        else {

            // Is the parameter a long or an integer?
            boolean isLong = format == ParameterTypeFormat.INT64 || format == ParameterTypeFormat.UINT64;

            // Exclude values if minimum or maximum are exclusive
            min = parameter.isExclusiveMinimum() ? min + 1 : min;
            max = parameter.isExclusiveMaximum() ? max - 1 : max;

            if (isLong) {
                return random.nextLong(min.longValue(), max.longValue());
            } else {
                return random.nextInt(min.intValue(), min.intValue());
            }
        }
    }
}
