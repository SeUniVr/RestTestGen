package io.resttestgen.core;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Configuration;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.CannotParseOpenApiException;
import io.resttestgen.core.openapi.OpenApi;
import io.resttestgen.core.openapi.OpenApiParser;
import io.resttestgen.core.operationdependencygraph.OperationDependencyGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * A container class for all the test environment components of the RestTestGen Core, such as the parsed specification,
 * dictionaries, the Operation Dependency Graph, etc.
 * Implemented as singleton for easy access from any class.
 */
public class Environment {

    private static final Logger logger = LogManager.getLogger(Environment.class);

    private static Environment instance = null;

    private Configuration configuration;
    private ApiUnderTest apiUnderTest;
    private OpenApi openAPI;
    private OperationDependencyGraph operationDependencyGraph;
    private Dictionary globalDictionary;
    private ExtendedRandom random;

    private Environment() {}

    /**
     * Sets up testing environment given a configuration and an API under test (as class instance).
     * NOTE: the API under test specified in the configuration file as string is ignored, in favour of the ApiUnderTest
     * class instance provided as argument to the method.
     * @param configuration the configuration.
     * @param apiUnderTest the API under test.
     * @throws CannotParseOpenApiException in case the provided specification is invalid.
     */
    public Environment setUp(@NotNull Configuration configuration, @NotNull ApiUnderTest apiUnderTest) throws CannotParseOpenApiException {
        this.configuration = configuration;
        this.apiUnderTest = apiUnderTest;
        NormalizedParameterName.setQualifiableNames(configuration.getQualifiableParameterNames());
        this.apiUnderTest = apiUnderTest;
        this.openAPI = new OpenApiParser(apiUnderTest).parse();
        this.operationDependencyGraph = new OperationDependencyGraph(openAPI);
        this.globalDictionary = new Dictionary();
        this.random = new ExtendedRandom();

        return this;
    }

    public static Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public ApiUnderTest getApiUnderTest() {
        return apiUnderTest;
    }

    public OpenApi getOpenAPI() {
        return openAPI;
    }

    public void setOpenAPI(OpenApi openAPI) {
        this.openAPI = openAPI;
    }

    public OperationDependencyGraph getOperationDependencyGraph() {
        return operationDependencyGraph;
    }

    public void setOperationDependencyGraph(OperationDependencyGraph operationDependencyGraph) {
        this.operationDependencyGraph = operationDependencyGraph;
    }

    public Dictionary getGlobalDictionary() {
        return globalDictionary;
    }

    public void setGlobalDictionary(Dictionary dictionary) {
        this.globalDictionary = dictionary;
    }

    public ExtendedRandom getRandom() {
        return random;
    }

    public void setRandom(ExtendedRandom random) {
        this.random = random;
    }

    /**
     * Resets the testing environment by setting all attributes to null.
     */
    public Environment reset() {
        configuration = null;
        apiUnderTest = null;
        openAPI = null;
        operationDependencyGraph = null;
        globalDictionary = null;
        random = null;
        return this;
    }
 }
