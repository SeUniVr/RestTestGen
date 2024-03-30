package io.resttestgen.core.testing;

public abstract class InteractionProcessor {

    /**
     * Check if the provided test interaction is processable by the implemented interaction processor. Some processors
     * might only accept executed interactions, some other only interaction that have been executed with a successful
     * status code, etc.
     * @param testInteraction the subject test interaction.
     * @return true if the provided test interaction is processable by this interaction processor.
     */
    public abstract boolean canProcess(TestInteraction testInteraction);

    /**
     * Implements the processing of the interaction.
     * @param testInteraction the target interaction.
     */
    public abstract void process(TestInteraction testInteraction);
}
