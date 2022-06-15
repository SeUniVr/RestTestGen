package io.resttestgen.core.datatype;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestHttpStatusCode {

    @Test
    public void testStatusCodeClasses() {
        HttpStatusCode informational = new HttpStatusCode(100);
        HttpStatusCode success = new HttpStatusCode(200);
        HttpStatusCode redirect = new HttpStatusCode(300);
        HttpStatusCode clientError = new HttpStatusCode(400);
        HttpStatusCode serverError = new HttpStatusCode(500);

        Assertions.assertTrue(informational.isInformational());
        Assertions.assertTrue(success.isSuccessful());
        Assertions.assertTrue(redirect.isRedirection());
        Assertions.assertTrue(clientError.isClientError());
        Assertions.assertTrue(serverError.isServerError());
    }

    @Test
    public void testEquals() {
        HttpStatusCode code0 = new HttpStatusCode(200);
        HttpStatusCode code1 = new HttpStatusCode(404);
        HttpStatusCode code2 = new HttpStatusCode(200);

        Assertions.assertEquals(code0, code2);
        Assertions.assertNotEquals(code0, code1);
    }

    @Test
    public void testWrongStatusCodes() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HttpStatusCode(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HttpStatusCode(999));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HttpStatusCode(-1));
    }
}
