package io.resttestgen.core.helper;

import io.resttestgen.core.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.CannotParseOpenAPIException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestRuleExtractorProxy {

    private static final Environment environment = Environment.getInstance();

    @BeforeAll
    public static void setUp() throws CannotParseOpenAPIException, IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Configuration configuration = new Configuration(true);
        environment.setUp(configuration);
    }

    @Test
    public void testSingleParameterRule() {
        //NlpRestTestProxy.extractRuleFromParameterText();
    }
}
