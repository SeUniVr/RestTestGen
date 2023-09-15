package io.resttestgen.implementation.responseprocessor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import io.resttestgen.core.datatype.parameter.ParameterFactory;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.testing.ResponseProcessor;
import io.resttestgen.core.testing.TestInteraction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses JSON response bodies to the internal parameter structure.
 */
public class JsonParserResponseProcessor extends ResponseProcessor {

    private static final Logger logger = LogManager.getLogger(JsonParserResponseProcessor.class);
    private static final Gson gson = new Gson();

    @Override
    public void process(TestInteraction testInteraction) {

        // Terminate processing if the response body is null, shorter than 2 chars, or longer than 1 million chars
        if (testInteraction.getResponseBody() == null || testInteraction.getResponseBody().length() < 2 ||
                testInteraction.getResponseBody().length() > 1000000) {
            return;
        }

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
