package io.resttestgen.core.testing;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HTTPMethod;
import io.resttestgen.core.datatype.HTTPStatusCode;
import io.resttestgen.core.helper.RequestManager;
import io.resttestgen.implementation.responseprocessor.DictionaryResponseProcessor;
import io.resttestgen.implementation.responseprocessor.GraphResponseProcessor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes a test sequence by issuing all the requests of the test interactions composing the test sequence.
 */
public class TestRunner {

    private static final Logger logger = LogManager.getLogger(TestRunner.class);

    private static TestRunner instance = null;
    private final OkHttpClient client = new OkHttpClient();
    private final List<ResponseProcessor> responseProcessors = new LinkedList<>();
    private final Set<HTTPStatusCode> invalidStatusCodes = new HashSet<>();
    private static final int MAX_ATTEMPTS = 10;
    private Environment environment;

    /**
     * Constructor in which response processors are initialized and invalid status codes are defined. An invalid status
     * code is a status code that suggests a replay of the test interaction is required. Method is private to prevent
     * external initializations.
     */
    private TestRunner() {
        addResponseProcessor(new DictionaryResponseProcessor());
        addResponseProcessor(new GraphResponseProcessor());
        addInvalidStatusCode(new HTTPStatusCode(429));
    }

    /**
     * Singleton instance management.
     * @return the TestRunner instance.
     */
    public static TestRunner getInstance() {
        if (instance == null) {
            instance = new TestRunner();
        }
        return instance;
    }

    /**
     * Runs a test sequence.
     * @param testSequence test sequence to run.
     */
    public void run(TestSequence testSequence) {
        testSequence.getTestInteractions().forEach(this::tryTestInteractionExecution);
    }

    /**
     * Try to execute a test interaction with the API. Execution is retried in the case the server returned an invalid
     * status code (eg. 429 too many requests). Execution is retried for a maximum number of attempts. Re-executions are
     * delayed according to the "Retry-After" header of the response, or 10 seconds.
     * FIXME: sleep in case of invalid status code
     * @param testInteraction the test interaction to execute.
     */
    @SuppressWarnings("BusyWait")
    private void tryTestInteractionExecution(TestInteraction testInteraction) {

        int attempts = 0;
        long retryAfter = 0;
        HTTPStatusCode obtainedStatusCode = new HTTPStatusCode(429);

        while (attempts < MAX_ATTEMPTS && invalidStatusCodes.contains(obtainedStatusCode)) {

            // Resets the information about the HTTP request and response (they could be filled with data from a
            // previous execution)
            testInteraction.reset();

            // Wait and increase waiting time for the next iteration
            if (retryAfter > 0) {
                try {
                    Thread.sleep(1000L * retryAfter);
                    retryAfter *= 2;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                retryAfter++;
            }

            try {
                executeTestInteraction(testInteraction);
                obtainedStatusCode = testInteraction.getResponseStatusCode();
                if (obtainedStatusCode.equals(new HTTPStatusCode(429))) {
                    Pattern pattern = Pattern.compile("Retry-After: ([0-9]+)\\n");
                    Matcher matcher = pattern.matcher(testInteraction.getResponseHeaders());
                    while (matcher.find()) {
                        retryAfter = Integer.parseInt(matcher.group(1));
                    }
                    logger.warn("Status code 429 detected. The request will be replayed in " + retryAfter +
                            " seconds. (Attempt " + (attempts + 1) + "/" + MAX_ATTEMPTS + ")");
                }
            } catch (IOException e) {
                logger.warn("Could not execute request.");
                e.printStackTrace();
            }

            attempts++;

            if (attempts == MAX_ATTEMPTS) {
                logger.warn("Execution aborted after " + MAX_ATTEMPTS + " attempts.");
            }
        }
    }

    /**
     * Executes a test interaction and fills the attributes with information about the performed request and the
     * received response.
     * @param testInteraction the test interaction to execute.
     * @throws IOException if the HTTP interaction fails.
     */
    private void executeTestInteraction(TestInteraction testInteraction) throws IOException {

        // Build request with RequestManager
        RequestManager requestManager = new RequestManager(environment, testInteraction.getOperation());
        Request request = requestManager.buildRequest();

        // Update test interaction with request info
        String requestBody = null;
        if (request.body() != null) {
            final Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            requestBody = buffer.readUtf8();
        }
        testInteraction.setRequestInfo(HTTPMethod.getMethod(request.method()), request.url().toString(),
                request.headers().toString(), requestBody);

        // Update test interaction with response info
        Response response = this.client.newCall(request).execute();
        String responseBody = null;
        if (response.body() != null) {
            responseBody = response.body().string();
        }
        testInteraction.setResponseInfo(response.protocol().toString(), new HTTPStatusCode(response.code()),
                response.headers().toString(), responseBody,
                new Timestamp(response.sentRequestAtMillis()),
                new Timestamp(response.receivedResponseAtMillis()));
    }

    /**
     * Process responses with response processor
     * @param response OkHttp response
     * @param responseBody response body
     */
    private void processResponse(Response response, String responseBody) {
        responseProcessors.forEach(responseProcessor -> responseProcessor.process(response, responseBody));
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void addResponseProcessor(ResponseProcessor responseProcessor) {
        responseProcessors.add(responseProcessor);
    }

    public void removeResponseProcessor(ResponseProcessor responseProcessor) {
        responseProcessors.remove(responseProcessor);
    }

    public void addInvalidStatusCode(HTTPStatusCode statusCode) {
        invalidStatusCodes.add(statusCode);
    }

    public void removeInvalidStatusCode(HTTPStatusCode statusCode) {
        invalidStatusCodes.remove(statusCode);
    }
}
