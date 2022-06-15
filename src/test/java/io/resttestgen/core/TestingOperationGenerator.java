package io.resttestgen.core;

import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.openapi.InvalidOpenAPIException;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class TestingOperationGenerator {

    private static final Logger logger = LogManager.getLogger(TestingOperationGenerator.class);

    public static Operation getTestingOperation() throws InvalidOpenAPIException {
        return new Operation("/my/endpoint", HttpMethod.GET, new HashMap<>());
    }
}
