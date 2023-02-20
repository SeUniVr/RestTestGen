package io.resttestgen.implementation.fuzzer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.ParameterArray;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Fuzzer;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.implementation.helper.InterParameterDependenciesHelper;
import io.resttestgen.implementation.parametervalueprovider.multi.EnumAndExamplePriorityParameterValueProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The Nominal Fuzzer generates nominal test sequences
 */
public class NominalFuzzer extends Fuzzer {

    private static final Logger logger = LogManager.getLogger(NominalFuzzer.class);
    private static final ExtendedRandom random = Environment.getInstance().getRandom();

    private final Operation operation;
    private Operation editableOperation;
    private ParameterValueProvider parameterValueProvider = new EnumAndExamplePriorityParameterValueProvider();
    private boolean strict = false;

    public final int PROBABILITY_TO_KEEP_A_NON_REQUIRED_LEAF = 10;
    
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

        editableOperation = operation.deepClone();
        InterParameterDependenciesHelper idpHelper = new InterParameterDependenciesHelper(editableOperation, parameterValueProvider);

        resolveCombinedSchemas();
        populateArrays();
        idpHelper.extractExampleValuesFromRequiresIpds();
        setValueToLeaves();
        idpHelper.applyInterParameterDependencies();

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

    public void setValue(ParameterElement parameter) {
        if (parameter instanceof ParameterLeaf) {
            ((ParameterLeaf) parameter).setValue(parameterValueProvider.provideValueFor((ParameterLeaf) parameter));
        } else if (parameter.isArrayOfLeaves()) {
            ParameterArray parameterArray = (ParameterArray) parameter;
            parameterArray.setValuesFromCommaSeparatedString(parameterValueProvider.provideValueFor((ParameterLeaf) parameterArray.getReferenceElement()).toString());
        }
    }

    public void removeValue(ParameterElement parameter) {
        if (parameter instanceof ParameterLeaf) {
            ((ParameterLeaf) parameter).removeValue();
        } else if (parameter.isArrayOfLeaves()) {
            ((ParameterArray) parameter).getElements().clear();
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
        Collection<ParameterArray> arrays = editableOperation.getArrays();
        LinkedList<ParameterArray> queue = new LinkedList<>(arrays);
        while (!queue.isEmpty()) {
            ParameterArray array = queue.getFirst();
            int n = random.nextShortLength(array.getMinItems(), array.getMaxItems());

            // If not required, remove array with a 0.7 probability
            if (!array.isRequired() && random.nextInt(10) < 8) {
                n = 0;
            }

            for (int i = 0; i < n; i++) {
                ParameterElement referenceElementCopy = array.getReferenceElement().deepClone();
                array.addElement(referenceElementCopy);
                queue.addAll(referenceElementCopy.getArrays());
            }
            queue.remove(array);
        }
    }

    /**
     * Set value to the leaves in the operation. For non required leaves, the values is assigned with the probability
     * defined in the PROBABILITY_TO_KEEP_A_NON_REQUIRED_LEAF variable.
     */
    public void setValueToLeaves() {
        // Assign values to leaves and remove random non-mandatory leaves
        Collection<ParameterLeaf> leaves = editableOperation.getLeaves();
        for (ParameterLeaf leaf : leaves) {

            // If parameter is not mandatory or if it is not part of an array, set value with 25% probability
            // Null parameters will be removed by the request manager
            if (leaf.isRequired() || random.nextInt(100) < PROBABILITY_TO_KEEP_A_NON_REQUIRED_LEAF ||
                    (leaf.getParent() != null && leaf.getParent() instanceof ParameterArray)) {
                leaf.setValue(parameterValueProvider.provideValueFor(leaf));
            }
        }
    }
}
