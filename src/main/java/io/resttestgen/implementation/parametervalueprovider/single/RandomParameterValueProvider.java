package io.resttestgen.implementation.parametervalueprovider.single;

import com.mifmif.common.regex.Generex;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Generates a random value for the given parameter.
 */
public class RandomParameterValueProvider extends ParameterValueProvider {

    private static final Logger logger = LogManager.getLogger(RandomParameterValueProvider.class);

    private static final ExtendedRandom random = Environment.getInstance().getRandom();

    @Override
    public Pair<ParameterValueProvider, Object> provideValueFor(LeafParameter leafParameter) {
        return new Pair<>(this, generateValueFor(leafParameter));
    }

    private Object generateValueFor(LeafParameter leafParameter) {

        if (leafParameter instanceof StringParameter) {
            return generateCompliantString((StringParameter) leafParameter);
        } else if (leafParameter instanceof NumberParameter) {
            return generateCompliantNumber((NumberParameter) leafParameter);
        } else if (leafParameter instanceof BooleanParameter) {
            return random.nextBoolean();
        } else if (leafParameter instanceof NullParameter) {
            return null;
        } else {
            switch (random.nextInt(0, 5)) {
                case 0:
                    return random.nextString();
                case 1:
                    return random.nextInt();
                case 2:
                    return random.nextDouble(-100000., 100000.);
                case 3:
                    return random.nextBoolean();
                default:
                    return null;
            }
        }
    }

    private String generateCompliantString(StringParameter parameter) {

        // If pattern (regex) is provided for the string, use it with 80% probability
        if (parameter.getPattern() != null && !parameter.getPattern().isEmpty() && random.nextInt(10) < 8) {

            String pattern = parameter.getPattern();

            // Clean pattern if it starts with ^ and ends with $
            if (pattern.startsWith("^") && pattern.endsWith("$")) {
                pattern = pattern.substring(1, pattern.length() - 1);
            }

            // Compute values of minLength and maxLength in the case they are null
            int min = parameter.getMinLength() == null || parameter.getMinLength() < 0 ? 0 : parameter.getMinLength();
            int max = parameter.getMaxLength() == null || parameter.getMaxLength() < min ? min + random.nextInt(20) : parameter.getMaxLength();


            try {
                Generex generex = new Generex(pattern);
                String generated = generex.random(min, max);
                System.out.println("GENERATED: " + generated);
                return generated;
            }

            // If the pattern is invalid, ignore it and continue with standard random generation
            catch (IllegalArgumentException e) {
                logger.warn("The specified pattern ({}) for parameter {} is invalid. Ignoring it.", parameter.getPattern(), parameter);
            }

            // Catch stack overflows
            catch (StackOverflowError e) {
                logger.warn("Generating a value for pattern {} cause a StackOverflowError. Generating now a random string.", pattern);
            }
        }

        // Generate a random length according to the provided bounds
        int length = random.nextLength(parameter.getMinLength(), parameter.getMaxLength());

        // Generate a random string in multiple format
        String generatedString = random.nextString(length);

        // Replace the generated string with the actual correct format in 90% of the cases
        if (random.nextInt(10) < 9) {
            switch (parameter.inferFormat()) {
                case BYTE:
                    generatedString = random.nextBase64();
                    break;
                case BINARY:
                    generatedString = random.nextBinaryString();
                    break;
                case DATE:
                    generatedString = random.nextDate();
                    break;
                case DATE_TIME:
                    generatedString = random.nextDateTime();
                    break;
                case TIME:
                    generatedString = random.nextTime();
                    break;
                case DURATION:
                    generatedString = random.nextTimeDuration();
                    break;
                case PASSWORD:
                    generatedString = random.nextRandomString(length);
                    break;
                case HOSTNAME:
                    generatedString = random.nextDomain(true);
                    break;
                case URI:
                    generatedString = random.nextURI();
                    break;
                case UUID:
                    generatedString = random.nextUUID();
                    break;
                case IPV4:
                    generatedString = random.nextIPV4();
                    break;
                case IPV6:
                    generatedString = random.nextIPV6();
                    break;
                case EMAIL:
                    generatedString = random.nextEmail();
                    break;
                case PHONE:
                    generatedString = random.nextPhoneNumber();
                    break;
                case IBAN:
                    generatedString = random.nextIBAN();
                    break;
                case SSN:
                    generatedString = random.nextSSN();
                    break;
                case FISCAL_CODE:
                    // TODO Add Fiscal Code
                    generatedString = random.nextString(length);
                    break;
                case LOCATION:
                    generatedString = random.nextLocation();
                    break;
                default:
                    generatedString = random.nextString(length);
            }
        }

        return generatedString;
    }

    private Number generateCompliantNumber(NumberParameter parameter) {

        // Get the actual format, or infer it
        ParameterTypeFormat format = parameter.getOrInferFormat();

        // If the parameter is a double
        if (format == ParameterTypeFormat.DOUBLE) {

            // Set min and max value, if defined
            double min = parameter.getMinimum() != null ? parameter.getMinimum() : -Double.MAX_VALUE;
            double max = parameter.getMaximum() != null ? parameter.getMaximum() : Double.MAX_VALUE;

            // Exclude values if minimum or maximum are exclusive
            min = parameter.isExclusiveMinimum() ? min + Double.MIN_VALUE : min;
            max = parameter.isExclusiveMaximum() ? max - Double.MIN_VALUE : max;

            // If min is not less than max, reset one of the two variables randomly
            if (min > max) {
                if (random.nextBoolean()) {
                    min = -Double.MAX_VALUE;
                } else {
                    max = Double.MAX_VALUE;
                }
            }

            // Generate and return the value
            return random.nextDouble(min, max);
        }

        // If the parameter is a float
        else if (format == ParameterTypeFormat.FLOAT) {

            // Set min and max value, if defined
            float min = parameter.getMinimum() != null ? parameter.getMinimum().floatValue() : -Float.MAX_VALUE;
            float max = parameter.getMaximum() != null ? parameter.getMaximum().floatValue() : Float.MAX_VALUE;

            // Exclude values if minimum or maximum are exclusive
            min = parameter.isExclusiveMinimum() ? min + Float.MIN_VALUE : min;
            max = parameter.isExclusiveMaximum() ? max - Float.MIN_VALUE : max;

            // If min is not less than max, reset one of the two variables randomly
            if (min > max) {
                if (random.nextBoolean()) {
                    min = -Float.MAX_VALUE;
                } else {
                    max = Float.MAX_VALUE;
                }
            }

            return random.nextFloat(min, max);
        }

        // If the parameter is an integer or long
        else {

            // Is the parameter a long or an integer?
            boolean isLong = format == ParameterTypeFormat.INT64 || format == ParameterTypeFormat.UINT64;

            // Default
            long min = (long) parameter.getMinimumRepresentableValue();
            long max = (long) parameter.getMaximumRepresentableValue();

            // Set min and max value, if defined
            min = parameter.getMinimum() != null ? parameter.getMinimum().longValue() : min;
            max = parameter.getMaximum() != null ? parameter.getMaximum().longValue() : max;

            // Exclude values if minimum or maximum are exclusive
            min = parameter.isExclusiveMinimum() ? min + 1 : min;
            max = parameter.isExclusiveMaximum() ? max - 1 : max;

            // If min is not less than max, reset one of the two variables randomly
            if (min > max) {
                if (random.nextBoolean()) {
                    min = Long.MIN_VALUE;
                } else {
                    max = Long.MAX_VALUE;
                }
            }

            if (isLong) {
                return random.nextLong(min, max);
            } else {
                return random.nextInt((int) min, (int) max);
            }
        }
    }
}
