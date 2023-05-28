package io.resttestgen.implementation.strategy.support;

import io.resttestgen.core.Environment;
import io.resttestgen.core.testing.Strategy;

import java.io.IOException;

/**
 * This strategy is not actually used to test, but it will only output the parsed OpenAPI specification.
 */
@SuppressWarnings("unused")
public class ExportSpecificationStrategy extends Strategy {

    @Override
    public void start() {
        try {
            Environment.getInstance().getOpenAPI().exportAsJsonOpenApiSpecification("exported_spec.json");
        } catch (IOException e) {
            System.out.println("Could not export spec.");
        }
    }
}