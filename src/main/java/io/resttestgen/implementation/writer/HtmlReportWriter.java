package io.resttestgen.implementation.writer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.boot.Configuration;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.CoverageCollection;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.TestStatus;
import io.resttestgen.core.testing.coverage.Coverage;
import io.resttestgen.core.testing.coverage.CoverageManager;
import io.resttestgen.implementation.coveragemetric.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates HTML reports with test sequence data and coverage information.
 * Handles file operations, template generation, and JSON data injection.
 */
public class HtmlReportWriter {
    private static final Logger logger = LogManager.getLogger(HtmlReportWriter.class);

    // Path constants for templates and resources
    private static final String REPORT_RESOURCES_TEMPLATE_FOLDER = "report-resources/";
    private static final String REPORT_TEMPLATE = "report-template.html";
    private static final String REPORT_OUTPUT_FILENAME = "report.html";
    private static final String CSS_SUBFOLDER = "css/";
    private static final String JS_SUBFOLDER = "js/";
    private static final String IMG_SUBFOLDER = "img/";
    private static final String JS_CONSTANTS_FILENAME = "constants.js";

    // JS constants placeholders
    private static final String API_NAME_JS_PLACEHOLDER = "{{API-NAME}}";
    private static final String SESSION_ID_JS_PLACEHOLDER = "{{TESTING-SESSION-ID}}";
    private static final String TIMESTAMP_JS_PLACEHOLDER = "{{TIMESTAMP}}";
    private static final String TEST_SEQUENCE_JSON_DATA_PLACEHOLDER = "//{{TEST-SEQUENCE-JSON-DATA}}";

    private final Configuration configuration;
    private CoverageCollection coverageCollection;
    private final Gson gson;
    private int globalIncrementalId;
    private Path jsConstantsOutputPath;

    /**
     * Creates a new HtmlReportWriter with dependency injection for configuration.
     *
     * @param configuration The application configuration
     * @throws IllegalArgumentException If configuration is null
     */
    public HtmlReportWriter(Configuration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        this.coverageCollection = new CoverageCollection();
        this.gson = new Gson();
        this.globalIncrementalId = 0;
    }

    /**
     * Generates a unique sequence ID for test sequences.
     *
     * @return A unique sequence ID
     */
    public int getGlobalIncrementalId() {
        return this.globalIncrementalId++;
    }

    /**
     * Populates coverage collection from coverage manager data.
     *
     * @param coverageManager The coverage manager containing coverage data
     * @return The populated coverage collection
     * @throws IllegalArgumentException If coverage manager is null
     */
    public CoverageCollection populateCoverageCollection(CoverageManager coverageManager) {
        Objects.requireNonNull(coverageManager, "Coverage manager cannot be null");

        List<Coverage> coverageMetrics = coverageManager.getCoverageMetrics();

        // Extract single coverages from list
        PathCoverage pathCoverage = null;
        OperationCoverage operationCoverage = null;
        StatusCodeCoverage statusCodeCoverage = null;
        ParameterCoverage parameterCoverage = null;
        ParameterValueCoverage parameterValueCoverage = null;

        for (Coverage coverage : coverageMetrics) {
            if (coverage instanceof PathCoverage) {
                pathCoverage = (PathCoverage) coverage;
            } else if (coverage instanceof OperationCoverage) {
                operationCoverage = (OperationCoverage) coverage;
            } else if (coverage instanceof StatusCodeCoverage) {
                statusCodeCoverage = (StatusCodeCoverage) coverage;
            } else if (coverage instanceof ParameterCoverage) {
                parameterCoverage = (ParameterCoverage) coverage;
            } else if (coverage instanceof ParameterValueCoverage) {
                parameterValueCoverage = (ParameterValueCoverage) coverage;
            }
        }

        this.coverageCollection = new CoverageCollection(
                pathCoverage != null ? pathCoverage.getDocumentedPaths() : new HashSet<>(),
                pathCoverage != null ? pathCoverage.getTestedPaths() : new HashSet<>(),
                operationCoverage != null ? operationCoverage.getDocumentedOperations() : new HashSet<>(),
                operationCoverage != null ? operationCoverage.getTestedOperations() : new HashSet<>(),
                statusCodeCoverage != null ? statusCodeCoverage.getDocumentedStatusCodes() : new HashMap<>(),
                statusCodeCoverage != null ? statusCodeCoverage.getTestedStatusCodes() : new HashMap<>(),
                parameterCoverage != null ? parameterCoverage.getDocumentedParameters() : new HashMap<>(),
                parameterCoverage != null ? parameterCoverage.getTestedParameters() : new HashMap<>(),
                parameterValueCoverage != null ? parameterValueCoverage.getDocumentedValues() : new HashMap<>(),
                parameterValueCoverage != null ? parameterValueCoverage.getTestedValues() : new HashMap<>()
        );

        return this.coverageCollection;
    }

    /**
     * Fills the JS constants file with basic information like API name and timestamp.
     *
     * @throws IOException If an I/O error occurs
     */
    public void fillJSConstantsMetadata() throws IOException {
        // Get the path to the JS constants file
        jsConstantsOutputPath = getJSConstantsOutputFile();

        // Create parent directories if they don't exist
        Files.createDirectories(jsConstantsOutputPath.getParent());

        // Read the template content
        String jsContent = new String(Files.readAllBytes(Paths.get(getHtmlReportPathAsString(), JS_SUBFOLDER, JS_CONSTANTS_FILENAME)));

        // If jsConstantsOutputPath is null, we need to get the path
        if (jsConstantsOutputPath == null) {
            jsConstantsOutputPath = getJSConstantsOutputFile();
        }

        // Replace placeholders
        jsContent = jsContent.replace(API_NAME_JS_PLACEHOLDER, configuration.getApiUnderTest());
        jsContent = jsContent.replace(SESSION_ID_JS_PLACEHOLDER, configuration.getTestingSessionName());

        // Format and replace timestamp
        LocalDateTime timestamp = configuration.getTimestamp();
        jsContent = jsContent.replace(TIMESTAMP_JS_PLACEHOLDER,
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Write to file
        try (FileWriter jsWriter = new FileWriter(jsConstantsOutputPath.toFile())) {
            jsWriter.write(jsContent);
        }
    }

    /**
     * Creates all required report resource files.
     *
     * @throws IOException If an I/O error occurs
     */
    public void createReportResourcesFiles() throws IOException {
        // Create output directory if it doesn't exist
        Files.createDirectories(getOutputPath());





        // Copy HTML template as is (no modifications or replacements needed)
        // All dynamic data is injected into the JS constants file instead
        Path htmlOutputPath = getHTMLOutputFile();
        Files.createDirectories(htmlOutputPath.getParent());
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + REPORT_TEMPLATE, htmlOutputPath.toString());
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + CSS_SUBFOLDER + "main.css", getHtmlReportPathAsString() + CSS_SUBFOLDER + "main.css");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + CSS_SUBFOLDER + "base.css", getHtmlReportPathAsString() + CSS_SUBFOLDER + "base.css");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + CSS_SUBFOLDER + "components/header.css", getHtmlReportPathAsString() + CSS_SUBFOLDER + "components/header.css");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + CSS_SUBFOLDER + "components/list.css", getHtmlReportPathAsString() + CSS_SUBFOLDER + "components/list.css");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + CSS_SUBFOLDER + "components/parameter.css", getHtmlReportPathAsString() + CSS_SUBFOLDER + "components/parameter.css");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + CSS_SUBFOLDER + "components/popup.css", getHtmlReportPathAsString() + CSS_SUBFOLDER + "components/popup.css");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + CSS_SUBFOLDER + "components/toast.css", getHtmlReportPathAsString() + CSS_SUBFOLDER + "components/toast.css");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + IMG_SUBFOLDER + "rtg-logo-light.svg", getHtmlReportPathAsString() + IMG_SUBFOLDER + "rtg-logo-light.svg");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + JS_SUBFOLDER + "constants.js", getHtmlReportPathAsString() + JS_SUBFOLDER + "constants.js");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + JS_SUBFOLDER + "dom-utils.js", getHtmlReportPathAsString() + JS_SUBFOLDER + "dom-utils.js");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + JS_SUBFOLDER + "list.js", getHtmlReportPathAsString() + JS_SUBFOLDER + "list.js");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + JS_SUBFOLDER + "list-manager.js", getHtmlReportPathAsString() + JS_SUBFOLDER + "list-manager.js");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + JS_SUBFOLDER + "main.js", getHtmlReportPathAsString() + JS_SUBFOLDER + "main.js");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + JS_SUBFOLDER + "parameter-utils.js", getHtmlReportPathAsString() + JS_SUBFOLDER + "parameter-utils.js");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + JS_SUBFOLDER + "popup.js", getHtmlReportPathAsString() + JS_SUBFOLDER + "popup.js");
        copyResourceFile(REPORT_RESOURCES_TEMPLATE_FOLDER + JS_SUBFOLDER + "postman-utils.js", getHtmlReportPathAsString() + JS_SUBFOLDER + "postman-utils.js");

        // Fill the JS constants file with basic information
        fillJSConstantsMetadata();
    }

    /**
     * Gets the base output path for the report.
     *
     * @return Path object representing the output directory
     */
    private Path getOutputPath() {
        return Paths.get(configuration.getOutputPath(), configuration.getTestingSessionName());
    }

    private String getHtmlReportPathAsString() {
        return getOutputPath().resolve("html-report/") + "/report-resources/";
    }

    /**
     * Gets the report resources output folder path.
     *
     * @return Path object for the output folder
     */
    private Path getReportResourcesOutputFolder() {
        return getOutputPath().resolve("html-report/report-resources");
    }

    /**
     * Gets the HTML output file path.
     *
     * @return Path object for the HTML output file
     */
    private Path getHTMLOutputFile() {
        return getOutputPath().resolve("html-report/" + REPORT_OUTPUT_FILENAME);
    }


    /**
     * Gets the JS output folder path.
     *
     * @return Path object for the JS output folder
     */
    private Path getJSOutputFolder() {
        return getReportResourcesOutputFolder().resolve(JS_SUBFOLDER);
    }

    /**
     * Gets the JS constants output file path.
     *
     * @return Path object for the JS constants file
     */
    private Path getJSConstantsOutputFile() {
        return getJSOutputFolder().resolve(JS_CONSTANTS_FILENAME);
    }

    /**
     * Injects test sequence data into the JS constants file.
     *
     * @param testSequences List of test sequences to report
     * @throws IOException If an I/O error occurs
     * @throws IllegalArgumentException If testSequences is null
     */
    public void injectTestSequenceData(List<TestSequence> testSequences) throws IOException {
        Objects.requireNonNull(testSequences, "Test sequences cannot be null");

        // If jsConstantsOutputPath is null, we need to get the path
        if (jsConstantsOutputPath == null) {
            jsConstantsOutputPath = getJSConstantsOutputFile();
        }

        String content = new String(Files.readAllBytes(jsConstantsOutputPath));

        // Find placeholder position
        int placeholderPos = content.indexOf(TEST_SEQUENCE_JSON_DATA_PLACEHOLDER);
        if (placeholderPos == -1) {
            throw new IOException("Could not find test sequence placeholder in JS constants file");
        }

        // Use StringBuilder for efficient string manipulation
        StringBuilder newContent = new StringBuilder(content.substring(0, placeholderPos));

        // Process each test sequence
        for (TestSequence testSequence : testSequences) {
            processTestSequence(testSequence, newContent);
        }

        // Append the rest of the file
        newContent.append(content.substring(placeholderPos + TEST_SEQUENCE_JSON_DATA_PLACEHOLDER.length()));

        // Write to file
        try (FileWriter writer = new FileWriter(jsConstantsOutputPath.toFile())) {
            writer.write(newContent.toString());
        }
    }

    /**
     * Processes a single test sequence and adds its JSON representation to the StringBuilder.
     *
     * @param testSequence The test sequence to process
     * @param contentBuilder The StringBuilder to append to
     */
    private void processTestSequence(TestSequence testSequence, StringBuilder contentBuilder) {
        List<TestInteraction> interactions = testSequence.getTestInteractions();
        if (interactions == null || interactions.isEmpty()) {
            return;
        }

        for (int i = 0; i < interactions.size(); i++) {
            TestInteraction interaction = interactions.get(i);

            if (interaction.getTestStatus() == TestStatus.EXECUTED) {
                JsonObject data = createInteractionJsonData(testSequence, interaction, i);
                contentBuilder.append(gson.toJson(data)).append(",\n");
            }
        }
    }

    /**
     * Creates a JSON object for a test interaction.
     *
     * @param testSequence The parent test sequence
     * @param interaction The test interaction
     * @param interactionIndex The index of the interaction in the sequence
     * @return A JsonObject containing the interaction data
     */
    private JsonObject createInteractionJsonData(TestSequence testSequence, TestInteraction interaction, int interactionIndex) {
        JsonObject data = new JsonObject();

        // Add basic sequence information
        data.addProperty("id", this.getGlobalIncrementalId());
        data.addProperty("generator", testSequence.getGenerator());
        data.addProperty("generatedAt", testSequence.getGeneratedAt().toString());
        data.add("testResults", gson.toJsonTree(testSequence.getTestResults()));
        data.add("tags", gson.toJsonTree(testSequence.getTags()));
        data.addProperty("belongsToInteraction", testSequence.getName());
        data.addProperty("interactionIndex", interactionIndex);

        // Get operation
        Operation operation = interaction.getFuzzedOperation();
        if (operation == null) {
            operation = interaction.getReferenceOperation();
        }

        // Add request/response information
        populateRequestResponseData(data, interaction, operation);

        // Add parameters information
        if (operation != null) {
            data.add("requestParameters", createParametersJsonArray(operation));
        }

        return data;
    }

    /**
     * Populates request and response data in the JSON object.
     *
     * @param data The JSON object to populate
     * @param interaction The test interaction containing request/response data
     * @param operation The operation associated with the interaction
     */
    private void populateRequestResponseData(JsonObject data, TestInteraction interaction, Operation operation) {
        // Request URL
        data.addProperty("requestURL", interaction.getRequestURL());
        data.addProperty("requestURLStripped", interaction.getRequestURL().replaceAll("^(https?://[^/]+)(/.*)?$", "$2"));
        try {
            data.addProperty("requestURLReference", interaction.getReferenceOperation().getEndpoint());
        } catch (NullPointerException e) {
            // The reference url does not exist because of the applied mutation
            try {
                String pathAndQuery = new URI(interaction.getRequestURL()).getPath() +
                        (new URI(interaction.getRequestURL()).getQuery() != null ? "?" + new URI(interaction.getRequestURL()).getQuery() : "");
                data.addProperty("requestURLReference", pathAndQuery);
            } catch (Exception ignored) {}
            data.addProperty("testType", "-");
        }
        data.addProperty("requestURLIsDocumented",
                operation != null && coverageCollection.isPathDocumented(operation.getEndpoint()));
        data.addProperty("requestURLIsTested",
                operation != null && coverageCollection.isPathTested(operation.getEndpoint()));

        // Request method
        data.addProperty("requestMethod", interaction.getRequestMethod().toString());
        data.addProperty("requestMethodIsDocumented",
                operation != null && coverageCollection.isOperationDocumented(operation));
        data.addProperty("requestMethodIsTested",
                operation != null && coverageCollection.isOperationTested(operation));

        // Response status code
        data.addProperty("responseStatusCode", interaction.getResponseStatusCode().toString());
        data.addProperty("responseStatusCodeIsDocumented",
                operation != null && coverageCollection.isStatusCodeDocumented(operation, interaction.getResponseStatusCode()));
        data.addProperty("responseStatusCodeIsTested",
                operation != null && coverageCollection.isStatusCodeTested(operation, interaction.getResponseStatusCode()));

        // Request header
        data.addProperty("requestHeader", interaction.getRequestHeaders());

        // Request body
        data.addProperty("requestBody", interaction.getRequestBody());

        // Response header
        data.addProperty("responseHeader", interaction.getResponseHeaders());

        // Response body
        data.addProperty("responseBody", interaction.getResponseBody());

        // Request type (positive or negative test, i.e., valid or invalid input)
        if (!data.has("testType")) {
            boolean negativeTest = interaction.getFuzzedOperation().getAllRequestParameters().stream().flatMap(p -> p.getTags().stream()).anyMatch(t -> t.equals("mutated"));
            data.addProperty("testType", negativeTest ? "-" : "+");
        }
    }

    /**
     * Creates a JSON array of parameters for an operation.
     *
     * @param operation The operation containing parameters
     * @return A JsonArray containing parameter data
     */
    private JsonArray createParametersJsonArray(Operation operation) {
        JsonArray parameters = new JsonArray();

        for (Parameter parameter : operation.getAllRequestParameters()) {
            if (!(parameter instanceof LeafParameter)) {
                continue;
            }

            LeafParameter leafParam = (LeafParameter) parameter;
            if (leafParam.getType() != ParameterType.BOOLEAN && !leafParam.isEnum()) {
                continue;
            }

            JsonObject parameterData = createParameterJsonObject(operation, leafParam);
            parameters.add(parameterData);
        }

        return parameters;
    }

    /**
     * Creates a JSON object for a single parameter.
     *
     * @param operation The operation containing the parameter
     * @param parameter The parameter to create JSON for
     * @return A JsonObject containing parameter data
     */
    private JsonObject createParameterJsonObject(Operation operation, LeafParameter parameter) {
        ParameterElementWrapper parameterWrapper = new ParameterElementWrapper(parameter);
        Object concreteValue = parameter.getConcreteValue();

        // Parameter data
        JsonObject parameterData = new JsonObject();
        parameterData.addProperty("name", parameter.getName().toString());
        parameterData.addProperty("isDocumented",
                coverageCollection.isParameterDocumented(operation, parameterWrapper));
        parameterData.addProperty("isTested",
                coverageCollection.isParameterTested(operation, parameterWrapper));

        // Parameter value data
        JsonObject valueData = new JsonObject();
        valueData.addProperty("value", concreteValue != null ? concreteValue.toString() : "null");
        valueData.addProperty("isDocumented",
                coverageCollection.isParameterValueDocumented(operation, parameterWrapper, concreteValue));
        valueData.addProperty("isTested",
                coverageCollection.isParameterValueTested(operation, parameterWrapper, concreteValue));

        parameterData.add("parameterValue", valueData);
        return parameterData;
    }

    public static void copyResourceFile(String resourcePath, String targetPath) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader(); // Most reliable

        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found on classpath: " + resourcePath);
            }

            Path target = Paths.get(targetPath);
            Files.createDirectories(target.getParent());
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
