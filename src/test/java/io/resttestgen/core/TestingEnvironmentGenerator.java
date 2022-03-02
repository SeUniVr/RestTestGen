package io.resttestgen.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestingEnvironmentGenerator {

    private static final Logger logger = LogManager.getLogger(Environment.class);

    public static Environment getTestingEnvironment() {
        Configuration c = new Configuration(true);
        try {
            Environment e = Environment.getInstance();
            e.setUp(c);
            return e;
        } catch (Exception e) {
            logger.error(e);
            throw new RuntimeException();
        }
    }

    public static Environment getTestingEnvironment(String filename) {
        Configuration c = new Configuration(true);
        c.setSpecificationFileName(filename);
        try {
            Environment e = new Environment();
            e.setUp(c);
            return e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
