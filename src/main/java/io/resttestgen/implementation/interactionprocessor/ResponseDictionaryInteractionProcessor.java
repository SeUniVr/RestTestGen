package io.resttestgen.implementation.interactionprocessor;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NullParameter;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.dictionary.DictionaryEntry;
import io.resttestgen.core.testing.InteractionProcessor;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestStatus;

import java.util.Optional;

import static io.resttestgen.core.datatype.parameter.ParameterUtils.getLeaves;

public class ResponseDictionaryInteractionProcessor extends InteractionProcessor {

    private static final Dictionary globalResponseDictionary = Environment.getInstance().getGlobalResponseDictionary();
    private Optional<Dictionary> localResponseDictionary = Optional.empty();

    /**
     * Only processes executes interactions with a successful status code.
     * @param testInteraction the subject test interaction.
     * @return true if an interaction is executed with successful status code.
     */
    @Override
    public boolean canProcess(TestInteraction testInteraction) {
        return testInteraction.getTestStatus() == TestStatus.EXECUTED && testInteraction.getResponseStatusCode().isSuccessful() &&
                testInteraction.getFuzzedOperation().getResponseBody() != null;
    }

    /**
     * TODO: consider removing values that are removed by DELETE requests from dictionaries. Note: don't remove IDs in higher hierarchy
     * @param testInteraction the target interaction.
     */
    @Override
    public void process(TestInteraction testInteraction) {

        // Iterate on leaves to store them into the response dictionary
        for (LeafParameter leaf : getLeaves(testInteraction.getFuzzedOperation().getResponseBody())) {
            if (!(leaf instanceof NullParameter)) {
                DictionaryEntry entry = new DictionaryEntry(leaf);
                globalResponseDictionary.addEntry(entry);
                localResponseDictionary.ifPresent(dictionary -> dictionary.addEntry(entry));
            }
        }
    }

    public Optional<Dictionary> getLocalResponseDictionary() {
        return localResponseDictionary;
    }

    public void setLocalResponseDictionary(Dictionary localResponseDictionary) {
        this.localResponseDictionary = Optional.of(localResponseDictionary);
    }
}
