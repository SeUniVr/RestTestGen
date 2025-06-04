package io.resttestgen.core.testing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public abstract class StrategyConfiguration {

    private static final Logger logger = LogManager.getLogger(StrategyConfiguration.class);
    private static final String strategyConfigurationFile = "strategy-config.yml";

    /**
     * Loads strategy configuration from file. If configuration is not available on file, returns default configuration.
     * @param type the type of the StrategyConfiguration.
     * @return the configuration.
     * @param <T> only supports subclasses of StrategyConfiguration.
     */
    @SuppressWarnings("unchecked")
    public static <T extends StrategyConfiguration> T loadConfiguration(Class<T> type) {

        Yaml yaml = new Yaml();
        File file = new File(strategyConfigurationFile);
        if (file.exists()) {
            String strategyName = type.getSimpleName().substring(0, type.getSimpleName().length() - "Configuration".length());
            try {
                InputStream inputStream = new FileInputStream(file);
                Map<String, Object> fileContent = yaml.load(inputStream);
                if (fileContent != null) {
                    Object thisConfigurationContent = fileContent.get(strategyName);
                    if (thisConfigurationContent != null) {
                        String thisConfigurationYaml = yaml.dump(thisConfigurationContent);
                        if (thisConfigurationYaml.length() > 2) {
                            return yaml.loadAs(thisConfigurationYaml, type);
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                logger.warn("Strategy configuration file not found. Using default configuration for strategy.");
            }
        }

        Constructor<?> constructor = null;
        try {
            constructor = type.getConstructor();
            return (T) constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // This should never happen... Added to remove "might be null" warning.
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
