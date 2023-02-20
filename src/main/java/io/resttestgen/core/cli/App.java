package io.resttestgen.core.cli;

import io.resttestgen.core.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.CannotParseOpenAPIException;
import io.resttestgen.core.openapi.InvalidOpenAPIException;
import io.resttestgen.core.testing.Strategy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * CLI application
 */
public class App {

    private static final Logger logger = LogManager.getLogger(App.class);
    private static final String toolVersion = "23.02";
    private static final String messageHeader = "RestTestGen Core " + toolVersion;
    private static final String helpMessage = messageHeader + "\n"
            + "Arguments:\n"
            + "   (OPTIONAL)  -s  testing strategy class name. Default: NominalAndErrorStrategy.\n"
            + "   (OPTIONAL)  -l  log verbosity level (DEBUG, INFO, WARN, ERROR). Default: INFO.\n"
            + "   (OPTIONAL)  -h  shows this message.";
    private static final String strategyPackageName = "io.resttestgen.implementation.strategy";

    /**
     * Entry point of the CLI application.
     * @param args command line arguments.
     */
    public static void main(String[] args) {

        // Check for help argument
        checkHelpArgument(args);

        // Set logger verbosity to INFO and welcome user
        Configurator.setRootLevel(Level.DEBUG);
        logger.info(messageHeader + " started.");

        // Configuration: the constructor loads the configuration from file, if available, or uses default
        Configuration configuration = new Configuration(true);

        // Check CLI arguments and overwrite configuration of specified arguments
        checkArguments(args, configuration);

        // Set up the environment (parses specification, creates ODG, etc.) starting from the configuration
        Environment environment = Environment.getInstance();
        try {
            environment.setUp(configuration);
        } catch (CannotParseOpenAPIException e) {
            logger.error("Cannot parse the provided OpenAPI specification.");
            System.exit(-1);
        } catch (InvalidOpenAPIException e) {
            logger.error("The provided OpenAPI specification is not valid.");
            System.exit(-1);
        } catch (Exception e) {
            logger.error("Error while creating the environment");
            e.printStackTrace();
            System.exit(-1);
        }

        // Launch the testing strategy class with reflections
        logger.info("Launching strategy with class name '" + environment.getConfiguration().getStrategyName() + "'");
        final String strategyClassFullName = strategyPackageName + "." + environment.getConfiguration().getStrategyName();
        try {
            Class<?> strategyClass = Class.forName(strategyClassFullName);
            Strategy strategy = (Strategy) strategyClass.getDeclaredConstructor().newInstance();
            Method startMethod = strategyClass.getMethod("start");
            startMethod.invoke(strategy);
        } catch (ClassNotFoundException e) {
            logger.error("Strategy class with name '" + strategyClassFullName + "' not found.");
        } catch (NoSuchMethodException e) {
            logger.error("Method 'start' not found in class '" + strategyClassFullName + "'.");
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops execution and prints help message if "-h" argument is found.
     * @param args command line arguments
     */
    private static void checkHelpArgument(String[] args) {
        CommandOptions cmd = new CommandOptions(args);

        if (cmd.hasOption("-h")) {
            System.out.println(helpMessage);
            System.exit(0);
        }
    }

    /**
     * Parses the command line arguments and applies the provided settings.
     * @param args command line arguments
     */
    private static void checkArguments(String[] args, Configuration configuration) {
        CommandOptions cmd = new CommandOptions(args);

        if (cmd.hasOption("-l")) {
            if (cmd.valueOf("-l").equalsIgnoreCase("DEBUG")) {
                Configurator.setRootLevel(Level.DEBUG);
                logger.info("Log verbosity level set to DEBUG.");
            } else if (cmd.valueOf("-l").equalsIgnoreCase("INFO")) {
                logger.info("Log verbosity level set to INFO.");
            } else if (cmd.valueOf("-l").equalsIgnoreCase("WARN")) {
                Configurator.setRootLevel(Level.WARN);
            } else if (cmd.valueOf("-l").equalsIgnoreCase("ERROR")) {
                Configurator.setRootLevel(Level.ERROR);
            } else {
                logger.warn("The provided value for -l argument is not valid. Log verbosity level set to info.");
            }
        }

        if (cmd.hasOption("-s")) {
            configuration.setStrategyName(cmd.valueOf("-s"));
        }
    }
}