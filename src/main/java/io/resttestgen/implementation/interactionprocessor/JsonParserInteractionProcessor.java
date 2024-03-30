package io.resttestgen.implementation.interactionprocessor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import io.resttestgen.core.datatype.parameter.ParameterFactory;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.testing.InteractionProcessor;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses JSON response bodies to the internal parameter structure.
 */
public class JsonParserInteractionProcessor extends InteractionProcessor {

    private static final Logger logger = LogManager.getLogger(JsonParserInteractionProcessor.class);
    private static final Gson gson = new Gson();

    @Override
    public boolean canProcess(TestInteraction testInteraction) {
        return testInteraction.getTestStatus() == TestStatus.EXECUTED && testInteraction.getResponseBody() != null &&
                testInteraction.getResponseBody().length() > 1 && testInteraction.getResponseBody().length() < 1000000;
    }

    @Override
    public void process(TestInteraction testInteraction) {

        // Try to parse the body with GSON
        try {
            JsonElement parsedJSON = gson.fromJson(testInteraction.getResponseBody(), JsonElement.class);
            StructuredParameter responseBody = (StructuredParameter)
                    ParameterFactory.getParameter(parsedJSON, null);
            testInteraction.getFuzzedOperation().setResponseBody(responseBody);
        } catch (JsonSyntaxException | ClassCastException e) {

            // Warn if content type is JSON-related but GSON can not parse it.
            if (testInteraction.getResponseHeaders().toLowerCase().contains("content-type: application/json")) {
                logger.warn("GSON could not parse the response body of this interaction, despite being JSON according to content type.");
            }
        }
    }
}
