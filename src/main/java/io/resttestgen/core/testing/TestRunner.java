package io.resttestgen.core.testing;

import io.resttestgen.boot.AuthenticationInfo;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.HttpStatusCode;
import io.resttestgen.core.helper.RequestManager;
import io.resttestgen.core.testing.coverage.CoverageManager;
import io.resttestgen.implementation.interactionprocessor.RequestDictionaryInteractionProcessor;
import io.resttestgen.implementation.interactionprocessor.ResponseDictionaryInteractionProcessor;
import io.resttestgen.implementation.interactionprocessor.GraphInteractionProcessor;
import io.resttestgen.implementation.interactionprocessor.JsonParserInteractionProcessor;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Executes a test sequence by issuing all the requests of the test interactions composing the test sequence.
 */
public class TestRunner {


    public static final TestSequence globalTestSequenceForDebug = new TestSequence();

    private static final Logger logger = LogManager.getLogger(TestRunner.class);

    private static TestRunner instance = null;
    private final OkHttpClient client;
    private final List<InteractionProcessor> interactionProcessors = new LinkedList<>();
    private final Set<HttpStatusCode> invalidStatusCodes = new HashSet<>();
    private static final int MAX_ATTEMPTS = 10;
    private AuthenticationInfo authenticationInfo = Environment.getInstance().getApiUnderTest().getDefaultAuthenticationInfo();
    private final CoverageManager coverage = new CoverageManager();

    /**
     * Constructor in which response processors are initialized and invalid status codes are defined. An invalid status
     * code is a status code that suggests a replay of the test interaction is required. Method is private to prevent
     * external initializations.
     */
    private TestRunner() {
        OkHttpClient client1;
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            X509TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            };
            sslContext.init(null, new TrustManager[]{TRUST_ALL_CERTS}, new java.security.SecureRandom());

            client1 = new OkHttpClient.Builder()
                    .hostnameVerifier((hostname, session) -> true)
                    .sslSocketFactory(sslContext.getSocketFactory(), TRUST_ALL_CERTS).build();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            client1 = new OkHttpClient();
            logger.warn("Could not instantiate OkHttp client to accept self-signed certificates. Using default client.");
        }
        client = client1;
        addInteractionProcessor(new JsonParserInteractionProcessor());
        addInteractionProcessor(new RequestDictionaryInteractionProcessor());
        addInteractionProcessor(new ResponseDictionaryInteractionProcessor());
        addInteractionProcessor(new GraphInteractionProcessor());
        addInvalidStatusCode(new HttpStatusCode(429));
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
        testSequence.forEach(this::tryTestInteractionExecution);
    }

    /**
     * Try to execute a test interaction with the API. Execution is retried in the case the server returned an invalid
     * status code (e.g., 429 too many requests). Execution is retried for a maximum number of attempts. Re-executions
     * are delayed according to the "Retry-After" header of the response, or 10 seconds.
     * @param testInteraction the test interaction to execute.
     */
    @SuppressWarnings("BusyWait")
    private void tryTestInteractionExecution(TestInteraction testInteraction) {

        int attempts = 0;
        long retryAfter = 0;
        HttpStatusCode obtainedStatusCode = null;

        while (attempts < MAX_ATTEMPTS && (attempts == 0 || invalidStatusCodes.contains(obtainedStatusCode))) {

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

            // Execute test interaction
            executeTestInteraction(testInteraction);

            // Only if the interaction is executed successfully and status code 429 is set as invalid status code
            if (testInteraction.getTestStatus() == TestStatus.EXECUTED &&
                    invalidStatusCodes.contains(new HttpStatusCode(429))) {

                // Check for status code 429 and set retryAfter accordingly
                obtainedStatusCode = testInteraction.getResponseStatusCode();
                if (obtainedStatusCode.equals(new HttpStatusCode(429))) {
                    Pattern pattern = Pattern.compile("Retry-After: (\\d+)\\n");
                    Matcher matcher = pattern.matcher(testInteraction.getResponseHeaders());
                    while (matcher.find()) {
                        retryAfter = Integer.parseInt(matcher.group(1));
                    }
                    logger.warn("Status code 429 detected. The request will be replayed in {} seconds. (Attempt {}/" + MAX_ATTEMPTS + ")", retryAfter, attempts + 1);
                }
            } else {

                // If the execution could not be completed because other reasons (e.g., timeout), retry after 2 seconds
                retryAfter = 2;
            }

            attempts++;

            if (attempts == MAX_ATTEMPTS) {
                logger.warn("Execution of test interaction aborted after " + MAX_ATTEMPTS + " attempts.");
            }
        }

        // Process response if the interaction could be executed correctly
        processInteraction(testInteraction);
    }

    /**
     * Executes a test interaction and fills the attributes with information about the performed request and the
     * received response.
     * @param testInteraction the test interaction to execute.
     */
    private void executeTestInteraction(TestInteraction testInteraction) {

        // Build request with RequestManager
        RequestManager requestManager = new RequestManager(testInteraction.getFuzzedOperation());
        requestManager.setAuthenticationInfo(authenticationInfo);
        Request request = requestManager.buildRequest();

        // Update test interaction with request info
        String requestBody = null;
        try {
            if (request.body() != null) {
                final Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                requestBody = buffer.readUtf8();
            }
        } catch (IOException ignored) {}

        testInteraction.setRequestInfo(HttpMethod.getMethod(request.method()), request.url().toString(),
                request.headers().toString(), requestBody);

        // Update test interaction with response info
        Call call = this.client.newCall(request);
        try {
            Response response = call.execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            testInteraction.setResponseInfo(response.protocol().toString(), new HttpStatusCode(response.code()),
                    response.headers().toString(), responseBody,
                    new Timestamp(response.sentRequestAtMillis()),
                    new Timestamp(response.receivedResponseAtMillis()));
            testInteraction.setTestStatus(TestStatus.EXECUTED);
            coverage.updateCoverage(testInteraction);
            logger.info("Executed test interaction: {} [{}]", testInteraction.getFuzzedOperation(), testInteraction.getResponseStatusCode());
        } catch (IOException e) {
            logger.warn("Request execution failed: connectivity problem or timeout.");
            call.cancel();
            testInteraction.setTestStatus(TestStatus.ERROR);
        }

        // Append executed interaction to global test sequence for debug
        globalTestSequenceForDebug.append(testInteraction);
    }

    /**
     * Process interaction with interaction processors, only with compatible interaction processors.
     * @param testInteraction the test interaction to process.
     */
    private void processInteraction(TestInteraction testInteraction) {
        interactionProcessors.stream()
                .filter(ip -> ip.canProcess(testInteraction))
                .forEachOrdered(ip -> ip.process(testInteraction));
    }

    public void addInteractionProcessor(InteractionProcessor interactionProcessor) {
        interactionProcessors.add(interactionProcessor);
    }

    public void removeInteractionProcessor(InteractionProcessor interactionProcessor) {
        interactionProcessors.remove(interactionProcessor);
    }

    public List<InteractionProcessor> getInteractionProcessors() {
        return Collections.unmodifiableList(interactionProcessors);
    }

    public List<InteractionProcessor> getInteractionProcessorsByType(Class<? extends InteractionProcessor> responseProcessorClass) {
        return interactionProcessors.stream().filter(responseProcessorClass::isInstance).collect(Collectors.toList());
    }

    public void addInvalidStatusCode(HttpStatusCode statusCode) {
        invalidStatusCodes.add(statusCode);
    }

    public void removeInvalidStatusCode(HttpStatusCode statusCode) {
        invalidStatusCodes.remove(statusCode);
    }

    public void setAuthenticationInfo(AuthenticationInfo authenticationInfo) {
        this.authenticationInfo = authenticationInfo;
    }

    public CoverageManager getCoverage(){
        return this.coverage;
    }
}
