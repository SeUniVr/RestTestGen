package io.resttestgen.implementation.interactionprocessor;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.helper.Experience;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.InteractionProcessor;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExperienceUpdaterInteractionProcessor extends InteractionProcessor {

    private final static Logger logger = LogManager.getLogger(ExperienceUpdaterInteractionProcessor.class);

    Experience experience = Environment.getInstance().getExperience();

    @Override
    public boolean canProcess(TestInteraction testInteraction) {
        // Process only interactions that got a successful status code
        return testInteraction.getTestStatus() == TestStatus.EXECUTED && testInteraction.getResponseStatusCode().isSuccessful();
    }

    @Override
    public void process(TestInteraction testInteraction) {

        Operation fuzzedOperation = testInteraction.getFuzzedOperation();

        // Process leaves
        for (LeafParameter leafParameter : fuzzedOperation.getLeaves()) {

            // Process values
            experience.addParameterValueObservation(leafParameter, leafParameter.getValueSource());

            // Process presence
            experience.addParameterPresenceObservation(leafParameter, leafParameter.getConcreteValue() != null);
        }

        // Process arrays
        for (ArrayParameter arrayParameter : fuzzedOperation.getArrays()) {
            experience.addArraySizeObservation(arrayParameter, arrayParameter.size());
        }

        logger.debug("Experience updated.");

        //experience.printStats();
    }
}
