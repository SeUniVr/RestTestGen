package io.resttestgen.core.environment;

import io.resttestgen.core.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationTest {

    @Test
    public void testFileName() {
        Configuration c = new Configuration(true);
        assertEquals(c.getSpecificationFileName(), "resources/openapi.json");
    }
}
