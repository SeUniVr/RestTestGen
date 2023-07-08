package io.resttestgen.boot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class ApiUnderTest {

    private static final Logger logger = LogManager.getLogger(ApiUnderTest.class);

    // User provided information
    private String name;
    private String specificationFileName = "openapi.json";
    private String host;
    private Map<String, String> authenticationCommands;
    private String resetCommand;
    private boolean resetBeforeTesting;


    // Computed information
    private transient String wildcard;
    private transient boolean loadedFromFile = false;
    private transient boolean testApi = false;
    private transient File dir;
    private transient String computedJsonSpecificationFileName;
    private transient String computedYamlSpecificationFileName;
    private transient String computedYmlSpecificationFileName;
    private transient Map<String, AuthenticationInfo> authenticationInfoMap = new LinkedHashMap<>();

    private static final Yaml yaml = new Yaml();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ApiUnderTest() {}

    /**
     * Creates a new instance of ApiUnderTest, either loading the API configuration from file (in case and API folder is
     * available in the file system), or return an empty instance (in case of new API).
     * @param wildcard, i.e., the name of the directory in which are contained (or will be contained) the configuration
     *                  and the specification of the API.
     */
    public ApiUnderTest(String wildcard) {

        this.wildcard = wildcard.trim();
        checkWildcard(this.wildcard);

        this.name = this.wildcard;

        dir = new File("./apis/" + this.wildcard + "/");

        if (dir.exists()) {
            throw new IllegalArgumentException("An API with the provided wildcard already exists. Please load it from file with the proper method.");
        }
    }

    /**
     * Checks the wildcard of the API. It can only contain letters, numbers, dashes, and underscores.
     * @param wildcard the wildcard to check.
     * @throws IllegalArgumentException if the wildcard is not properly formatted.
     */
    private static void checkWildcard(String wildcard) {
        if (!wildcard.matches("^[-a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Wildcard can only contain: letters (uppercase and lowercase), numbers, dashed, and underscores.");
        }
    }

    private String getPath() {
        if (isTestApi()) {
            return "./apis/.test-apis/" + wildcard + "/";
        } else {
            return "./apis/" + wildcard + "/";
        }
    }

    /**
     * Loads the API configuration from file. Supported files are api-config.yml, api-config.yaml, and api-config.json.
     * YAML configuration is the preferred one.
     */
    private void loadConfigurationFromFile() throws IOException {

        LinkedHashMap<String, Object> apiConfigMap = null;
        File ymlApiConfig = new File(getPath() + "api-config.yml");
        File yamlApiConfig = new File(getPath() + "api-config.yaml");
        File jsonApiConfig = new File(getPath() + "api-config.json");

        // Read configuration from file
        if (ymlApiConfig.exists()) {
            apiConfigMap = yaml.load(Files.newInputStream(ymlApiConfig.toPath()));
        } else if (yamlApiConfig.exists()) {
            apiConfigMap = yaml.load(Files.newInputStream(yamlApiConfig.toPath()));
        } else if (jsonApiConfig.exists()) {
            apiConfigMap = gson.fromJson(Files.newBufferedReader(jsonApiConfig.toPath()), LinkedHashMap.class);
        }

        if (apiConfigMap == null) {
            return;
        }

        // Apply configuration
        if (apiConfigMap.get("name") != null) {
            setName(apiConfigMap.get("name").toString());
        }
        if (apiConfigMap.get("specificationFileName") != null) {
            setSpecificationFileName(apiConfigMap.get("specificationFileName").toString());
        }
        if (apiConfigMap.get("host") != null) {
            setHost(this.host = apiConfigMap.get("host").toString());
        }
        if (apiConfigMap.get("authenticationCommands") != null) {
            this.authenticationCommands = (LinkedHashMap<String, String>) apiConfigMap.get("authenticationCommands");
            for (String description : this.authenticationCommands.keySet()) {
                this.authenticationInfoMap.put(description, new AuthenticationInfo(description, this.authenticationCommands.get(description)));
            }
        }
        if (apiConfigMap.get("resetCommand") != null) {
            setResetCommand(apiConfigMap.get("resetCommand").toString());
        }
        if (apiConfigMap.get("resetBeforeTesting") != null) {
            setResetBeforeTesting(Boolean.parseBoolean(apiConfigMap.get("resetBeforeTesting").toString()));
        }
    }

    private void computeSpecificationFileNames() {
        String noExtensionFileName = specificationFileName.substring(0, specificationFileName.lastIndexOf("."));
        computedJsonSpecificationFileName = noExtensionFileName + ".json";
        computedYmlSpecificationFileName = noExtensionFileName + ".yml";
        computedYamlSpecificationFileName = noExtensionFileName + ".yaml";
    }

    /**
     * Checks the presence of the OpenAPI specification in the API folder. The OpenAPI should be in JSON format. In case
     * of YAML specifications, they will be previously converted to JSON and stored in the API folder.
     */
    private void checkAndPrepareOpenApiSpecification() throws IOException {

        // Check if the JSON specification exists
        File jsonSpec = new File(getPath() + "specifications/" + computedJsonSpecificationFileName);
        if (jsonSpec.exists()) {
            return;
        }

        File ymlSpec = new File(getPath() + "specifications/" + computedYmlSpecificationFileName);
        File yamlSpec = new File(getPath() + "specifications/" + computedYamlSpecificationFileName);
        File finalSpec = null;

        // Check if YAML specification exists (with bot yaml and yml extensions)
        if (yamlSpec.exists()) {
            finalSpec = yamlSpec;
        } else if (ymlSpec.exists()) {
            finalSpec = ymlSpec;
        } else {
            throw new FileNotFoundException("Could not locate OpenAPI specification for this API. Make sure you " +
                    "placed the specification in the specifications/ folder with the proper file name.");
        }

        // Convert YAML specification to JSON specification
        logger.info("Could not find JSON OpenAPI specification for " + name + ". Converting YAML specification to " +
                "JSON. The JSON specification will be stored in the API folder for future use.");
        FileWriter writer = new FileWriter(jsonSpec);
        gson.toJson((Map<String, Object>) yaml.load(new FileInputStream(finalSpec)), writer);
        writer.close();
    }

    public String getWildcard() {
        return wildcard;
    }

    private void setWildcard(String wildcard) {
        this.wildcard = wildcard;
    }

    private File getDir() {
        return dir;
    }

    private void setDir(File dir) {
        this.dir = dir;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComputedJsonSpecificationPath() {
        return getPath() + "specifications/" + computedJsonSpecificationFileName;
    }

    public void setSpecificationFileName(String specificationFileName) {
        this.specificationFileName = specificationFileName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        logger.warn("The host from API configuration is currently not supported. Please change the server in the " +
                "OpenAPI specification to change the destination host.");
        this.host = host;
    }

    public String getAuthenticationCommand(String description) {
        return authenticationCommands.get(description);
    }

    public AuthenticationInfo getAuthenticationInfo(String description) {
        return authenticationInfoMap.get(description);
    }

    /**
     * Gets the default AuthenticationInfo for the API, i.e., the one with "default" as description. In case no
     * AuthenticationInfo has "default" as description, the first element of the map is returned.
     * @return the default AuthenticationInfo.
     */
    public AuthenticationInfo getDefaultAuthenticationInfo() {
        if (getAuthenticationInfo("default") != null) {
            return getAuthenticationInfo("default");
        }
        if (authenticationInfoMap.size() == 0) {
            return null;
        }
        Map.Entry<String, AuthenticationInfo> entry = authenticationInfoMap.entrySet().iterator().next();
        return entry.getValue();
     }

    public void addAuthenticationCommand(String description, String command) {
        authenticationCommands.put(description, command);
        authenticationInfoMap.put(description, new AuthenticationInfo(description, command));
    }

    public void removeAuthenticationCommand(String description) {
        authenticationCommands.remove(description);
        authenticationInfoMap.remove(description);
    }

    public String getResetCommand() {
        return resetCommand;
    }

    public void setResetCommand(String resetCommand) {
        this.resetCommand = resetCommand;
    }

    public boolean isResetBeforeTesting() {
        return resetBeforeTesting;
    }

    public void setResetBeforeTesting(boolean resetBeforeTesting) {
        this.resetBeforeTesting = resetBeforeTesting;
    }

    /**
     * Resets the API under test by executing the reset script.
     * TODO: implement.
     */
    public void reset() {

    }

    private void setLoadedFromFile(boolean loadedFromFile) {
        this.loadedFromFile = loadedFromFile;
    }

    public boolean isTestApi() {
        return testApi;
    }

    private void setTestApi(boolean testApi) {
        this.testApi = testApi;
    }

    public boolean isLoadedFromFile() {
        return loadedFromFile;
    }

    public static Set<ApiUnderTest> loadAllApisFromFile() {
        Set<ApiUnderTest> apis = new HashSet<>();

        // Get all the sub-folders of ".apis/"
        Set<String> apiDirs = Stream.of(Objects.requireNonNull(new File("./apis/").listFiles()))
                .filter(File::isDirectory).map(File::getName).collect(Collectors.toSet());

        // For each folder, load it as an ApiUnderTest
        for (String apiDir : apiDirs) {
            try {
                ApiUnderTest api = ApiUnderTest.loadApiFromFile(apiDir);
                apis.add(api);
            } catch (IOException e) {
                logger.warn("Could not load API with wildcard: " + apiDir + ". " + e.getMessage());
            }
        }

        return apis;
    }

    public static Set<ApiUnderTest> loadAllTestApisFromFile() {
        Set<ApiUnderTest> apis = new HashSet<>();

        // Get all the sub-folders of ".apis/"
        Set<String> apiDirs = Stream.of(Objects.requireNonNull(new File("./apis/.test-apis/").listFiles()))
                .filter(File::isDirectory).map(File::getName).collect(Collectors.toSet());

        // For each folder, load it as an ApiUnderTest
        for (String apiDir : apiDirs) {
            try {
                ApiUnderTest api = ApiUnderTest.loadApiFromFile(apiDir);
                apis.add(api);
            } catch (IOException e) {
                logger.warn("Could not load API with wildcard: " + apiDir + ". " + e.getMessage());
            }
        }

        return apis;
    }

    public static ApiUnderTest loadApiFromFile(String wildcard) throws IOException {

        wildcard = wildcard.trim();
        checkWildcard(wildcard);

        File dir = new File("./apis/" + wildcard + "/");

        // If the API directory exists, load configuration from file.
        if (dir.exists()) {

            ApiUnderTest apiUnderTest = new ApiUnderTest();
            apiUnderTest.setWildcard(wildcard);
            apiUnderTest.setName(wildcard);
            apiUnderTest.setDir(dir);
            apiUnderTest.setLoadedFromFile(true);

            // Load configuration file
            apiUnderTest.loadConfigurationFromFile();

            // Compute specification file names (for JSON and YAML specs)
            apiUnderTest.computeSpecificationFileNames();

            // Load OpenAPI specification from file
            apiUnderTest.checkAndPrepareOpenApiSpecification();

            return apiUnderTest;

        } else {
            throw new FileNotFoundException("Could not locate API directory: " + dir.getAbsolutePath());
        }
    }

    public static ApiUnderTest loadTestApiFromFile(String wildcard) throws IOException {

        wildcard = wildcard.trim();
        checkWildcard(wildcard);

        File dir = new File("./apis/.test-apis/" + wildcard + "/");

        // If the API directory exists, load configuration from file.
        if (dir.exists()) {

            ApiUnderTest apiUnderTest = new ApiUnderTest();
            apiUnderTest.setWildcard(wildcard);
            apiUnderTest.setName(wildcard);
            apiUnderTest.setLoadedFromFile(true);
            apiUnderTest.setTestApi(true);

            // Load configuration file
            apiUnderTest.loadConfigurationFromFile();

            // Compute specification file names (for JSON and YAML specs)
            apiUnderTest.computeSpecificationFileNames();

            // Load OpenAPI specification from file
            apiUnderTest.checkAndPrepareOpenApiSpecification();

            return apiUnderTest;

        } else {
            throw new FileNotFoundException("Could not locate API directory: " + dir.getAbsolutePath());
        }
    }

    /**
     * Stores/updates the configuration for this API to file.
     */
    public void save() {
        // TODO: implement
    }

    @Override
    public String toString() {
        return name;
    }
}
