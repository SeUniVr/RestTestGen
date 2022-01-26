package io.resttestgen.core.openapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EditReadOnlyOperationException extends RuntimeException {

    private static final Logger logger = LogManager.getLogger(EditReadOnlyOperationException.class);

    public EditReadOnlyOperationException(Operation o) {
        super("Modifications on read-only instance of operation '" + o.toString() + "' is forbidden.");
    }
}
