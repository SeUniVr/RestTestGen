package io.resttestgen.core.datatype.parameter.attributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum ParameterStyle {
    MATRIX,
    LABEL,
    FORM,
    SIMPLE,
    SPACE_DELIMITED,
    PIPE_DELIMITED,
    DEEP_OBJECT;

    private static final Logger logger = LogManager.getLogger(ParameterStyle.class);

    public static ParameterStyle getStyleFromString(String styleName) {
        if (styleName == null) {
            return null;
        }
        switch (styleName.toLowerCase()) {
            case "matrix":
                return MATRIX;
            case "label":
                return LABEL;
            case "form":
                return FORM;
            case "simple":
                return SIMPLE;
            case "spacedelimited":
                return SPACE_DELIMITED;
            case "pipedelimited":
                return PIPE_DELIMITED;
            case "deepobject":
                return DEEP_OBJECT;
            default:
                logger.warn("Unknown type \"" + styleName + "\".");
                return null;
        }
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase().replace("_", "");
    }
}
