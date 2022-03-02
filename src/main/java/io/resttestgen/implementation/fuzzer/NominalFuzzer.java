package io.resttestgen.implementation.fuzzer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.ParameterArray;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.helper.ResponseAnalyzer;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Fuzzer;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.implementation.oracle.StatusCodeOracle;
import io.resttestgen.implementation.parametervalueprovider.multi.RandomProviderParameterValueProvider;
import io.resttestgen.implementation.writer.ReportWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The Nominal Fuzzer generates nominal test sequences
 */
public class NominalFuzzer extends Fuzzer {

    private static final Logger logger = LogManager.getLogger(NominalFuzzer.class);

    private final Operation operation;
    private ParameterValueProvider parameterValueProvider = new RandomProviderParameterValueProvider();
    private Dictionary localDictionary;
    
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
        Collection<ParameterArray> arrays = editableOperation.getArrays();
        LinkedList<ParameterArray> queue = new LinkedList<>(arrays);
        while (!queue.isEmpty()) {
            ParameterArray array = queue.getFirst();
            int n = Environment.getInstance().getRandom().nextLength(0, 5); // FIXME: with actual limits from the array, or move this logic to the array class
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

            // Set value with 70% probability, if parameter is not mandatory. Null parameters will be removed by the
            // request manager
            if (leaf.isRequired() || Environment.getInstance().getRandom().nextInt(100) < 70) {
                leaf.setValue(parameterValueProvider.provideValueFor(leaf));
            }
        }

        // Create a test interaction from the request
        TestInteraction testInteraction = new TestInteraction(editableOperation);

        // Encapsulate test interaction into test sequence
        TestSequence testSequence = new TestSequence(this, testInteraction);
        SimpleDateFormat dformat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        testSequence.setName(editableOperation.getOperationId() + "_" + dformat.format(testSequence.getGeneratedAt()));

        // Run test sequence
        TestRunner testRunner = TestRunner.getInstance();
        testRunner.run(testSequence);

        // Evaluate sequence with oracles
        StatusCodeOracle statusCodeOracle = new StatusCodeOracle();
        statusCodeOracle.assertTestSequence(testSequence);

        // Write report to file
        try {
            ReportWriter reportWriter = new ReportWriter(testSequence);
            reportWriter.write();
        } catch (IOException e) {
            logger.warn("Could not write report to file.");
        }

        ResponseAnalyzer responseAnalyzer = new ResponseAnalyzer();
        responseAnalyzer.analyzeResponse(editableOperation, testInteraction.getResponseStatusCode(),
                testInteraction.getResponseBody());

        // Create and return test sequence containing the test interaction
        return testSequence;
    }

    public void setParameterValueProvider(ParameterValueProvider parameterValueProvider) {
        this.parameterValueProvider = parameterValueProvider;
    }

    public void setLocalDictionary(Dictionary localDictionary) {
        this.localDictionary = localDictionary;
    }
}
