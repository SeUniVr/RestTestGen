package io.resttestgen.core;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.helper.ResponseAnalyzer;
import io.resttestgen.core.openapi.CannotParseOpenAPIException;
import io.resttestgen.core.openapi.InvalidOpenAPIException;
import io.resttestgen.core.openapi.OpenAPI;
import io.resttestgen.core.openapi.OpenAPIParser;
import io.resttestgen.core.operationdependencygraph.OperationDependencyGraph;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

/**
 * A container class for all the environment elements of the RestTestGen Core, such as the parsed specification,
 * dictionaries, the Operation Dependency Graph, etc.
 */
public class Environment {

    private static final Logger logger = LogManager.getLogger(Environment.class);

    public Configuration configuration;
    public OpenAPI openAPI;
    public OperationDependencyGraph operationDependencyGraph;
    public Dictionary dictionary;
    public ResponseAnalyzer responseAnalyzer;
    public OkHttpClient httpClient;
    public ExtendedRandom random;
    private Map<String, String> auth;

    /**
     * Given the configuration, the constructor parses the OpenAPI specification, builds the Operation Dependency Graph,
     * fills the default dictionary and prepares the response dictionary. These objects can be used by strategies,
     * fuzzers and oracles to
     * @param configuration
     */
    public Environment(Configuration configuration) throws CannotParseOpenAPIException, InvalidOpenAPIException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, InterruptedException {
        this.configuration = configuration;
        NormalizedParameterName.setQualifiableNames(configuration.getQualifiableNames());
        this.openAPI = new OpenAPIParser(Paths.get(configuration.getSpecificationFileName())).parse();
        this.operationDependencyGraph = new OperationDependencyGraph(this.openAPI, this.configuration);
        this.dictionary = new Dictionary();
        this.responseAnalyzer = new ResponseAnalyzer(this);
        //this.defaultDictionary = loadDefaultDictionary(configuration);
        //this.responseDictionary = prepareResponseDictionary();
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
            this.auth = new Gson().fromJson(stringBuilder.toString(), Map.class);
        } catch (JsonSyntaxException e) {
            logger.error("Authorization script must return a valid json. Instead, its result was:\n" + stringBuilder);
        }
        // Check that the json contains all and only the required fields
        if (this.auth.size() != 4 || !this.auth.containsKey("name") || !this.auth.containsKey("value") ||
                !this.auth.containsKey("in") || !this.auth.containsKey("timeout")) {
            logger.error("Authorization script must return a json containing all and only the fields" +
                    "'name', 'value', 'in', 'timeout'. Instead, its result was:\n" + stringBuilder);
        }
    }

    /**
     * Loads the values of the default dictionary from file
     * @param configuration
     */
    private static Dictionary loadDefaultDictionary(Configuration configuration) {
        // TODO: load default dictionary either form the resources or from file if provided
        return null;
    }

    /**
     *
     * @return
     */
    private static Dictionary prepareResponseDictionary() {
        // TODO: prepare an empty response dictionary
        return null;
    }

    public Map<String, String> getAuth() {
        return Collections.unmodifiableMap(this.auth);
    }
}
