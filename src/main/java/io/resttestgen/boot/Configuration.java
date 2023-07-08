package io.resttestgen.boot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Configuration {

    private static final Logger logger = LogManager.getLogger(Configuration.class);
    private static final Yaml yaml = new Yaml();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File ymlConfig = new File("./rtg-config.yml");
    private static final File yamlConfig = new File("./rtg-config.yaml");
    private static final File jsonConfig = new File("./rtg-config.json");

    private Level logVerbosity;
    private String apiUnderTest;
    private String strategyClassName;
    private String testingSessionName;
    private final List<String> qualifiableParameterNames;
    private boolean globalOutputPath;
    private String odgFileName;

    /**
     * Initializes the default configuration
     */
    private Configuration() {

        logVerbosity = Level.INFO;

        apiUnderTest = "bookstore";

        // Choose random testing session name with date and random number
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        LocalDateTime now = LocalDateTime.now();
        testingSessionName = "testing-session-" + dtf.format(now);

        // Use local path (API path) by default
        globalOutputPath = false;

        strategyClassName = "NominalAndErrorStrategy";

        odgFileName = "odg.dot";

        qualifiableParameterNames = new ArrayList<>();
        qualifiableParameterNames.add("id");
        qualifiableParameterNames.add("name");
    }

    public Level getLogVerbosity() {
        return logVerbosity;
    }

    public void setLogVerbosity(Level logVerbosity) {
        this.logVerbosity = logVerbosity;
    }

    public String getTestingSessionName() {
        return testingSessionName;
    }

    public void setTestingSessionName(String testingSessionName) {
        this.testingSessionName = testingSessionName;
    }

    public String getOutputPath() {
        StringBuilder outputPath = new StringBuilder(System.getProperty("user.dir")).append("/");
        if (!globalOutputPath) {
            outputPath.append("apis/").append(apiUnderTest).append("/");
        }
        outputPath.append("results/");
        return outputPath.toString();
    }

    public String getStrategyClassName() {
        return strategyClassName;
    }

    public void setStrategyClassName(String strategyClassName) {
        this.strategyClassName = strategyClassName;
    }

    public void setResultsLocation(String location) {
        if (location.trim().equalsIgnoreCase("global")) {
            this.globalOutputPath = true;
            return;
        }
        this.globalOutputPath = false;
    }

    public List<String> getQualifiableParameterNames() {
        return Collections.unmodifiableList(this.qualifiableParameterNames);
    }

    public void clearQualifiableParameterNames() {
        this.qualifiableParameterNames.clear();
    }

    public void addQualifiableParameterName(String qualifiableParameterName) {
        this.qualifiableParameterNames.add(qualifiableParameterName);
    }

    public String getOdgFileName() {
        return odgFileName;
    }

    public void setOdgFileName(String odgFileName) {
        this.odgFileName = odgFileName;
    }

    public String getApiUnderTest() {
        return apiUnderTest;
    }

    public void setApiUnderTest(String apiUnderTest) {
        this.apiUnderTest = apiUnderTest;
    }

    public static Configuration defaultConfiguration() {
        return new Configuration();
    }

    @SuppressWarnings("unchecked")
    public static Configuration fromFile() throws IOException {

        // Load default configuration
        Configuration configuration = Configuration.defaultConfiguration();

        Map<String, Object> configMap;

        // Search for configuration file
        if (ymlConfig.exists()) {
            configMap = yaml.load(new FileInputStream(ymlConfig));
        } else if (yamlConfig.exists()) {
            configMap = yaml.load(new FileInputStream(yamlConfig));
        } else if (jsonConfig.exists()) {
            configMap = gson.fromJson(Files.newBufferedReader(jsonConfig.toPath()), Map.class);
        } else {
            logger.warn("RestTestGen configuration file not found. Using default configuration.");
            return configuration;
        }

        // If parsing of configMap failed for some reason, just return default configuration.
        if (configMap == null) {
            logger.warn("Could not parse RestTestGen configuration file. Using default configuration.");
            return configuration;
        }

        if (configMap.get("apiUnderTest") != null) {
            configuration.setApiUnderTest(configMap.get("apiUnderTest").toString());
        }
        if (configMap.get("logVerbosity") != null) {
            configuration.setLogVerbosity(Level.getLevel(configMap.get("logVerbosity").toString()));
        }
        if (configMap.get("testingSessionName") != null) {
            configuration.setTestingSessionName(configMap.get("testingSessionName").toString());
        }
        if (configMap.get("strategyClassName") != null) {
            configuration.setStrategyClassName(configMap.get("strategyClassName").toString());
        }
        if (configMap.get("odgFileName") != null) {
            configuration.setOdgFileName(configMap.get("odgFileName").toString());
        }
        if (configMap.get("qualifiableParameterNames") != null) {
            List<String> names = (List<String>) configMap.get("qualifiableParameterNames");
            names.forEach(configuration::addQualifiableParameterName);
        }
        if (configMap.get("resultsLocation") != null) {
            configuration.setResultsLocation(configMap.get("resultsLocation").toString());
        }

        return configuration;
    }
}
