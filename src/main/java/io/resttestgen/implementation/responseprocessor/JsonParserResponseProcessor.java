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
 * WORK IN PROGRESS: parses JSON response bodies to the internal parameter structure
 */
public class JsonParserResponseProcessor extends ResponseProcessor {

    private static final Logger logger = LogManager.getLogger(JsonParserResponseProcessor.class);

    @Override
    public void process(TestInteraction testInteraction) {

        // Terminate processing if the response body is not in JSON format
        if (!testInteraction.getResponseHeaders().toLowerCase().contains("content-type: application/json")) {
            return;
        }

        // Try to parse the body with GSON
        Gson gson = new Gson();
        try {
            JsonElement parsedJSON = gson.fromJson(testInteraction.getResponseBody(), JsonElement.class);
            StructuredParameter responseBody = (StructuredParameter)
                    ParameterFactory.getParameter(parsedJSON, null);
            testInteraction.getFuzzedOperation().setResponseBody(responseBody);
        } catch (JsonSyntaxException | ClassCastException e) {
            logger.warn("GSON could not parse the response body of this interaction.");
        }
    }
}
