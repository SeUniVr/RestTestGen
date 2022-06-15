package io.resttestgen.core.openapi;

import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import static io.resttestgen.core.openapi.Helper.getJSONMap;
import static org.junit.jupiter.api.Assertions.*;

public class TestOperation {

    private static final Logger logger = LogManager.getLogger(TestOperation.class);

    @BeforeAll
    public static void setNormalizer() {
        Helper.setNormalizer();
    }

    @Test
    public void testReadOnly() throws IOException, InvalidOpenAPIException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        logger.info("Test parsing read-only properties of Operation and its associated Parameters");

        String endpoint = "/pets";
        HttpMethod method = HttpMethod.POST;
        Operation o = new Operation(endpoint, method, getJSONMap("build/resources/test/operations/postPets_full.json"));

        // test read-only on operation
        assertTrue(o.isReadOnly());
        assertThrows(EditReadOnlyOperationException.class, () -> o.setPathParameters(null));
        assertThrows(EditReadOnlyOperationException.class, () -> o.setHeaderParameters(null));
        assertThrows(EditReadOnlyOperationException.class, () -> o.setQueryParameters(null));
        assertThrows(EditReadOnlyOperationException.class, () -> o.setCookieParameters(null));
        assertThrows(EditReadOnlyOperationException.class, () -> o.setRequestBody(null));
        // Read-only fields of Operation instance
        assertThrows(UnsupportedOperationException.class, () -> o.getPathParameters().add(null));
        assertThrows(UnsupportedOperationException.class, () -> o.getHeaderParameters().add(null));
        assertThrows(UnsupportedOperationException.class, () -> o.getQueryParameters().add(null));
        assertThrows(UnsupportedOperationException.class, () -> o.getCookieParameters().add(null));
        assertThrows(UnsupportedOperationException.class, () -> o.getOutputParameters().put("", null));
        // Not read-only: do not affect Operation instance state
        assertDoesNotThrow(() -> o.getOutputParametersSet().add(null));
        assertDoesNotThrow(() -> o.getReferenceLeaves().add(null));

        // clone has read-only true by default
        Operation oClone = o.deepClone();
        assertFalse(oClone.isReadOnly());

        // Test read-only on operation parameters
        Set<ParameterElement> headerParameters = o.getHeaderParameters();
        StringParameter apiKey = (StringParameter) headerParameters.stream().findAny().get();
        assertThrows(EditReadOnlyOperationException.class, () -> apiKey.addExample(new Object()));
        assertThrows(EditReadOnlyOperationException.class, () -> apiKey.setValue(new Object()));
        assertThrows(UnsupportedOperationException.class, () -> apiKey.getExamples().add(new Object()));
        assertThrows(UnsupportedOperationException.class, () -> apiKey.getEnumValues().add(new Object()));
        // Test not read-only clone
        Set<ParameterElement> cloneHeaderParameters = oClone.getHeaderParameters();
        StringParameter cloneApiKey = (StringParameter) cloneHeaderParameters.stream().findAny().get();
        assertDoesNotThrow(() -> cloneApiKey.addExample(new Object()));
        assertDoesNotThrow(() -> cloneApiKey.setValue(new Object()));
        assertDoesNotThrow(() -> cloneHeaderParameters.add(null));

        Set<ParameterElement> queryParameters = o.getQueryParameters();
        NumberParameter paramNumber = (NumberParameter) queryParameters.stream().findAny().get();
        assertThrows(EditReadOnlyOperationException.class, () -> paramNumber.addExample(new Object()));
        assertThrows(EditReadOnlyOperationException.class, () -> paramNumber.setValue(new Object()));
        // Test not read-only clone
        Set<ParameterElement> cloneQueryParameters = oClone.getQueryParameters();
        NumberParameter cloneParamNumber = (NumberParameter) cloneQueryParameters.stream().findAny().get();
        assertDoesNotThrow(() -> cloneParamNumber.addExample(new Object()));
        assertDoesNotThrow(() -> cloneParamNumber.setValue(new Object()));
        assertDoesNotThrow(() -> cloneQueryParameters.add(null));

        ParameterObject body = (ParameterObject) o.getRequestBody();
        assertThrows(EditReadOnlyOperationException.class, () -> body.addExample(new Object()));
        assertThrows(EditReadOnlyOperationException.class, body::removeUninitializedParameters);
        assertThrows(EditReadOnlyOperationException.class, () -> body.setKeepIfEmpty(true));
        assertThrows(UnsupportedOperationException.class, () -> body.getProperties().add(null));
        assertThrows(UnsupportedOperationException.class, () -> body.getExamples().add(null));
        assertThrows(UnsupportedOperationException.class, () -> body.getEnumValues().add(null));
        // Not read-only: do not affect Parameter instance state
        assertDoesNotThrow(() -> body.getLeaves().add(null));
        // Test not read-only clone
        ParameterObject cloneBody = (ParameterObject) oClone.getRequestBody();
        assertDoesNotThrow(() -> cloneBody.addExample(new HashMap<>()));
        assertDoesNotThrow(cloneBody::removeUninitializedParameters);
        assertDoesNotThrow(() -> cloneBody.setKeepIfEmpty(true));
        assertDoesNotThrow(() -> cloneBody.getProperties().add(null));
        assertDoesNotThrow(() -> cloneBody.getExamples().add(null));
        assertDoesNotThrow(() -> cloneBody.getEnumValues().add(null));

        ParameterArray photoUrls = (ParameterArray) body.getProperties().stream()
                .filter(p -> p.getName().equals(new ParameterName("photoUrls")))
                .findAny().get();
        assertThrows(EditReadOnlyOperationException.class, () -> photoUrls.addExample(new Object()));
        assertThrows(EditReadOnlyOperationException.class, photoUrls::removeUninitializedParameters);
        assertThrows(EditReadOnlyOperationException.class, () -> photoUrls.setKeepIfEmpty(true));
        assertThrows(EditReadOnlyOperationException.class, photoUrls::clearElements);
        assertThrows(EditReadOnlyOperationException.class, () -> photoUrls.addElement(null));
        assertThrows(UnsupportedOperationException.class, () -> photoUrls.getElements().add(null));
        assertThrows(UnsupportedOperationException.class, () -> photoUrls.getExamples().add(null));
        assertThrows(UnsupportedOperationException.class, () -> photoUrls.getEnumValues().add(null));
        // Not read-only: do not affect Parameter instance state
        assertDoesNotThrow(() -> body.getLeaves().add(null));
        // Test not read-only clone
        ParameterArray clonePhotoUrls = (ParameterArray) ((ParameterObject) o.deepClone().getRequestBody())
                .deepClone().getProperties().stream()
                .filter(p -> p.getName().equals(new ParameterName("photoUrls")))
                .findAny().get();
        assertDoesNotThrow(() -> clonePhotoUrls.addExample(new LinkedList<>()));
        assertDoesNotThrow(clonePhotoUrls::removeUninitializedParameters);
        assertDoesNotThrow(() -> clonePhotoUrls.setKeepIfEmpty(true));
        assertDoesNotThrow(clonePhotoUrls::clearElements);
        // TODO Check what is wrong
        //assertDoesNotThrow(() -> clonePhotoUrls.addElement(null));
        assertDoesNotThrow(() -> clonePhotoUrls.getElements().add(null));
        assertDoesNotThrow(() -> clonePhotoUrls.getExamples().add(null));
        assertDoesNotThrow(() -> clonePhotoUrls.getEnumValues().add(null));

    }
}
