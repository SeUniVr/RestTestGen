package io.resttestgen.implementation.responseprocessor;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.core.datatype.parameter.StructuredParameterElement;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.ResponseProcessor;
import io.resttestgen.core.testing.TestInteraction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * WORK IN PROGRESS: parses JSON response bodies to the internal parameter structure
 */
public class JSONParserResponseProcessor implements ResponseProcessor {

    private static final Logger logger = LogManager.getLogger(JSONParserResponseProcessor.class);

    @Override
    public void process(TestInteraction testInteraction) {

        // Terminate processing if the response body is not in JSON format
        // FIXME: perhaps other content types indicate a JSON body
        if (!testInteraction.getResponseHeaders().toLowerCase().contains("content-type: application/json")) {
            return;
        }

        // Try to parse the body with GSON
        Gson gson = new Gson();
        Object parsedJSON;
        try {
            parsedJSON = gson.fromJson(testInteraction.getResponseBody(), Object.class);
        } catch (JsonSyntaxException e) {
            logger.warn("GSON could not parse the response body of this interaction.");
            e.printStackTrace();
            return;
        }

        // Get operation and response body schema for the obtained status code
        Operation operation = testInteraction.getOperation();
        StructuredParameterElement outputParameters = operation.getOutputParameters()
                .get(testInteraction.getResponseStatusCode().toString());

        // If there is no schema defined for the obtained status code, create it
        /*if (outputParameters == null) {
            StructuredParameterElement newOutputParameter = null;
            if (parsedJSON instanceof ArrayList) {
                newOutputParameter = new ParameterArray(null, null, operation, null);
                operation.getOutputParameters().put(testInteraction.getResponseStatusCode().toString(), newOutputParameter);
            } else if (parsedJSON instanceof LinkedTreeMap) {
                newOutputParameter = new ParameterObject(null, null, operation, null);

            }
            if (newOutputParameter != null) {
                operation.getOutputParameters().put(testInteraction.getResponseStatusCode().toString(), newOutputParameter);
            }
        }*/

        // The main JSON element is an array
        if (parsedJSON instanceof ArrayList) {

        }

        // The main JSON element is an object
        else if (parsedJSON instanceof LinkedTreeMap) {
        }
    }
}
