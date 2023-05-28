package io.resttestgen.implementation.mutator;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.testing.Mutator;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.implementation.parametervalueprovider.single.RandomParameterValueProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConstraintViolationMutator extends Mutator {

    private static final Logger logger = LogManager.getLogger(ConstraintViolationMutator.class);
    private static final ParameterValueProvider valueProvider = new RandomParameterValueProvider();

    @Override
    public boolean isParameterMutable(LeafParameter parameter) {
        if (parameter.getValue() == null) {
            return false;
        }

        // Check if parameter is an enum
        if (parameter.isEnum() && parameter.getEnumValues().size() > 0) {
            return true;
        }

        // Check if string parameters have length constraints
        else if (parameter instanceof StringParameter) {
            return ((StringParameter) parameter).getMaxLength() != null ||
                    (((StringParameter) parameter).getMinLength() != null &&
                            ((StringParameter) parameter).getMinLength() > 0);
        }

        // Check if number parameters have value constraints
        else if (parameter instanceof NumberParameter) {
            return ((NumberParameter) parameter).getMinimum() != null ||
                    ((NumberParameter) parameter).getMaximum() != null;
        }

        // Others parameters are not mutable
        return false;
    }

    @Override
    public LeafParameter mutate(LeafParameter parameter) {
        if (isParameterMutable(parameter)) {
            if (parameter.isEnum() && parameter.getEnumValues().size() > 0) {
                mutateEnum(parameter);
            } else if (parameter instanceof StringParameter) {
                mutateString((StringParameter) parameter);
            } else if (parameter instanceof NumberParameter) {
                mutateNumber((NumberParameter) parameter);
            }
        } else {
            logger.warn("The provided parameter cannot be mutated because it does not provide constraints to violate.");
        }
        return parameter;
    }

    /**
     * Assigns to the parameter a value outside the enum constraints
     * @param parameter the parameter to mutate
     */
    private void mutateEnum(LeafParameter parameter) {
        // Set a random value to the parameter (it is very unlikely that the generated value belongs to the enum values
        // FIXME: check that the generated value does not belong to the enum values
        parameter.setValue(valueProvider.provideValueFor(parameter));

        //TODO: remove: it is replaced by RandomValueProvider
        //parameter.setValue(parameter.generateCompliantValue());
    }

    /**
     * String length is changed to violate length constraints
     * @param parameter the parameter to mutate
     */
    private void mutateString(StringParameter parameter) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        // Save to this list the possible lengths of the mutated string
        List<Integer> lengths = new ArrayList<>();
        if (parameter.getMinLength() != null && parameter.getMinLength() > 1) {
            lengths.add(random.nextLength(0, parameter.getMinLength()));
        }
        if (parameter.getMaxLength() != null && parameter.getMaxLength() < Integer.MAX_VALUE - 1) {
            lengths.add(random.nextLength(parameter.getMaxLength() + 1, Integer.MAX_VALUE));
        }

        // Choose a random length
        Optional<Integer> chosenLength = random.nextElement(lengths);

        // If the current value is longer, just cut the string
        if (chosenLength.isPresent() && ((String) parameter.getValue()).length() > chosenLength.get()) {
            parameter.setValue(((String) parameter.getValue()).substring(0, chosenLength.get()));
        }

        // If the current value is shorter, add random characters
        else if (chosenLength.isPresent() && ((String) parameter.getValue()).length() < chosenLength.get()) {
            parameter.setValue(parameter.getValue() +
                    random.nextRandomString(chosenLength.get() - ((String) parameter.getValue()).length()));
        }
    }

    /**
     * Number value is changed to violate value constraints
     * FIXME: generate number (superclass) instead of double
     * @param parameter the parameter to mutate
     */
    private void mutateNumber(NumberParameter parameter) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        List<Double> values = new ArrayList<>();
        if (parameter.getMinimum() != null && parameter.getMinimum() > Double.MIN_VALUE) {
            values.add(random.nextDouble(Double.MIN_VALUE, parameter.getMinimum()));
        }
        if (parameter.getMaximum() != null && parameter.getMaximum() < Double.MAX_VALUE) {
            values.add(random.nextDouble(parameter.getMaximum(), Double.MAX_VALUE));
        }

        Optional<Double> chosenValue = random.nextElement(values);
        chosenValue.ifPresent(parameter::setValue);
    }
 }
