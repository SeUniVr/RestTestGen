package io.resttestgen.implementation.interactionprocessor;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.dictionary.DictionaryEntry;
import io.resttestgen.core.testing.InteractionProcessor;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestStatus;

import java.util.Optional;

public class RequestDictionaryInteractionProcessor extends InteractionProcessor {

    private static final Dictionary globalRequestDictionary = Environment.getInstance().getGlobalRequestDictionary();
    private Optional<Dictionary> localRequestDictionary = Optional.empty();

    /**
     * Only processes executes interactions with a successful status code.
     * @param testInteraction the subject test interaction.
     * @return true if an interaction is executed with successful status code.
     */
    @Override
    public boolean canProcess(TestInteraction testInteraction) {
        return testInteraction.getTestStatus() == TestStatus.EXECUTED && testInteraction.getResponseStatusCode().isSuccessful();
    }

    @Override
    public void process(TestInteraction testInteraction) {
        testInteraction.getFuzzedOperation().getAllRequestParameters().stream()
                .filter(ParameterUtils::isLeaf)
                .filter(Parameter::hasValue)
                .forEach(p -> {
                    DictionaryEntry entry = new DictionaryEntry((LeafParameter) p);
                    globalRequestDictionary.addEntry(entry);
                    localRequestDictionary.ifPresent(dictionary -> dictionary.addEntry(entry));
                });
    }

    public Optional<Dictionary> getLocalRequestDictionary() {
        return localRequestDictionary;
    }

    public void setLocalRequestDictionary(Dictionary localRequestDictionary) {
        this.localRequestDictionary = Optional.of(localRequestDictionary);
    }
}
