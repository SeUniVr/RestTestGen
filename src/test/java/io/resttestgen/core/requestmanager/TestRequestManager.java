package io.resttestgen.core.requestmanager;

import io.resttestgen.core.Environment;
import io.resttestgen.core.TestingEnvironmentGenerator;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.helper.RequestManager;
import io.resttestgen.core.openapi.Operation;
import okhttp3.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRequestManager {

    private static final Logger logger = LogManager.getLogger(TestRequestManager.class);

    private static Environment e;

    @BeforeAll
    public void setUp() {
        e = TestingEnvironmentGenerator.getTestingEnvironment("build/resources/test/specifications/medium_petstore.json");
    }

    // TODO: this is just a blueprint of the tests. Implement real tests.
    @Test
    public static void TestBaseExecution() {
        logger.info("Test Requests creation");

        Operation findPetsByStatus = e.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("findPetsByStatus"))
                .findFirst().get();
        logger.debug("Testing " + findPetsByStatus);
        RequestManager rm = new RequestManager(findPetsByStatus);
        StringParameter status = (StringParameter) rm.getOperation().getQueryParameters().stream().findAny().get();
        status.setValue("available");
        logger.debug("Status: " + status.getValueAsFormattedString());
        try {
            logger.debug(rm.run());
            rm.removeParameter(status);
            logger.debug(rm.run());
            rm.setNullParameter(status);
            logger.debug(rm.run());
            rm.addParameter(status);
            logger.debug(rm.run());
            rm.removeParameter(status);
            rm.addParameter(new ParameterArray(status));
            Request r = rm.buildRequest("MySpecialToken");
            logger.debug(rm.run(r));
            StringParameter key = new StringParameter(status);
            Field name = ParameterElement.class.getDeclaredField("name");
            name.setAccessible(true);
            name.set(key, new ParameterName("api_key"));
            Field location = ParameterElement.class.getDeclaredField("location");
            location.setAccessible(true);
            location.set(key, ParameterLocation.HEADER);
            Field style = ParameterElement.class.getDeclaredField("style");
            style.setAccessible(true);
            style.set(key, ParameterStyle.SIMPLE);
            key.setValue("AnotherSpecialToken");
            rm.addParameter(key);
            r = rm.buildRequestAsFuzzed();
            logger.debug(rm.run(r));
            r = rm.buildRequestDroppingAuth();
            logger.debug(rm.run(r));
        } catch (IOException | NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
        status.setValue("not encoded value?!");
        logger.debug("Status: " + status.getValueAsFormattedString());
        assertEquals("not+encoded+value%3F%21", status.getValueAsFormattedString(ParameterStyle.SIMPLE));
        try {
            logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Operation findByTags = e.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("findPetsByTags"))
                .findFirst().get();
        logger.debug("Testing " + findByTags);
        rm = new RequestManager(findByTags);
        ParameterArray tags = (ParameterArray) rm.getOperation().getQueryParameters().stream().findAny().get();
        for (int i = 1; i < 3; ++i) {
            StringParameter tag = new StringParameter((ParameterLeaf) tags.getReferenceElement());
            tag.setValue("tag" + i);
            tags.addElement(tag);
        }
        try {
            logger.debug(rm.run());
            rm.setNullParameter(tags);
            logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Operation petId = e.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("getPetById"))
                .findFirst().get();
        logger.debug("Testing " + petId);
        rm = new RequestManager(petId);
        NumberParameter parameterPetId = (NumberParameter) rm.getOperation().getPathParameters().stream().findAny().get();
        parameterPetId.setValue(10);
        try {
            logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Operation user = e.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("createUser"))
                .findFirst().get();
        logger.debug("Testing " + user);
        rm = new RequestManager(user);
        user = rm.getOperation();
        ParameterObject userBody = (ParameterObject) rm.getOperation().getRequestBody();
        ((ParameterLeaf) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("id"))).findFirst().get())
                .setValue(999);
        ((ParameterLeaf) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("username"))).findFirst().get())
                .setValue("MyTestUsername");
        ((ParameterLeaf) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("firstName"))).findFirst().get())
                .setValue("Amedeo");
        ((ParameterLeaf) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("lastName"))).findFirst().get())
                .setValue("Zampieri");
        ((ParameterLeaf) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("email"))).findFirst().get())
                .setValue("amedeo.zampieri@univr.it");
        ((ParameterLeaf) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("password"))).findFirst().get())
                .setValue("secret!!");

        ParameterObject expandedUserBody = userBody.deepClone();
        try {
            logger.debug(rm.run());
            rm.setNullParameter(userBody);
            logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        ((ParameterLeaf) expandedUserBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("phone"))).findFirst().get())
                .setValue("3333333333");
        ((ParameterLeaf) expandedUserBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("userStatus"))).findFirst().get())
                .setValue(1);
        user.setRequestBody(expandedUserBody);
        logger.debug("Expanded: \n" + expandedUserBody.getJSONString());
        logger.debug("Operation body: \n" + user.getRequestBody().getJSONString());
        try {
            logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Operation createWithList = e.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("createUsersWithListInput"))
                .findFirst().get();
        logger.debug("Testing " + createWithList);
        rm = new RequestManager(createWithList);
        ParameterArray listBody = (ParameterArray) rm.getOperation().getRequestBody();
        for (int i = 1; i < 3; ++i) {
            ParameterObject newUser = (ParameterObject) listBody.getReferenceElement().deepClone();
            ((ParameterLeaf) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("id"))).findFirst().get())
                    .setValue(99 + i);
            ((ParameterLeaf) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("username"))).findFirst().get())
                    .setValue("MyTestUsername" + i);
            ((ParameterLeaf) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("firstName"))).findFirst().get())
                    .setValue("Amedeo" + i);
            ((ParameterLeaf) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("lastName"))).findFirst().get())
                    .setValue("Zampieri" + i);
            ((ParameterLeaf) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("email"))).findFirst().get())
                    .setValue("amedeo.zampieri@univr.it" + i);
            ((ParameterLeaf) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("password"))).findFirst().get())
                    .setValue("secret!!" + i);
            ((ParameterLeaf) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("phone"))).findFirst().get())
                    .setValue("33333333" + i);
            ((ParameterLeaf) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("userStatus"))).findFirst().get())
                    .setValue(1);
            listBody.addElement(newUser);
        }
        try {
            logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
