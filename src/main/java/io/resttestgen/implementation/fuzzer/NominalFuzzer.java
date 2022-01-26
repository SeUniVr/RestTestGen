package io.resttestgen.implementation.fuzzer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.ParameterArray;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.helper.ResponseAnalyzer;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Fuzzer;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.implementation.oracle.StatusCodeOracle;
import io.resttestgen.implementation.writer.ReportWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The Nominal Fuzzer generates nominal test sequences
 */
public class NominalFuzzer extends Fuzzer {

    private static final Logger logger = LogManager.getLogger(NominalFuzzer.class);

    private Environment environment;
    private Operation operation;
    private Dictionary dictionary;
    
    public NominalFuzzer(Environment environment, Operation operation) {
        this.environment = environment;
        this.operation = operation;
        this.dictionary = environment.dictionary;
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
            int n = environment.random.nextLength(0, 5); // FIXME: with actual limits from the array, or move this logic to the array class
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
            assignValue(leaf, environment.random, dictionary);
        }

        // Create a test interaction from the request
        TestInteraction testInteraction = new TestInteraction(editableOperation);

        // Encapsulate test interaction into test sequence
        TestSequence testSequence = new TestSequence(this, testInteraction);
        SimpleDateFormat dformat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        testSequence.setName(editableOperation.getOperationId() + "_" + dformat.format(testSequence.getGeneratedAt()));

        // Run test sequence
        TestRunner testRunner = TestRunner.getInstance();
        testRunner.setEnvironment(environment);
        testRunner.run(testSequence);

        // Evaluate sequence with oracles
        StatusCodeOracle statusCodeOracle = new StatusCodeOracle();
        statusCodeOracle.assertTestSequence(testSequence);

        // Write report to file
        ReportWriter reportWriter = new ReportWriter(environment, testSequence);
        reportWriter.write();


        ResponseAnalyzer responseAnalyzer = new ResponseAnalyzer(environment);
        responseAnalyzer.setDictionary(dictionary); // FIXME: move to setDictionary method
        responseAnalyzer.analyzeResponse(editableOperation, testInteraction.getResponseStatusCode(),
                testInteraction.getResponseBody());

        // Create and return test sequence containing the test interaction
        return testSequence;
    }


    /**
     * Assign value to a leaf.
     * Required parameters are always assigned with a value, non-required parameters have a 50% probability to have a
     * value assigned.
     * @param leaf the leaf to which assign the value
     */
    private void assignValue(ParameterLeaf leaf, ExtendedRandom random, Dictionary dictionary) {

        // Set value with 70% probability, if parameter is not mandatory. Null parameters will be removed
        if (leaf.isRequired() || random.nextInt(100) < 70) {

            List<String> sources = new LinkedList<>();
            sources.add("random");
            if (leaf.getDefaultValue() != null) {
                sources.add("default");
            }
            if (leaf.getEnumValues().size() > 0) {
                leaf.getEnumValues().forEach(v -> sources.add("enum"));
            }
            if (leaf.getExamples().size() > 0) {
                leaf.getEnumValues().forEach(v -> sources.add("examples"));
            }
            if (leaf.countValuesInNormalizedDictionary(dictionary) > 0) {
                for (int i = 0; i < leaf.countValuesInNormalizedDictionary(dictionary); i++) {
                    sources.add("normalizedDictionary");
                }
            }
            if (leaf.countValuesInDictionary(dictionary) > 0) {
                for (int i = 0; i < leaf.countValuesInDictionary(dictionary); i++) {
                    sources.add("dictionary");
                }
            }

            int source_index = random.nextInt(sources.size());
            switch (sources.get(source_index)) {
                case "random": {
                    Object val = leaf.generateCompliantValue();
                    leaf.setValue(val);
                    break;
                }
                case "default": {
                    Object val = leaf.getDefaultValue();
                    leaf.setValue(val);
                    break;
                }
                case "enum": {
                    Object val = new ArrayList<>(leaf.getEnumValues()).get(random.nextInt(leaf.getEnumValues().size()));
                    leaf.setValue(val);
                    break;
                }
                case "examples": {
                    Object val = new ArrayList<>(leaf.getExamples()).get(random.nextInt(leaf.getExamples().size()));
                    leaf.setValue(val);
                    break;
                }
                case "normalizedDictionary": {
                    Object val = leaf.getValueFromNormalizedDictionary(dictionary);
                    leaf.setValue(val);
                    break;
                }
                case "dictionary": {
                    Object val = leaf.getValueFromDictionary(dictionary);
                    leaf.setValue(val);
                    break;
                }
            }

        }
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }
}
