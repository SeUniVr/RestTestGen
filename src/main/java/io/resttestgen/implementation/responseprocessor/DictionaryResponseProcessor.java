package io.resttestgen.implementation.responseprocessor;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.NullParameter;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.datatype.parameter.StructuredParameterElement;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.dictionary.DictionaryEntry;
import io.resttestgen.core.testing.ResponseProcessor;
import io.resttestgen.core.testing.TestInteraction;

import java.util.Optional;

public class DictionaryResponseProcessor extends ResponseProcessor {

    private static final Dictionary globalDictionary = Environment.getInstance().getGlobalDictionary();
    private Optional<Dictionary> localDictionary = Optional.empty();

    @Override
    public void process(TestInteraction testInteraction) {

        StructuredParameterElement responseBody = testInteraction.getOperation().getResponseBody();

        // If the parsed response body is null, try to parse it
        if (responseBody == null) {
            JsonParserResponseProcessor jsonParserResponseProcessor = new JsonParserResponseProcessor();
            jsonParserResponseProcessor.process(testInteraction);
            responseBody = testInteraction.getOperation().getResponseBody();
        }

        // If the response body is still null, terminate the processing of the response
        if (responseBody == null) {
            return;
        }

        // Iterate on leaves to store them into the dictionary
        for (ParameterLeaf leaf : responseBody.getLeaves()) {
            if (!(leaf instanceof NullParameter)) {
                DictionaryEntry entry = new DictionaryEntry(leaf);
                globalDictionary.addEntry(entry);
                localDictionary.ifPresent(dictionary -> dictionary.addEntry(entry));
            }
        }
    }

    public Optional<Dictionary> getLocalDictionary() {
        return localDictionary;
    }

    public void setLocalDictionary(Dictionary localDictionary) {
        this.localDictionary = Optional.of(localDictionary);
    }
}
