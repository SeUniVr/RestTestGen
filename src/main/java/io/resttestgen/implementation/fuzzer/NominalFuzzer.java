package io.resttestgen.implementation.fuzzer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.ParameterArray;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Fuzzer;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.implementation.parametervalueprovider.multi.EnumAndExamplePriorityParameterValueProvider;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The Nominal Fuzzer generates nominal test sequences
 */
public class NominalFuzzer extends Fuzzer {

    private static final Logger logger = LogManager.getLogger(NominalFuzzer.class);
    private static final ExtendedRandom random = Environment.getInstance().getRandom();

    private final Operation operation;
    private ParameterValueProvider parameterValueProvider = new EnumAndExamplePriorityParameterValueProvider();
    private boolean strict = false;
    
    public NominalFuzzer(Operation operation) {
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
        logger.warn("You asked for a list of sequences with " + numberOfSequences
                + "elements. Returned an empty list.");
        return testSequences;
    }

    /**
     * Generate a single test sequence, composed by a single nominal test interaction.
     * @return the generated test sequence
     */
    private TestSequence generateTestSequence() {

        // Get editable clone of operation
        Operation editableOperation = operation.deepClone();

        // TODO: resolve combined schemas
        /*Collection<CombinedSchemaParameter> combinedSchemas = requestManager.getCombinedSchemas();
        for (CombinedSchemaParameter combinedSchema : combinedSchemas) {

        }*/

        // Populate arrays. Use of cue to add to support concurrent modification of cue
        // TODO: support uniqueItems = true
        Collection<ParameterArray> arrays = editableOperation.getArrays();
        LinkedList<ParameterArray> queue = new LinkedList<>(arrays);
        while (!queue.isEmpty()) {
            ParameterArray array = queue.getFirst();
            int n = Environment.getInstance().getRandom().nextLength(array.getMinItems(), array.getMaxItems());

            // Remove array with a 0.7 probability
            if (random.nextInt(10) < 8) {
                n = 0;
            }

            for (int i = 0; i < n; i++) {
                ParameterElement referenceElementCopy = array.getReferenceElement().deepClone();
                array.addElement(referenceElementCopy);
                queue.addAll(referenceElementCopy.getArrays());
            }
            queue.remove(array);
        }


        // Assign values to leaves and remove random non-mandatory leaves
        Collection<ParameterLeaf> leaves = editableOperation.getLeaves();
        for (ParameterLeaf leaf : leaves) {

            // Set value with 70% probability, if parameter is not mandatory or if it is not part of an array.
            // Null parameters will be removed by the request manager
            if (leaf.isRequired() || random.nextInt(100) < 30 ||
                    (leaf.getParent() != null && leaf.getParent() instanceof ParameterArray)) {
                leaf.setValue(parameterValueProvider.provideValueFor(leaf));
            }
        }


        // Apply requires IDPs
        for (Pair<String, String> requires : editableOperation.getRequires()) {

            boolean conditionHolds = true;

            // In the case the condition requires a parameter to have a value
            if (requires.getFirst().contains("==")) {
                Pattern pattern = Pattern.compile("(.*)==(.*)");
                Matcher matcher = pattern.matcher(requires.getFirst());
                if (matcher.find()) {
                    String parameterName = matcher.group(1);
                    String parameterValue = matcher.group(2);

                    HashSet<ParameterName> parameterNames = new HashSet<>();
                    parameterNames.add(new ParameterName(parameterName));
                    List<ParameterElement> conditionParameters = collectRequestParametersWithNames(editableOperation, parameterNames);

                    // Cut out quotes from string values
                    if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                            (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                        parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                    }

                    if (conditionParameters.size() > 0) {
                        for (ParameterElement parameter : conditionParameters) {
                            if (parameter instanceof ParameterLeaf && ((ParameterLeaf) parameter).getConcreteValue() != null &&
                                    ((ParameterLeaf) parameter).getConcreteValue().toString().equals(parameterValue)) {
                                conditionHolds = false;
                            }
                        }
                    } else {
                        conditionHolds = false;
                    }
                }
            }

            // In the case the condition concerns the presence of the parameter
            else {
                HashSet<ParameterName> parameterNames = new HashSet<>();
                parameterNames.add(new ParameterName(requires.getFirst()));
                List<ParameterElement> conditionParameters = collectRequestParametersWithNames(editableOperation, parameterNames);
                if (conditionParameters.size() > 0) {
                    for (ParameterElement parameter : conditionParameters) {
                        if (parameter instanceof ParameterLeaf && ((ParameterLeaf) parameter).getConcreteValue() == null) {
                            conditionHolds = false;
                            break;
                        }
                    }
                } else {
                    conditionHolds = false;
                }
            }

            if (conditionHolds) {

                // If the requirement is a parameter with a specific value, we set the value accordingly
                if (requires.getSecond().contains("==")) {
                    Pattern pattern = Pattern.compile("(.*)==(.*)");
                    Matcher matcher = pattern.matcher(requires.getSecond());
                    if (matcher.find()) {
                        String parameterName = matcher.group(1);
                        String parameterValue = matcher.group(2);

                        HashSet<ParameterName> parameterNames = new HashSet<>();
                        parameterNames.add(new ParameterName(parameterName));
                        List<ParameterElement> conditionParameters = collectRequestParametersWithNames(editableOperation, parameterNames);

                        // Cut out quotes from string values
                        if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                            parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                        }

                        for (ParameterElement parameter : conditionParameters) {
                            if (parameter instanceof ParameterLeaf) {
                                try {
                                    Object castedValue = ObjectHelper.castToParameterValueType(parameterValue, parameter.getType());
                                    ((ParameterLeaf) parameter).setValue(castedValue);
                                } catch (ClassCastException e) {
                                    logger.warn("Could not cast value from IPD.");
                                }
                            }
                        }
                    }
                }

                // If the requirement is the presence of a parameter, we set the value of the parameter
                else {
                    HashSet<ParameterName> parameterNames = new HashSet<>();
                    parameterNames.add(new ParameterName(requires.getSecond()));
                    List<ParameterElement> conditionParameters = collectRequestParametersWithNames(editableOperation, parameterNames);

                    for (ParameterElement parameter : conditionParameters) {
                        if (parameter instanceof ParameterLeaf) {
                            ((ParameterLeaf) parameter).setValue(parameterValueProvider.provideValueFor((ParameterLeaf) parameter));
                        }
                    }
                }
            }
        }


        // Apply or IPD
        for (Set<ParameterName> or : editableOperation.getOr()) {
            List<ParameterElement> orParameters = collectRequestParametersWithNames(editableOperation, or);
            // If none of the or parameters is set, then set a value for a subset of these parameters
            if (orParameters.stream().noneMatch(p -> p instanceof ParameterLeaf &&
                    ((ParameterLeaf) p).getConcreteValue() != null)) {
                Environment.getInstance().getRandom().nextElement(orParameters).ifPresent(p ->
                        ((ParameterLeaf) p).setValue(parameterValueProvider.provideValueFor((ParameterLeaf) p)));
            }
        }

        // Apply onlyOne IPD
        for (Set<ParameterName> onlyOne : editableOperation.getOnlyOne()) {
            List<ParameterElement> onlyOneParameters = collectRequestParametersWithNames(editableOperation, onlyOne);
            List<ParameterElement> setOnlyOneParameters = filterBySetParameters(onlyOneParameters);
            // In case all parameters are not set, set one of them with value
            if (setOnlyOneParameters.size() == 0 && onlyOneParameters.size() > 0) {
                random.nextElement(onlyOneParameters).ifPresent(p -> ((ParameterLeaf) p)
                        .setValue(parameterValueProvider.provideValueFor((ParameterLeaf) p)));
            }
            // In case more than one parameter is set, just keep one and remove all the others
            else if (setOnlyOneParameters.size() > 1) {
                setOnlyOneParameters.remove(random.nextElement(setOnlyOneParameters).get());
                setOnlyOneParameters.forEach(p -> ((ParameterLeaf) p).removeValue());
            }
        }


        // Apply allOrNone IPD
        for (Set<ParameterName> allOrNone : editableOperation.getAllOrNone()) {
            List<ParameterElement> allOrNoneParameters = collectRequestParametersWithNames(editableOperation, allOrNone);
            List<ParameterElement> setAllOrNoneParameters = filterBySetParameters(allOrNoneParameters);
            if (!(setAllOrNoneParameters.size() == 0 || setAllOrNoneParameters.size() == allOrNoneParameters.size())) {
                boolean allOrNoneChoice = random.nextBoolean(); // true: all, false: none
                if (allOrNoneChoice) {
                    for (ParameterElement parameter : allOrNoneParameters) {
                        if (parameter instanceof ParameterLeaf && ((ParameterLeaf) parameter).getConcreteValue() == null) {
                            ((ParameterLeaf) parameter).setValue(parameterValueProvider.provideValueFor((ParameterLeaf) parameter));
                        }
                    }
                } else {
                    allOrNoneParameters.forEach(p -> ((ParameterLeaf) p).removeValue());
                }
            }
        }


        // Apply zeroOrOne IPD
        for (Set<ParameterName> zeroOrOne : editableOperation.getZeroOrOne()) {
            List<ParameterElement> zeroOrOneParameters = collectRequestParametersWithNames(editableOperation, zeroOrOne);
            List<ParameterElement> setZeroOrOneParameters = filterBySetParameters(zeroOrOneParameters);
            if (setZeroOrOneParameters.size() > 1) {
                boolean zeroOrOneChoice = random.nextBoolean(); // true: 1, false: 0
                if (zeroOrOneChoice) {
                    setZeroOrOneParameters.remove(random.nextElement(setZeroOrOneParameters).get());
                }
                setZeroOrOneParameters.forEach(p -> ((ParameterLeaf) p).removeValue());
            }
        }


        // Create a test interaction from the operation
        TestInteraction testInteraction = new TestInteraction(editableOperation);

        // Encapsulate test interaction into test sequence
        TestSequence testSequence = new TestSequence(this, testInteraction);
        String sequenceName = editableOperation.getOperationId().length() > 0 ?
                editableOperation.getOperationId() :
                editableOperation.getMethod().toString() + "-" + editableOperation.getEndpoint();
        testSequence.setName(sequenceName);
        testSequence.appendGeneratedAtTimestampToSequenceName();

        // Create and return test sequence containing the test interaction
        return testSequence;
    }

    public void setParameterValueProvider(ParameterValueProvider parameterValueProvider) {
        this.parameterValueProvider = parameterValueProvider;
        this.parameterValueProvider.setStrict(this.strict);
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
        this.parameterValueProvider.setStrict(strict);
    }

    /**
     * Given a set of parameter names, returns a list with the actual parameter elements with the corresponding names.
     * @param parameterNames a set of parameter names.
     * @return a list of the corresponding parameters.
     */
    @NotNull
    private List<ParameterElement> collectRequestParametersWithNames(Operation operation, Set<ParameterName> parameterNames) {

        // At the moment, we only support leaves
        return operation.getAllRequestParameters().stream()
                .filter(p -> p instanceof ParameterLeaf && parameterNames.contains(p.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Filters a list of parameters by only returning the parameter leaves with a value.
     * @param parameters the list of parameters to filter.
     * @return the filtered list of parameters.
     */
    @NotNull
    private List<ParameterElement> filterBySetParameters(List<ParameterElement> parameters) {
        return parameters.stream()
                .filter(p -> p instanceof ParameterLeaf && ((ParameterLeaf) p).getConcreteValue() != null).
                collect(Collectors.toList());
    }
}
