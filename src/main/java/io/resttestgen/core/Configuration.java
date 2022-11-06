package io.resttestgen.core;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private static final Logger logger = LogManager.getLogger(Configuration.class);
    private static final Path configurationPath = Paths.get("rtg_config.json"); // File name of the configuration

    private Level logVerbosity = Level.INFO;
    private String testingSessionName;
    private String outputPath;
    private String specificationFileName;
    private String authCommand;
    private String strategyName;
    private List<String> qualifiableNames;
    private String odgFileName;

    /**
     * Initializes the default configuration
     */
    public Configuration() {
        // Choose random testing session name with date and random number
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        LocalDateTime now = LocalDateTime.now();
        testingSessionName = "testing-session-" + dtf.format(now);

        outputPath = System.getProperty("user.dir") + "/output/";

        specificationFileName = "openapi.json";

        strategyName = "NominalAndErrorStrategy";

        odgFileName = "odg.dot";

        qualifiableNames = new ArrayList<>();
        qualifiableNames.add("id");
        qualifiableNames.add("name");
    }

    public Configuration(Boolean loadFromFile) {
        this();
        if (loadFromFile) {
            // Overwrite the default configuration with the configuration on file, if available
            try {
                Gson gson = new Gson();
                Reader reader = Files.newBufferedReader(configurationPath);
                Configuration tempConfiguration = gson.fromJson(reader, Configuration.class);

                this.logVerbosity = tempConfiguration.logVerbosity;
                this.outputPath = tempConfiguration.outputPath;
                this.specificationFileName = tempConfiguration.specificationFileName;
                this.testingSessionName = computeTestingSessionName();
                this.authCommand = tempConfiguration.authCommand;
                this.strategyName = tempConfiguration.strategyName;
                this.qualifiableNames = tempConfiguration.qualifiableNames;
                this.odgFileName = tempConfiguration.odgFileName;
            } catch (IOException e) {
                logger.warn("Could not read configuration from file. Using default configuration.");
            }
        }
    }

    private String computeTestingSessionName() {
        try {
            // Apply new name only if no custom name is provided
            if (testingSessionName.startsWith("testing-session-")) {
                String[] specificationSplit = specificationFileName.split("/");
                String specFileNameWithoutExtension = specificationSplit[specificationSplit.length - 1].split(".json")[0];
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                LocalDateTime now = LocalDateTime.now();
                return specFileNameWithoutExtension + "-" + dtf.format(now);
            } else {
                return testingSessionName;
            }
        } catch (Exception e) {
            // If any exception occurs, just return the initial name
            return testingSessionName;
        }
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
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getSpecificationFileName() {
        return specificationFileName;
    }

    public void setSpecificationFileName(String specificationFileName) {
        if (specificationFileName.endsWith(".json") || specificationFileName.endsWith(".yaml")) {
            this.specificationFileName = specificationFileName;
        }
    }

    public String getAuthCommand() {
        return authCommand;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public List<String> getQualifiableNames() {
        return qualifiableNames;
    }

    public String getOdgFileName() {
        return odgFileName;
    }

    public void setOdgFileName(String odgFileName) {
        this.odgFileName = odgFileName;
    }

}
