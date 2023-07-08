package io.resttestgen.core.helper;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.CannotParseOpenApiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestCrudInformationExtractor {

    private static Environment environment;

    @BeforeAll
    public static void setUp() throws IOException, CannotParseOpenApiException {
        environment = Starter.initEnvironment(ApiUnderTest.loadApiFromFile("bookstore"));
    }

    @Test
    public void testClustering() {
        CrudInformationExtractor crudInformationExtractor = new CrudInformationExtractor();
        crudInformationExtractor.extract();
    }
}
