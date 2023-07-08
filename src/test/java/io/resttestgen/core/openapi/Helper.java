package io.resttestgen.core.openapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.resttestgen.core.datatype.NormalizedParameterName;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Helper {

    public static Map<String, Object> getJSONMap (String pathname) throws IOException {
        Gson gson = new Gson();
        File operationJSON = new File(pathname);
        Reader reader = Files.newBufferedReader(Paths.get(operationJSON.getAbsolutePath()));
        Map<String, Object> operationMap = gson.fromJson(reader, Map.class);

        return operationMap;
    }

    public static Map<String, Object> getParserMap(OpenApiParser parser) throws NoSuchFieldException, IllegalAccessException {
        Field openAPIMapField = OpenApiParser.class.getDeclaredField("openAPIMap");
        openAPIMapField.setAccessible(true);
        return (Map<String, Object>) openAPIMapField.get(parser);
    }

    public static void invokeParserMethod(OpenApiParser parser, String methodName) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Method method = OpenApiParser.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(parser);
    }

    public static void writeJSON(String filename, Object jsonObj) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Writer writer = new FileWriter(filename);
        gson.toJson(jsonObj, writer);
        writer.flush();
        writer.close();
    }

    public static void setNormalizer() {
        List<String> disNames = new ArrayList<>();
        disNames.add("id");
        disNames.add("name");
        NormalizedParameterName.setQualifiableNames(disNames);
    }
}
