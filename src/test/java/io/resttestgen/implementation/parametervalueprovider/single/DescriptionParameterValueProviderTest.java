package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;

public class DescriptionParameterValueProviderTest {

    private static Environment environment;
    private static Collection<LeafParameter> parameters;
    DescriptionParameterValueProvider descriptionParameterValueProvider = new DescriptionParameterValueProvider();

    @BeforeAll
    public static void setUp() throws IOException {
        environment = Starter.initEnvironment(ApiUnderTest.loadTestApiFromFile("values-in-description"));
        parameters = environment.getOpenAPI().getOperations().stream().findFirst().get().getLeaves();
    }

    @Test
    public void testExtractedValues() {
        // TODO: implement assertions
        for (LeafParameter p : parameters) {
            Collection<Object> values = descriptionParameterValueProvider.collectValuesFor(p);
            System.out.println(p + ": " + values);
        }
    }
}
