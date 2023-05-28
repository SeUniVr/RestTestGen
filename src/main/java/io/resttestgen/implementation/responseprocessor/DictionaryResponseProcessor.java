package io.resttestgen.implementation.responseprocessor;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.NullParameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.dictionary.DictionaryEntry;
import io.resttestgen.core.testing.ResponseProcessor;
import io.resttestgen.core.testing.TestInteraction;

import java.util.Optional;

import static io.resttestgen.core.datatype.parameter.ParameterUtils.getLeaves;

public class DictionaryResponseProcessor extends ResponseProcessor {

    private static final Dictionary globalDictionary = Environment.getInstance().getGlobalDictionary();
    private Optional<Dictionary> localDictionary = Optional.empty();

    @Override
    public void process(TestInteraction testInteraction) {

        StructuredParameter responseBody = testInteraction.getFuzzedOperation().getResponseBody();

        // If the parsed response body is null, try to parse it
        if (responseBody == null) {
            JsonParserResponseProcessor jsonParserResponseProcessor = new JsonParserResponseProcessor();
            jsonParserResponseProcessor.process(testInteraction);
            responseBody = testInteraction.getFuzzedOperation().getResponseBody();
        }

        // If the response body is still null, terminate the processing of the response
        if (responseBody == null) {
            return;
        }

        // Iterate on leaves to store them into the dictionary
        for (LeafParameter leaf : getLeaves(responseBody)) {
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
