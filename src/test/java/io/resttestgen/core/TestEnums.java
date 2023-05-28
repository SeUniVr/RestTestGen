package io.resttestgen.core;

import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestEnums {

    private static final Logger logger = LogManager.getLogger(TestEnums.class);

    @Test
    public void testHTTPMethod() {
        logger.info("Test values from getMethod function in HTTPMethod enum");

        assertEquals(HttpMethod.GET, HttpMethod.getMethod("Get"));
        assertEquals(HttpMethod.POST, HttpMethod.getMethod("POST"));
        assertEquals(HttpMethod.PUT, HttpMethod.getMethod("put"));
        assertEquals(HttpMethod.PATCH, HttpMethod.getMethod("pAtch"));
        assertEquals(HttpMethod.DELETE, HttpMethod.getMethod("Delete"));
        assertEquals(HttpMethod.HEAD, HttpMethod.getMethod("heaD"));
        assertEquals(HttpMethod.OPTIONS, HttpMethod.getMethod("options"));
        assertEquals(HttpMethod.TRACE, HttpMethod.getMethod("trace"));
        assertThrows(IllegalArgumentException.class, () -> HttpMethod.getMethod("Custom"));
        assertThrows(IllegalArgumentException.class, () -> HttpMethod.getMethod(null));
    }

    @Test
    public void testParameterStyle() {
        logger.info("Test values from getStyleFromString function in ParameterStyle enum");

        assertEquals(ParameterStyle.SIMPLE, ParameterStyle.getStyleFromString("simple"));
        assertEquals(ParameterStyle.DEEP_OBJECT, ParameterStyle.getStyleFromString("DEEPOBJECT"));
        assertEquals(ParameterStyle.FORM, ParameterStyle.getStyleFromString("form"));
        assertEquals(ParameterStyle.LABEL, ParameterStyle.getStyleFromString("LABeL"));
        assertEquals(ParameterStyle.MATRIX, ParameterStyle.getStyleFromString("matriX"));
        assertEquals(ParameterStyle.PIPE_DELIMITED, ParameterStyle.getStyleFromString("PIPEDELIMITED"));
        assertEquals(ParameterStyle.SPACE_DELIMITED, ParameterStyle.getStyleFromString("SPACEDELIMITED"));
        assertNull(ParameterStyle.getStyleFromString(null));
        assertNull(ParameterStyle.getStyleFromString("NotAStyle"));
    }

    @Test
    public void testParameterType() {
        logger.info("Test values from getTypeFromString function in ParameterType enum");

        assertEquals(ParameterType.ARRAY, ParameterType.getTypeFromString("array"));
        assertEquals(ParameterType.BOOLEAN, ParameterType.getTypeFromString("BOOLEAN"));
        assertEquals(ParameterType.INTEGER, ParameterType.getTypeFromString("INTEGEr"));
        assertEquals(ParameterType.NUMBER, ParameterType.getTypeFromString("NUMBER"));
        assertEquals(ParameterType.OBJECT, ParameterType.getTypeFromString("OBJECT"));
        assertEquals(ParameterType.STRING, ParameterType.getTypeFromString("STRiNG"));
        assertEquals(ParameterType.UNKNOWN, ParameterType.getTypeFromString("Custom"));
        assertEquals(ParameterType.MISSING, ParameterType.getTypeFromString(null));
    }

    @Test
    public void testParameterTypeFormat() {
        logger.info("Test values from getFormatFromString function in ParameterTypeFormat enum");

        assertEquals(ParameterTypeFormat.BINARY, ParameterTypeFormat.getFormatFromString("binary"));
        assertEquals(ParameterTypeFormat.BYTE, ParameterTypeFormat.getFormatFromString("BYTE"));
        assertEquals(ParameterTypeFormat.DATE, ParameterTypeFormat.getFormatFromString("DATE"));
        assertEquals(ParameterTypeFormat.DATE_TIME, ParameterTypeFormat.getFormatFromString("DATE-time"));
        assertEquals(ParameterTypeFormat.DOUBLE, ParameterTypeFormat.getFormatFromString("DOUBLE"));
        assertEquals(ParameterTypeFormat.FLOAT, ParameterTypeFormat.getFormatFromString("FLOAT"));
        assertEquals(ParameterTypeFormat.INT32, ParameterTypeFormat.getFormatFromString("INT32"));
        assertEquals(ParameterTypeFormat.INT64, ParameterTypeFormat.getFormatFromString("int64"));
        assertEquals(ParameterTypeFormat.PASSWORD, ParameterTypeFormat.getFormatFromString("PassWORD"));
        assertEquals(ParameterTypeFormat.MISSING, ParameterTypeFormat.getFormatFromString(null));
        assertEquals(ParameterTypeFormat.UNKNOWN, ParameterTypeFormat.getFormatFromString("Custom"));
    }
}
