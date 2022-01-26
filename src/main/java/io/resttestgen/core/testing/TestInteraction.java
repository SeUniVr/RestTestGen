package io.resttestgen.core.testing;

import io.resttestgen.core.datatype.HTTPMethod;
import io.resttestgen.core.datatype.HTTPStatusCode;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;

/**
 * Represents a single HTTP test interaction (including a request and a response).
 */
public class TestInteraction {

    private static final Logger logger = LogManager.getLogger(TestInteraction.class);

    // Request fields
    private transient Operation operation;
    private HTTPMethod requestMethod;
    private String requestURL;
    private String requestHeaders;
    private String requestBody;
    private Timestamp requestSentAt;

    // Response fields
    private String responseProtocol;
    private HTTPStatusCode responseStatusCode;
    private String responseHeaders;
    private String responseBody;
    private Timestamp responseReceivedAt;

    // Other fields
    private Timestamp executionTime;
    private transient TestStatus testStatus = TestStatus.CREATED;


    public TestInteraction(Operation operation) {
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public void setResponseStatusCode(HTTPStatusCode statusCode) {
        this.responseStatusCode = statusCode;
    }

    public HTTPStatusCode getResponseStatusCode() {
        return responseStatusCode;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public TestStatus getTestStatus() {
        return testStatus;
    }

    public void setTestStatus(TestStatus testStatus) {
        this.testStatus = testStatus;
    }

    public void setRequestInfo(HTTPMethod httpMethod, String requestURL, String requestHeaders, String requestBody) {
        this.requestMethod = httpMethod;
        this.requestURL = requestURL;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
    }

    public void setResponseInfo(String responseProtocol, HTTPStatusCode responseStatusCode, String responseHeaders,
                                String responseBody, Timestamp requestSentAt, Timestamp responseReceivedAt) {
        this.responseProtocol = responseProtocol;
        this.responseStatusCode = responseStatusCode;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.requestSentAt = requestSentAt;
        this.responseReceivedAt = responseReceivedAt;
        this.testStatus = TestStatus.EXECUTED;
    }

    public void reset() {

        // Reset request info
        requestMethod = null;
        requestURL = null;
        requestHeaders = null;
        requestBody = null;
        requestSentAt = null;

        // Reset response info
        responseProtocol = null;
        responseStatusCode = null;
        responseHeaders = null;
        responseBody = null;
        responseReceivedAt = null;

        // Reset test status
        testStatus = TestStatus.CREATED;
    }

    public TestInteraction deepClone() {
        return new TestInteraction(operation.deepClone());
    }
}
