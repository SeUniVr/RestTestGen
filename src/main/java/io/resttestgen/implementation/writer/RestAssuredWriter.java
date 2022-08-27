package io.resttestgen.implementation.writer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.Writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RestAssuredWriter extends Writer {

    private final List<Operation> allOperation = new LinkedList<>();
    private int num;
    private int numSequence;
    public RestAssuredWriter(TestSequence testSequence) {
        super(testSequence);
        this.num=0;
    }


    @Override
    public String getOutputFormatName() {
        return "REST-assured";
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void write() throws IOException {

        String path;
        if(Objects.equals(configuration.getProjectDirectoryRoot(), configuration.getOutputPath())){
            path = getOutputPath();
        }else{
            path = configuration.getProjectDirectoryRoot();
            path+= "/src/test/java/";
            if(configuration.getPackageName()!=null){
                path+=configuration.getPackageName().replaceAll("\\.","/") + "/" ;
            }
            path+=testSequence.getGenerator()+"/";
        }
        File file = new File(path);
        file.mkdirs();

        FileWriter writer = new FileWriter(path + getSuggestedFileName("java"));


        String content = "";
        //write imports
        content += generateImport();
        //write tests classes
        content += generateClass();
        //write mainClass
        content += generateMainTestMethod();


        content += "}\n";
        writer.write(content);


        // Close the writer
        writer.close();
    }


    private String generateImport(){
        String imports = "";

        // Check for package name;
        imports += "package ";
        if(configuration.getPackageName()==null){
            imports += testSequence.getGenerator()+ ";\n\n";
        }else{
            imports += configuration.getPackageName()+"."+testSequence.getGenerator()+ ";\n\n";
        }

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
                "import org.junit.jupiter.api.*;\n"+ "//import org.junit.runners.*;\n\n";

        return imports;
    }

    private String generateClass(){
        String s = "//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)\n";
        s += "@Order("+numSequence+")\n";
        s += "public class " + testSequence.getName().replaceAll("-","_") +"{\n\n";
        return s;
    }

    private String generateMainTestMethod(){
        StringBuilder content = new StringBuilder();
        Environment environment = Environment.getInstance();

        //set baseURL from OpenAPI
        content.append("String baseURL =\"").append(environment.getOpenAPI().getServers().get(0)).append("\";\n\n");

        // for all Test interaction generate method
        for (TestInteraction testInteraction : testSequence) {
            content.append(generateTestMethod(testInteraction));
            this.num++;
        }

        //write main class Test
        content.append("\t@Test\n");
        content.append("\tpublic void test_").append(testSequence.getName().replaceAll("-","_")).append("()  throws JSONException{\n");


        for(int i=0; i<num;i++){
            content.append("\t\ttest").append(i).append("();\n");
        }

        content.append("\t}\n");
        return content.toString();
    }

    //One method for each testInteraction
    private String generateTestMethod(TestInteraction testInteraction){

        StringBuilder content = new StringBuilder();

        // take operation
        Operation operation = testInteraction.getOperation();
        //initialization of operations: take the first operation and generate recursively the operation for build the request
        operationsInitialization(operation);

        //Test
        content.append("\tprivate void test").append(this.num).append("() throws JSONException{\n");
        //write method for get parameters

        writeOperation(content);
        content.append("\t\t//OPERATION 0\n");
        parametersInitialization(operation,content,0);
        content.append("\t\tAssertions.assertFalse(response0.getStatusCode()>=500,\"The test sequence was not executed successfully.\");\n");
        //content.append("result0.getStatusCode().assertThat().statusCode(").append(operation.getOutputParameters().get("code")).append(");");
        content.append("\t}\n");
        return content.toString();
    }



    private void parametersInitialization(Operation operation, StringBuilder content, int numOperation){

        //take all leaves
        Collection<ParameterLeaf> allParameters = operation.getLeaves();
        if (allParameters.size() > 0) {
            content.append("\t\t//Parameter initialization\n");
        }
        for(ParameterLeaf parameterLeaf: allParameters) {
            if(parameterLeaf.getValue() instanceof ParameterLeaf) {
                ParameterLeaf parameterLeafParent = (ParameterLeaf) parameterLeaf.getValue();
                content.append("\t\tString ");
                if(parameterLeaf.getLocation()==ParameterLocation.REQUEST_BODY || parameterLeaf.getLocation()==ParameterLocation.RESPONSE_BODY){
                    content.append(buildVariableName("request",numOperation,"body",parameterLeaf.getName()));
                }else{
                    content.append(buildVariableName("request",numOperation,parameterLeaf.getLocation().toString(),parameterLeaf.getName()));
                }
                content.append(" = ");

                //check if location of parameterLeaf is not RESPONSE_BODY
                if(parameterLeafParent.getLocation()!=ParameterLocation.RESPONSE_BODY){
                    if(parameterLeafParent.getLocation()!=ParameterLocation.REQUEST_BODY){
                        content.append(buildVariableName("request",allOperation.indexOf(parameterLeafParent.getOperation()) + 1,parameterLeafParent.getLocation().toString(),parameterLeafParent.getName()));
                    }else{
                        content.append(buildVariableName("request",allOperation.indexOf(parameterLeafParent.getOperation()) + 1,"body",parameterLeafParent.getName()));
                    }
                }
                else{
                    content.append("JsonPath.read(");
                    content.append(buildVariableName("response",allOperation.indexOf(parameterLeafParent.getOperation()) + 1,"body",null));
                    content.append(" , \"");
                    content.append(parameterLeafParent.getJsonPath());
                    content.append("\").toString()");
                }
                content.append(";\n");
            }else {
                content.append("\t\tString ");
                if(parameterLeaf.getLocation()==ParameterLocation.REQUEST_BODY || parameterLeaf.getLocation()==ParameterLocation.RESPONSE_BODY){
                    content.append(buildVariableName("request",numOperation,"body",parameterLeaf.getName()));
                }else{
                    content.append(buildVariableName("request",numOperation,parameterLeaf.getLocation().toString(),parameterLeaf.getName()));
                }
                content.append(" = \"").append(parameterLeaf.getValueAsFormattedString(ParameterStyle.SIMPLE)).append("\";\n");
            }
        }

        //if request needs body
        if(operation.getRequestBody()!=null && !operation.getRequestBody().isEmpty()){
            content.append("\t\t//build bodyRequest\n");
            content.append("\t\tJSONObject ");
            content.append(buildVariableName("request",numOperation,"body",null));
            content.append(" = new JSONObject();\n ");
            for(ParameterLeaf parameterLeaf: allParameters){
                if(parameterLeaf.getLocation()==ParameterLocation.REQUEST_BODY){
                    content.append("\t\t");
                    content.append(buildVariableName("request",numOperation,"body",null));
                    content.append(".put(\"").append(parameterLeaf.getName()).append("\" , ");
                    content.append(buildVariableName("request",numOperation,"body",parameterLeaf.getName()));
                    content.append(");\n");
                }
            }
        }

        //write the Rest Assured operation for Response
        buildRequest(content,operation,numOperation);

    }


    private void buildRequest(StringBuilder content,Operation operation,int numOperation ){
        content.append("\t\t//Build request\n ");
        content.append("\t\tRequestSpecification ").append(buildVariableName("request",numOperation,null,null)).append(" = RestAssured.given();\n");
        for(ParameterElement parameter: operation.getLeaves()){
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
            content.append("\t\t").append(buildVariableName("request",numOperation,null,null)).append(".contentType(ContentType.JSON).body(").append(buildVariableName("request",numOperation,"body",null)).append(".toString());\n");
        }
        content.append("\t\t//Build Response\n");
        content.append("\t\tResponse ").append(buildVariableName("response",numOperation,null,null)).append(" = ").append(buildVariableName("request",numOperation,null,null));
        content.append(".when().").append(operation.getMethod().toString().toLowerCase()).append("(baseURL+\"").append(operation.getEndpoint()).append("\");\n");

        //extract body as String
        if(operation.getResponseBody()!=null && !operation.getResponseBody().isEmpty()){
            content.append("\t\tString ");
            content.append(buildVariableName("response",numOperation,"body",null));
            content.append(" = ");
            content.append(buildVariableName("response",numOperation,null,null)).append(".getBody().asString();\n\n");
        }

    }

    private void operationsInitialization(Operation operation) {
        //for each parameter check if it is a ParameterLeaf
        Collection<ParameterLeaf> allParameters = operation.getLeaves();
        for(ParameterLeaf p:allParameters){
            if(p.getValue() instanceof ParameterLeaf){
                Operation newOperation = ((ParameterLeaf) p.getValue()).getOperation();
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
        }
    }

    private String buildVariableName(String operation, int numOperation, String location, ParameterName parameterName){
        if(location!=null){
            if(parameterName!=null){
                return operation+numOperation+"_"+location.toLowerCase()+"_"+parameterName;
            }
            return operation+numOperation+"_"+location.toLowerCase();
        }
        return operation+numOperation;
    }

    public void setNumSequence(int numSequence){
        this.numSequence=numSequence;
    }
}

