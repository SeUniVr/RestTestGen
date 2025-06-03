package io.resttestgen.core.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;

import static io.resttestgen.core.datatype.parameter.attributes.ParameterType.*;
import static io.resttestgen.core.helper.ObjectHelper.castToParameterValueType;
import static org.junit.jupiter.api.Assertions.*;

public class TestObjectHelper {

    private static final Logger logger = LogManager.getLogger(TestObjectHelper.class);

    @Disabled
    @Test
    public void testCastToParameterValueType() {
        logger.info("Test castings done by class ObjectHelper");

        assertEquals(1, castToParameterValueType(1, INTEGER));
        assertEquals(1.0, castToParameterValueType("1", INTEGER));
        assertEquals(0.1, castToParameterValueType("0.1", INTEGER));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(true, INTEGER));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(null, INTEGER));

        assertEquals(1, castToParameterValueType(1, NUMBER));
        assertEquals(1.0, castToParameterValueType("1", NUMBER));
        assertEquals(0.1, castToParameterValueType("0.1", NUMBER));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(true, NUMBER));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(null, NUMBER));

        assertEquals(true, castToParameterValueType(1, BOOLEAN));
        assertEquals(true, castToParameterValueType(1.0, BOOLEAN));
        assertEquals(true, castToParameterValueType("1", BOOLEAN));
        assertEquals(false, castToParameterValueType(0, BOOLEAN));
        assertEquals(false, castToParameterValueType("0", BOOLEAN));
        assertEquals(false, castToParameterValueType("0.0", BOOLEAN));
        assertEquals(true, castToParameterValueType("true", BOOLEAN));
        assertEquals(false, castToParameterValueType("false", BOOLEAN));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(12, BOOLEAN));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(null, BOOLEAN));
        assertThrows(ClassCastException.class, () -> castToParameterValueType("aaa", BOOLEAN));

        assertEquals(new HashMap<>(), castToParameterValueType(new HashMap<>(), OBJECT));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(null, OBJECT));
        assertThrows(ClassCastException.class, () -> castToParameterValueType("12", OBJECT));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(true, OBJECT));

        assertEquals(new LinkedList<>(), castToParameterValueType(new LinkedList<>(), ARRAY));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(null, ARRAY));
        assertThrows(ClassCastException.class, () -> castToParameterValueType("12", ARRAY));
        assertThrows(ClassCastException.class, () -> castToParameterValueType(true, ARRAY));

        assertEquals("", castToParameterValueType("", STRING));
        assertEquals("1", castToParameterValueType(1, STRING));
        assertEquals("1.5", castToParameterValueType(1.5, STRING));
        assertEquals("true", castToParameterValueType(true, STRING));
        assertEquals("false", castToParameterValueType(false, STRING));
    }
}
