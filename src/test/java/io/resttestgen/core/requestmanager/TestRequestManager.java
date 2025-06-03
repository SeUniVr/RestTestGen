package io.resttestgen.core.requestmanager;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.helper.RequestManager;
import io.resttestgen.core.openapi.Operation;
import okhttp3.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;

public class TestRequestManager {

    private static final Logger logger = LogManager.getLogger(TestRequestManager.class);

    private static Environment environment;

    @BeforeAll
    public static void setUp() throws IOException {
        environment = Starter.initEnvironment(ApiUnderTest.loadApiFromFile("petstore"));
    }

    // TODO: this is just a blueprint of the tests. Implement real tests.
    // FIXME: commented out rm.run() because method was removed from source.
    @Test
    public void TestBaseExecution() {
        logger.info("Test Requests creation");

        Operation findPetsByStatus = environment.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("findPetsByStatus"))
                .findFirst().get();
        logger.debug("Testing " + findPetsByStatus);
        RequestManager rm = new RequestManager(findPetsByStatus);
        StringParameter status = (StringParameter) ((ArrayParameter) rm.getOperation().getQueryParameters().stream().findAny().get()).getReferenceElement();
        status.setValueManually("available");
        logger.debug("Status: " + status.getValueAsFormattedString());
        try {
            //logger.debug(rm.run());
            rm.removeParameter(status);
            //logger.debug(rm.run());
            rm.setNullParameter(status);
            //logger.debug(rm.run());
            rm.addParameter(status);
            //logger.debug(rm.run());
            rm.removeParameter(status);
            rm.addParameter(new ArrayParameter(status));
            Request r = rm.buildRequest("MySpecialToken");
            //logger.debug(rm.run(r));
            StringParameter key = new StringParameter(status);
            Field name = Parameter.class.getDeclaredField("name");
            name.setAccessible(true);
            name.set(key, new ParameterName("api_key"));
            Field location = Parameter.class.getDeclaredField("location");
            location.setAccessible(true);
            location.set(key, ParameterLocation.HEADER);
            Field style = Parameter.class.getDeclaredField("style");
            style.setAccessible(true);
            style.set(key, ParameterStyle.SIMPLE);
            key.setValueManually("AnotherSpecialToken");
            rm.addParameter(key);
            r = rm.buildRequestAsFuzzed();
            //logger.debug(rm.run(r));
            r = rm.buildRequestDroppingAuth();
            //logger.debug(rm.run(r));
        } catch (/*IOException | */NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
        status.setValueManually("not encoded value?!");
        logger.debug("Status: " + status.getValueAsFormattedString());
        // TODO: check the following
        //assertEquals("not+encoded+value%3F%21", status.getValueAsFormattedString(ParameterStyle.SIMPLE));
        /*try {
            //logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/

        Operation findByTags = environment.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("findPetsByTags"))
                .findFirst().get();
        logger.debug("Testing " + findByTags);
        rm = new RequestManager(findByTags);
        ArrayParameter tags = (ArrayParameter) rm.getOperation().getQueryParameters().stream().findAny().get();
        for (int i = 1; i < 3; ++i) {
            StringParameter tag = new StringParameter((LeafParameter) tags.getReferenceElement());
            tag.setValueManually("tag" + i);
            tags.addElement(tag);
        }
        /*try {
            //logger.debug(rm.run());
            rm.setNullParameter(tags);
            //logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/

        Operation petId = environment.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("getPetById"))
                .findFirst().get();
        logger.debug("Testing " + petId);
        rm = new RequestManager(petId);
        NumberParameter parameterPetId = (NumberParameter) rm.getOperation().getPathParameters().stream().findAny().get();
        parameterPetId.setValueManually(10);
        /*try {
            //logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/

        /*Operation user = environment.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("createUser"))
                .findFirst().get();
        logger.debug("Testing " + user);
        rm = new RequestManager(user);
        user = rm.getOperation();
        ObjectParameter userBody = (ObjectParameter) rm.getOperation().getRequestBody();
        ((LeafParameter) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("id"))).findFirst().get())
                .setValue(999);
        ((LeafParameter) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("username"))).findFirst().get())
                .setValue("MyTestUsername");
        ((LeafParameter) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("firstName"))).findFirst().get())
                .setValue("Amedeo");
        ((LeafParameter) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("lastName"))).findFirst().get())
                .setValue("Zampieri");
        ((LeafParameter) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("email"))).findFirst().get())
                .setValue("amedeo.zampieri@univr.it");
        ((LeafParameter) userBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("password"))).findFirst().get())
                .setValue("secret!!");

        ObjectParameter expandedUserBody = userBody.deepClone();*/
        /*try {
            //logger.debug(rm.run());
            rm.setNullParameter(userBody);
            //logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/
        /*((LeafParameter) expandedUserBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("phone"))).findFirst().get())
                .setValue("3333333333");
        ((LeafParameter) expandedUserBody.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("userStatus"))).findFirst().get())
                .setValue(1);
        user.setRequestBody(expandedUserBody);
        logger.debug("Expanded: \n" + expandedUserBody.getJSONString());
        logger.debug("Operation body: \n" + user.getRequestBody().getJSONString());
        /*try {
            //logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/

        /*Operation createWithList = environment.getOpenAPI().getOperations().stream().filter(o -> o.getOperationId().equals("createUsersWithListInput"))
                .findFirst().get();
        logger.debug("Testing " + createWithList);
        rm = new RequestManager(createWithList);
        ArrayParameter listBody = (ArrayParameter) rm.getOperation().getRequestBody();
        for (int i = 1; i < 3; ++i) {
            ObjectParameter newUser = (ObjectParameter) listBody.getReferenceElement().deepClone();
            ((LeafParameter) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("id"))).findFirst().get())
                    .setValue(99 + i);
            ((LeafParameter) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("username"))).findFirst().get())
                    .setValue("MyTestUsername" + i);
            ((LeafParameter) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("firstName"))).findFirst().get())
                    .setValue("Amedeo" + i);
            ((LeafParameter) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("lastName"))).findFirst().get())
                    .setValue("Zampieri" + i);
            ((LeafParameter) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("email"))).findFirst().get())
                    .setValue("amedeo.zampieri@univr.it" + i);
            ((LeafParameter) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("password"))).findFirst().get())
                    .setValue("secret!!" + i);
            ((LeafParameter) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("phone"))).findFirst().get())
                    .setValue("33333333" + i);
            ((LeafParameter) newUser.getProperties().stream().filter(p -> p.getName().equals(new ParameterName("userStatus"))).findFirst().get())
                    .setValue(1);
            listBody.addElement(newUser);
        }
        /*try {
            //logger.debug(rm.run());
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/
    }
}
