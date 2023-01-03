package io.resttestgen.core.helper;

import io.resttestgen.core.Environment;
import io.resttestgen.core.TestingEnvironmentGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
public class TestCrudInformationExtractor {

    private static Environment environment;

    @BeforeAll
    public static void setUp() {
        environment = TestingEnvironmentGenerator.getTestingEnvironment();
    }

    @Test
    public void testClustering() {
        CrudInformationExtractor crudInformationExtractor = new CrudInformationExtractor();
        crudInformationExtractor.extract();
    }
}
