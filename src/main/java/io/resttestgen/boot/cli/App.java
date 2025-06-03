package io.resttestgen.boot.cli;

import io.resttestgen.boot.Configuration;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.openapi.InvalidOpenApiException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;

/**
 * CLI application
 */
public class App {

    private static final Logger logger = LogManager.getLogger(App.class);
    private static final String toolVersion = "25.06";
    private static final String messageHeader = "RestTestGen CLI " + toolVersion;
    private static final String helpMessage = messageHeader + "\n"
            + "Arguments:\n"
            + "   (OPTIONAL)  -a  API under test. Default: Book Store.\n"
            + "   (OPTIONAL)  -s  testing strategy class name. Default: NominalAndErrorStrategy.\n"
            + "   (OPTIONAL)  -l  log verbosity level (DEBUG, INFO, WARN, ERROR). Default: INFO.\n"
            + "   (OPTIONAL)  -h  shows this message.";

    /**
     * Entry point of the CLI application.
     * @param args command line arguments.
     */
    public static void main(String[] args) {

        // Check for help argument
        checkHelpArgument(args);

        // Set logger verbosity to INFO and welcome user
        Configurator.setRootLevel(Level.INFO);
        logger.info(messageHeader + " started.");

        // Configuration: the constructor loads the configuration from file, if available, or uses default
        Configuration configuration;
        try {
            configuration = Configuration.fromFile();
        } catch (IOException e) {
            configuration = Configuration.defaultConfiguration();
            logger.info("Configuration file not found. Using default configuration.");
        }

        // Check CLI arguments and overwrite configuration of specified arguments
        checkArguments(args, configuration);

        // Start RestTestGen using the starter class
        try {
            Starter.start(configuration);
            logger.info("RestTestGen execution completed successfully.");
        } catch (InvalidOpenApiException e) {
            logger.error("The provided OpenAPI specification is not valid. {}", e.getMessage());
            System.exit(-1);
        } catch (Exception e) {
            logger.error("An error occurred during the execution of RestTestGen. Please report it on GitHub.");
            e.printStackTrace();
            System.exit(-1);
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

        if (cmd.hasOption("-a")) {
            configuration.setApiUnderTest(cmd.valueOf("-a"));
        }

        if (cmd.hasOption("-s")) {
            configuration.setStrategyClassName(cmd.valueOf("-s"));
        }
    }
}