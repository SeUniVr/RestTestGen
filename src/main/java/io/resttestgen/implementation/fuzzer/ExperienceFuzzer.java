package io.resttestgen.implementation.fuzzer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.helper.Experience;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Fuzzer;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.core.testing.parametervalueprovider.ValueNotAvailableException;
import io.resttestgen.implementation.helper.InterParameterDependenciesHelper;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import io.resttestgen.implementation.parametervalueprovider.multi.ExperienceDrivenMultiParameterValueProvider;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.resttestgen.core.datatype.parameter.ParameterUtils.getArrays;
import static io.resttestgen.core.datatype.parameter.ParameterUtils.isArrayOfLeaves;

/**
 * The Nominal Fuzzer generates nominal test sequences
 */
public class ExperienceFuzzer extends Fuzzer {

    private static final Logger logger = LogManager.getLogger(ExperienceFuzzer.class);
    private static final ExtendedRandom random = Environment.getInstance().getRandom();
    private static final Experience experience = Environment.getInstance().getExperience();
    private static final double epsilon = 0.1;

    private final Operation operation;
    private Operation editableOperation;
    private final ExperienceDrivenMultiParameterValueProvider parameterValueProvider = (ExperienceDrivenMultiParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.EXPERIENCE);
    private boolean strict = false;

    public ExperienceFuzzer(Operation operation) {
        this.operation = operation;
    }

    public List<TestSequence> generateTestSequences(int numberOfSequences) {
        List<TestSequence> testSequences = new LinkedList<>();
        if (numberOfSequences > 0) {
            for (int i = 0; i < numberOfSequences; i++) {
                testSequences.add(generateTestSequence());
            }
            return testSequences;
        }
        logger.warn("You asked for a list of sequences with {} elements. Returned an empty list.", numberOfSequences);
        return testSequences;
    }

    /**
     * Generate a single test sequence, composed by a single nominal test interaction.
     * @return the generated test sequence
     */
    private TestSequence generateTestSequence() {

        editableOperation = operation.deepClone();
        InterParameterDependenciesHelper idpHelper = new InterParameterDependenciesHelper(editableOperation, parameterValueProvider);

        boolean useMoreParameters = random.nextInt(10) < 2;

        resolveCombinedSchemas();
        populateArrays();
        idpHelper.extractExampleValuesFromRequiresIpds();
        setValueToLeaves(useMoreParameters);
        idpHelper.applyInterParameterDependencies();

        // Create a test interaction from the operation
        TestInteraction testInteraction = new TestInteraction(editableOperation);

        // Encapsulate test interaction into test sequence
        TestSequence testSequence = new TestSequence(this, testInteraction);
        String sequenceName = !editableOperation.getOperationId().isEmpty() ?
                editableOperation.getOperationId() :
                editableOperation.getMethod().toString() + "-" + editableOperation.getEndpoint();
        testSequence.setName(sequenceName);
        testSequence.appendGeneratedAtTimestampToSequenceName();

        // Create and return test sequence containing the test interaction
        return testSequence;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
        this.parameterValueProvider.setStrict(strict);
    }

    public void setValue(Parameter parameter) {
        try {
            if (parameter instanceof LeafParameter) {
                ((LeafParameter) parameter).setValueWithProvider(parameterValueProvider);
            } else if (isArrayOfLeaves(parameter)) {
                ArrayParameter arrayParameter = (ArrayParameter) parameter;
                arrayParameter.setValuesFromCommaSeparatedString(parameterValueProvider.provideValueFor((LeafParameter) arrayParameter.getReferenceElement()).toString());
            }
        } catch (ValueNotAvailableException e) {
            logger.warn("Parameter value provider could not find a value for parameter: " + parameter);
        }
    }

    public void removeValue(Parameter parameter) {
        if (parameter instanceof LeafParameter) {
            ((LeafParameter) parameter).removeValue();
        } else if (isArrayOfLeaves(parameter)) {
            ((ArrayParameter) parameter).clearElements();
        }
    }


    /**
     * Resolves combines schemas. Not yet supported
     * TODO: implement
     */
    public void resolveCombinedSchemas() {}

    /**
     * Fills arrays with copies of their reference element.
     * TODO: support uniqueItems: true
     */
    public void populateArrays() {
        Collection<ArrayParameter> arrays = editableOperation.getArrays();
        LinkedList<ArrayParameter> queue = new LinkedList<>(arrays);
        while (!queue.isEmpty()) {
            ArrayParameter array = queue.getFirst();
            int size = getArraySizeFromExperience(array);

            for (int i = 0; i < size; i++) {
                Parameter referenceElementCopy = array.getReferenceElement().deepClone();
                array.addElement(referenceElementCopy);
                queue.addAll(getArrays(referenceElementCopy));
            }
            queue.remove(array);
        }
    }

    /**
     * Set value to the leaves in the operation. For non required leaves, the values is assigned according to the
     * collected experience
     * @param useMoreParameters if true, non-required parameters will be included with higher probability
     */
    public void setValueToLeaves(boolean useMoreParameters) {

        // Set epsilon of provider with same epsilon as this class
        parameterValueProvider.setEpsilon(epsilon);

        Collection<LeafParameter> leaves = editableOperation.getLeaves();
        for (LeafParameter leaf : leaves) {

            // Decide whether to keep a leaf based on experience
            if (getParameterPresenceFromExperience(leaf, useMoreParameters)) {
                leaf.setValueWithProvider(parameterValueProvider);
            }
        }
    }

    /**
     * Provides the size of an array based on experience. There are three available size classes:
     * - 0: array has size 0 (is not rendered in request)
     * - 1: array has size 1
     * - 2: array has random size between minItems and maxItems
     * @param arrayParameter the array for which the size has to be provided
     * @return the actual size (not size class), e.g., 0, 1, 4, 10, 24.
     */
    private int getArraySizeFromExperience(ArrayParameter arrayParameter) {

        // Initialize sizeClass to 0
        int sizeClass = 0;

        // Ignore experience if no experience is available for parameter, or if epsilon matches
        if (random.nextDouble() <= epsilon || experience.getArraySizeExperienceForNormalizedName(arrayParameter.getNormalizedName()) == null) {

            double choice = random.nextDouble();

            // With 50% probability size is 0 -> no need to change sizeClass which is already 0

            // With 30% probability size is 1
            if (choice > 0.5 && choice <= 0.8) {
                sizeClass = 1;
            }

            // With 20% probability size is random according to maxItems and minItems
            else if (choice > 0.8) {
                sizeClass = 2;
            }
        }

        // Conversely, use experience to choose the most suitable size class for the array
        else {

            HashMap<Integer, Integer> sizeExperience = experience.getArraySizeExperienceForNormalizedName(arrayParameter.getNormalizedName());
            int total = sizeExperience.get(0) + sizeExperience.get(1) + sizeExperience.get(2);
            int choice = random.nextInt(total);

            if (choice > sizeExperience.get(0) && choice <= (sizeExperience.get(0) + sizeExperience.get(1))) {
                sizeClass = 1;
            }

            else if (choice > (sizeExperience.get(0) + sizeExperience.get(1))) {
                sizeClass = 2;
            }
        }

        // Return size (equal to sizeClass for 0 and 1, and computed if sizeClass == 2
        return sizeClass == 2 ? random.nextShortLength(arrayParameter.getMinItems(), arrayParameter.getMaxItems()) : sizeClass;
    }

    /**
     * Determine to use a leaf parameter ina request based on experience.
     * @param leafParameter the subject parameter.
     * @return true if the parameter should be used in the request.
     */
    private boolean getParameterPresenceFromExperience(LeafParameter leafParameter, boolean useMoreParameters) {

        // Always use required parameters
        if (leafParameter.isRequired()) {
            return true;
        }

        // Always use leaves that are array elements, as their presence is determined by array size
        if (ParameterUtils.isArrayElement(leafParameter)) {
            return true;
        }

        // Ignore experience if no experience is available for parameter, or if epsilon matches
        if (random.nextDouble() <= epsilon || experience.getSimpleParameterPresenceExperienceForNormalizedName(leafParameter.getNormalizedName()) == null) {

            double threshold = 0.2;
            if (useMoreParameters) {
                threshold = 0.5;
            }

            return random.nextDouble() <= threshold;
        }

        // Use parameter in request with probability equal to observed experience
        Pair<AtomicInteger, AtomicInteger> presenceExperience = experience.getSimpleParameterPresenceExperienceForNormalizedName(leafParameter.getNormalizedName());
        return random.nextDouble() <= (((double) presenceExperience.getSecond().get()) / ((double) presenceExperience.getFirst().get()));
    }
}
