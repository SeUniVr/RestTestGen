package io.resttestgen.core.datatype.parameter.exceptions;

import io.resttestgen.core.datatype.parameter.ParameterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class ParameterCreationException extends RuntimeException {
    private static final Logger logger = LogManager.getLogger(ParameterFactory.class);

    public ParameterCreationException() {
        logger.warn("Unable to create parameter");
    }

    public ParameterCreationException(String description) {
        logger.warn(description);
    }

    public ParameterCreationException(Map<String, Object> parameterMap) {
        logger.warn("Unable to create parameter " + parameterMap);
    }
}
