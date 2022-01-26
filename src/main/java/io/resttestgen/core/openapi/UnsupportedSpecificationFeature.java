package io.resttestgen.core.openapi;

import io.resttestgen.core.datatype.parameter.ParameterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UnsupportedSpecificationFeature extends RuntimeException {

    private static final Logger logger = LogManager.getLogger(ParameterFactory.class);

    public UnsupportedSpecificationFeature(String description) {
        logger.warn(description);
    }
}
