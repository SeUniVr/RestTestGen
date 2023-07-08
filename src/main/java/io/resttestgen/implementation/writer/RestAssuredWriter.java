package io.resttestgen.implementation.writer;

import io.resttestgen.boot.AuthenticationInfo;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.Writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RestAssuredWriter extends Writer {

    // TODO : support Long Value with 'L'
    private static final AtomicInteger nextSequenceId = new AtomicInteger(1);
    private final int sequenceId = nextSequenceId.getAndIncrement();
    private final List<Operation> allOperation = new LinkedList<>();
    private int numberOfInteraction;
    private final Environment environment = Environment.getInstance();

    public RestAssuredWriter(TestSequence testSequence) {
        super(testSequence);
        this.numberOfInteraction=0;
    }


    @Override
    public String getOutputFormatName() {
        return "REST-assured";
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void write() throws IOException {


        StringBuilder path = new StringBuilder();
        path.append(getOutputPath());
        File file = new File(path.toString());
        file.mkdirs();

        FileWriter writer = new FileWriter(path + getSuggestedFileName("java").replaceAll("-","_"));


        //write imports
        String content = generateImport() +
                //write tests classes
                generateClass() +
                //write mainClass
                generateMainTestMethod() +
                "}\n";
        writer.write(content);


        // Close the writer
        writer.close();
    }


    private String generateImport(){
        String imports = "package " + testSequence.getGenerator()+ ";\n\n";

        //utils imports
        imports +="import static io.restassured.RestAssured.*;\n"+
                "import com.jayway.jsonpath.JsonPath;\n"+
                "import io.restassured.RestAssured;\n"+
                "import io.restassured.specification.RequestSpecification;\n"+
                "import static io.restassured.matcher.RestAssuredMatchers.*;\n"+
                "import static org.hamcrest.Matchers.*;\n"+
                "import io.restassured.http.ContentType;\n"+
                "import io.restassured.response.Response;\n"+
                "import io.restassured.common.mapper.TypeRef;\n"+
                "import java.util.List;\n"+
                "import java.util.Map;\n"+
                "import java.util.HashMap;\n"+
                "import org.json.*;\n"+
                "import org.junit.jupiter.api.*;\n"+
                "//import org.junit.runners.*;\n\n";
        return imports;
    }

    private String generateClass(){
        String s = "//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)\n";
        s += "@Order("+sequenceId+")\n";
        s += "public class " + testSequence.getName().replaceAll("-","_") +"{\n\n";
        return s;
    }

    private String generateMainTestMethod(){
        StringBuilder content = new StringBuilder();

        // Set baseURL from OpenAPI
        content.append("String baseURL =\"").append(environment.getOpenAPI().getServers().get(0)).append("\";\n\n");


        // TODO: only on the last testInteraction or on all testInteractions ?

        // Generate method for all TestInteractions in the sequence
        for (TestInteraction testInteraction : testSequence) {
            content.append(generateTestMethod(testInteraction));
            this.numberOfInteraction++;
        }

        // Write main class Test
        content.append("\t@Test\n");
        content.append("\tpublic void test_").append(testSequence.getName().replaceAll("-","_")).append("()  throws JSONException{\n");


        for( int i = 0; i < numberOfInteraction; i++){
            content.append("\t\ttest").append(i).append("();\n");
        }

        content.append("\t}\n");
        return content.toString();
    }

    // One method for each testInteraction
    private String generateTestMethod(TestInteraction testInteraction){

        StringBuilder content = new StringBuilder();

        // Take operation
        Operation operation = testInteraction.getFuzzedOperation();
        // initialization of operations: take the first operation and generate recursively the operation for build the request
        operationsInitialization(operation);

        // Test
        content.append("\tprivate void test").append(this.numberOfInteraction).append("() throws JSONException{\n");
        // Write method for get parameters

        writeOperation(content);
        content.append("\t\t//OPERATION 0\n");
        parametersInitialization(operation,content,0);
        if(Objects.equals(testSequence.getGenerator(), "ErrorFuzzer")){
            content.append("\t\tAssertions.assertFalse(response0.getStatusCode()<=299,\"StatusCode 2xx: The test sequence was not executed successfully.\");\n");
        }
        content.append("\t\tAssertions.assertFalse(response0.getStatusCode()>=500,\"StatusCode 5xx: The test sequence was not executed successfully.\");\n");
        //content.append("result0.getStatusCode().assertThat().statusCode(").append(operation.getOutputParameters().get("code")).append(");");
        content.append("\t}\n");
        return content.toString();
    }


    private void structuredParameterInitialization(StringBuilder content, Parameter parameter, String parentName){
        if(parameter instanceof ArrayParameter){
            content.append("\t\tJSONArray ");
            content.append(parentName).append(" = new JSONArray();\n");
            int count = 0;
            List<Parameter> childrenParameters = ((ArrayParameter) parameter).getElements();
            for(Parameter childParameter : childrenParameters) {
                if (childParameter.getParent() == parameter) {
                    String childName = parentName + "_" + childParameter.getName() + count;
                    if (childParameter instanceof StructuredParameter) {
                        structuredParameterInitialization(content, childParameter, childName);
                    }
                    if (childParameter instanceof LeafParameter) {
                        parameterLeafInitialization(content, childParameter, childName);
                    }
                    content.append("\t\t").append(parentName).append(".put(").append(childName).append(");\n");
                    count++;
                }
            }
        }
        if(parameter instanceof ObjectParameter){
            content.append("\t\tJSONObject ");
            content.append(parentName).append(" = new JSONObject();\n");
            List<Parameter> childrenParameters =((ObjectParameter) parameter).getProperties();
            for(Parameter childParameter : childrenParameters){
                String childName =parentName+"_"+childParameter.getName();
                    if(childParameter instanceof StructuredParameter){
                        structuredParameterInitialization(content, childParameter, childName);
                    }
                    if(childParameter instanceof LeafParameter){
                        parameterLeafInitialization(content,childParameter, childName);
                    }
                    content.append("\t\t").append(parentName).append(".put(\"").append(childParameter.getName()).append("\" , ").append(childName).append(");\n");
            }
        }
    }

    private void parameterLeafInitialization(StringBuilder content, Parameter parameter, String parentName){

        content.append("\t\tObject ").append(parentName).append(" = ");
        if(parameter.getValue() instanceof LeafParameter && !parameter.getTags().contains("mutated")) {
            LeafParameter parameterRef= (LeafParameter) parameter.getValue();
            //check if location of parameterLeaf is not RESPONSE_BODY
            if(parameterRef.getLocation()!= ParameterLocation.RESPONSE_BODY){
                content.append(buildVariableName("request",allOperation.indexOf(parameterRef.getOperation()) + 1,parameterRef.getLocation().toString(),parameterRef.getName()));
            }
            else{
                content.append("JsonPath.read(");
                content.append(buildVariableName("response",allOperation.indexOf(parameterRef.getOperation()) + 1,parameterRef.getLocation().toString(),null));
                content.append(" , \"");
                content.append(parameterRef.getJsonPath());
                content.append("\")");
            }
            content.append(";\n");
        }else {
            if(parameter.getValue()!=null){
                if(parameter.getValue() instanceof String){
                    content.append("\"").append(parameter.getValueAsFormattedString(ParameterStyle.SIMPLE)).append("\";\n");
                }else{
                    content.append(parameter.getValueAsFormattedString(ParameterStyle.SIMPLE)).append(";\n");
                }
            }else{
                content.append("null").append(";\n");
            }
        }
    }

    private void parametersInitialization(Operation operation, StringBuilder content, int numOperation){
        Set<Parameter> parameters = (Set<Parameter>) operation.getAllRequestParameters();

        if (parameters.size() > 0) {
            content.append("\t\t//Parameter initialization\n");
        }
        for(Parameter parameter : parameters){
            if(parameter.getParent()==null){
                if(parameter instanceof StructuredParameter){
                    structuredParameterInitialization(content,parameter,buildVariableName("request",numOperation,parameter.getLocation().toString(),parameter.getName()));
                }
                if(parameter instanceof LeafParameter){
                    parameterLeafInitialization(content,parameter, buildVariableName("request",numOperation,parameter.getLocation().toString(),parameter.getName()));
                }
            }
        }

        //write the Rest Assured operation for Response
        buildRequest(content,operation,numOperation);

    }


    private void buildRequest(StringBuilder content,Operation operation,int numOperation ){
        content.append("\t\t//Build request\n ");
        content.append("\t\tRequestSpecification ").append(buildVariableName("request",numOperation,null,null)).append(" = RestAssured.given()");
        AuthenticationInfo authInfo = environment.getApiUnderTest().getDefaultAuthenticationInfo();
        if(authInfo != null){
            content.append(".header(\"Authorization\",\"").append(authInfo.getValue()).append("\")");
        }
        content.append(";\n");
        for(Parameter parameter: operation.getLeaves()){
            if(parameter.getLocation()!= ParameterLocation.REQUEST_BODY){
                content.append("\t\t").append(buildVariableName("request",numOperation,null,null));
                switch (parameter.getLocation()){
                    case PATH:
                        content.append(".pathParam(\"").append(parameter.getName()).append("\" , ").append(buildVariableName("request",numOperation,"path",parameter.getName())).append(");\n");
                        break;
                    case HEADER:
                        content.append(".header(\"").append(parameter.getName()).append("\" , ").append(buildVariableName("request",numOperation,"header",parameter.getName())).append(");\n");
                        break;
                    case QUERY:
                        content.append(".queryParam(\"").append(parameter.getName()).append("\" , ").append(buildVariableName("request",numOperation,"query",parameter.getName())).append(");\n");
                        break;
                    case COOKIE:
                        content.append(".cookie(\"").append(parameter.getName()).append("\" , ").append(buildVariableName("request",numOperation,"cookie",parameter.getName())).append(");\n");
                        break;
                }
            }
        }
        if(operation.getRequestBody()!=null && !operation.getRequestBody().isEmpty()){
            content.append("\t\t").append(buildVariableName("request",numOperation,null,null)).append(".contentType(ContentType.JSON).body(").append(buildVariableName("request",numOperation,"request_body",null)).append(".toString());\n");
        }
        content.append("\t\t//Build Response\n");
        content.append("\t\tResponse ").append(buildVariableName("response",numOperation,null,null)).append(" = ").append(buildVariableName("request",numOperation,null,null));
        content.append(".when().").append(operation.getMethod().toString().toLowerCase()).append("(baseURL+\"").append(operation.getEndpoint()).append("\");\n");

        //extract body as String
        if(operation.getResponseBody()!=null && !operation.getResponseBody().isEmpty()){
            content.append("\t\tString ");
            content.append(buildVariableName("response",numOperation,"response_body",null));
            content.append(" = ");
            content.append(buildVariableName("response",numOperation,null,null)).append(".getBody().asString();\n\n");
        }

    }

    private void operationsInitialization(Operation operation) {
        //for each parameter check if it is a ParameterLeaf
        Collection<LeafParameter> allParameters = operation.getLeaves();
        for(LeafParameter p:allParameters){
            if(p.getValue() instanceof LeafParameter){
                Operation newOperation = ((LeafParameter) p.getValue()).getOperation();
                //this avoids the replication of operations
                allOperation.remove(newOperation);
                allOperation.add(newOperation);
                operationsInitialization(newOperation);
            }
        }
    }

    private void writeOperation(StringBuilder content) {
        //write operation from the last one
        for(int count = allOperation.size();count>0;count--){
            content.append("\t\t//OPERATION ").append(count).append("\n");
            parametersInitialization(allOperation.get(count-1),content,count);
            content.append("\t\tAssertions.assertTrue(response").append(count).append(".getStatusCode()<=299,\"StatusCode not 2xx for previous operation.\");\n");
        }
    }

    private String buildVariableName(String operation, int numOperation, String location, ParameterName parameterName){
        if(location!=null){
            if(parameterName!=null){
                if(!Objects.equals(parameterName.toString(), "")){
                    return operation+numOperation+"_"+location.toLowerCase()+"_"+parameterName;
                }else{
                    return operation+numOperation+"_"+location.toLowerCase();
                }
            }
            return operation+numOperation+"_"+location.toLowerCase();
        }
        return operation+numOperation;
    }

}

