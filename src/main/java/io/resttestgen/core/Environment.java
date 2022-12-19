package io.resttestgen.core;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.CannotParseOpenAPIException;
import io.resttestgen.core.openapi.InvalidOpenAPIException;
import io.resttestgen.core.openapi.OpenAPI;
import io.resttestgen.core.openapi.OpenAPIParser;
import io.resttestgen.core.operationdependencygraph.OperationDependencyGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A container class for all the environment components of the RestTestGen Core, such as the parsed specification,
 * dictionaries, the Operation Dependency Graph, etc.
 * Implemented as singleton for easy access from any class.
 */
public class Environment {

    private static final Logger logger = LogManager.getLogger(Environment.class);

    private static Environment instance = null;

    private Configuration configuration;
    private OpenAPI openAPI;
    private OperationDependencyGraph operationDependencyGraph;
    private Dictionary globalDictionary;
    private ExtendedRandom random;
    private List<AuthenticationInfo> authInfo;

    Environment() {

    }

    /**
     * Given the configuration, the constructor parses the OpenAPI specification, builds the Operation Dependency Graph,
     * fills the default dictionary and prepares the response dictionary. These objects can be used by strategies,
     * fuzzers and oracles to
     * @param configuration the provided configuration.
     */
    public void setUp(Configuration configuration) throws CannotParseOpenAPIException, InvalidOpenAPIException,
            IOException {

        this.configuration = configuration;
        NormalizedParameterName.setQualifiableNames(configuration.getQualifiableNames());
        this.openAPI = new OpenAPIParser(Paths.get(configuration.getSpecificationFileName())).parse();
        this.operationDependencyGraph = new OperationDependencyGraph(openAPI);
        this.globalDictionary = new Dictionary();
        this.random = new ExtendedRandom();

        // Exec auth command
        if (configuration.getAuthCommand() == null) {
            logger.info("No authentication defined in the configuration file");
            return;
        }

        Process process = Runtime.getRuntime().exec(configuration.getAuthCommand());
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        StringBuilder stringBuilder = new StringBuilder();
        String s;
        while ((s = stdError.readLine()) != null) {
            logger.error(s);
        }
        while ((s = stdInput.readLine()) != null) {
            stringBuilder.append(s);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object>[] maps = new Gson().fromJson(stringBuilder.toString(), Map[].class);
            this.authInfo = new LinkedList<>();
            for (Map<String, Object> map : maps) {
                this.authInfo.add(AuthenticationInfo.parse(map));
            }
        } catch (JsonSyntaxException | NullPointerException e) {
            logger.error("Authorization script must return a valid json. Instead, its result was:\n" + stringBuilder);
        }
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

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    public void setOpenAPI(OpenAPI openAPI) {
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

    public AuthenticationInfo getAuthenticationInfo(int index) {
        if (this.authInfo != null && index >= 0 && index < authInfo.size()) {
            return authInfo.get(index);
        }
        return null;
    }
    public AuthenticationInfo getAuthenticationInfo(String description) {
        return authInfo.stream().filter(i -> i.getDescription().equals(description)).findFirst().orElse(null);
    }

    public List<AuthenticationInfo> getAuthenticationInfoList() {
        return this.authInfo;
    }
 }
