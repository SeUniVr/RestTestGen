package io.resttestgen.implementation.parametervalueprovider.single;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class LlmParameterValueProvider extends CountableParameterValueProvider {

    private static final Logger logger = LogManager.getLogger(LlmParameterValueProvider.class);

    private LinkedTreeMap<String, LinkedTreeMap<String, ArrayList<Object>>> llmDictionary;

    private final Gson gson;
    private final boolean ENABLE_LLM = false; // Configure your LLM below to use it!!
    private final String SYSTEM_PROMPT = "You are a REST API testing assistant that has to generate example values for HTTP parameters and JSON properties in the body of requests. I am only interested in values. Each time I will tell you the method, the endpoint of a REST operation, the parameter name, and the parameter type (string, number, etc.), and you have to provide 25 example values for it. 20 of these values should be nominal values that have to be accepted as valid by the API. 5 of these values should be peculiar, e.g., corner cases, strange symbols, etc., but still in the requested format. I expect the response to be a JSON array in this format: [\"value1\", \"value2\", ...]. Do not use MD, only raw JSON.";
    private final boolean STORE_NEW_VALUES_TO_DICTIONARY_FILE = true;
    private final String DICTIONARY_PATH = Environment.getInstance().getApiUnderTest().getDir() + "/dictionaries/llm.json";

    public LlmParameterValueProvider() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Type type = new TypeToken<LinkedTreeMap<String, LinkedTreeMap<String, ArrayList<Object>>>>() {}.getType();
            JsonReader reader = new JsonReader(new FileReader(DICTIONARY_PATH));
            llmDictionary = gson.fromJson(reader, type);
        } catch (FileNotFoundException e) {
            llmDictionary = new LinkedTreeMap<>();
            logger.warn("LLM dictionary for this API not found. Creating empty dictionary.");
            try {
                Path path = Path.of(DICTIONARY_PATH);
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(Path.of(DICTIONARY_PATH), "{}", StandardCharsets.UTF_8);
            } catch (IOException ex) {
                logger.warn("Could not create empty LLM dictionary file.");
            }
        } catch (JsonSyntaxException e) {
            llmDictionary = new LinkedTreeMap<>();
            logger.warn("The provided LLM dictionary is not in the expected JSON format. Ignoring it.");
        } catch (ClassCastException e) {
            llmDictionary = new LinkedTreeMap<>();
            logger.warn("The schema of the provided LLM dictionary is not compliant. Dictionary will ignored and possibly overwritten.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Collection<Object> collectValuesFor(LeafParameter leafParameter) {

        // If parameter is not string or number, LLM is not used
        if (!(leafParameter instanceof StringParameter || leafParameter instanceof NumberParameter)) {
            return List.of();
        }

        // Get values from dictionary (user provided or cached)
        List<Object> llmDictionaryValues = getValuesFromLlmDictionary(leafParameter);

        // Return them only if at least 5. Otherwise, proceed with new generation with LLM
        if (llmDictionaryValues.size() > 5) {
            return llmDictionaryValues;
        }

        // Proceed with generation only if an LLM is available (configured and reachable)
        if (ENABLE_LLM) {

            // Ask LLM
            List<Object> llmValues = promptLlmForValues(leafParameter);

            // Store values in cache (i.e., in-memory dictionary)
            for (Object value : llmValues) {
                llmDictionary.computeIfAbsent(leafParameter.getOperation().toString(), k -> new LinkedTreeMap<>())
                        .computeIfAbsent(leafParameter.getName().toString(), k -> new ArrayList<>())
                        .add(value);
            }

            // Save in-memory dictionary to file
            if (STORE_NEW_VALUES_TO_DICTIONARY_FILE) {
                String json = gson.toJson(llmDictionary);
                try {
                    Files.writeString(Path.of(DICTIONARY_PATH), json, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    logger.warn("Could not write new values to LLM dictionary file.");
                }
            }
        }

        // Get updated values from dictionary after LLM filled it
        llmDictionaryValues = getValuesFromLlmDictionary(leafParameter);

        // Fallback to an empty list of values
        return llmDictionaryValues;
    }

    private List<Object> promptLlmForValues(LeafParameter leafParameter) {

        final String NEW_LINES = "\n\n";

        // Start building the prompt
        StringBuilder prompt = new StringBuilder()
                .append("Operation: ").append(leafParameter.getOperation()).append(NEW_LINES);

        // Add operation description, if available and longer than 5 chars
        if (leafParameter.getOperation().getDescription() != null && leafParameter.getOperation().getDescription().length() > 5) {
            prompt.append("Operation description: ").append(leafParameter.getOperation().getDescription()).append(NEW_LINES);
        }

        // Add parameter name
        prompt.append("Parameter name: ").append(leafParameter.getName()).append(NEW_LINES);

        // Add parameter type
        prompt.append("Parameter format: ").append(leafParameter.getType()).append(NEW_LINES);


        // Add parameter format, if available
        if (leafParameter.getFormat() != null && leafParameter.getFormat() != ParameterTypeFormat.MISSING && leafParameter.getFormat() != ParameterTypeFormat.UNKNOWN) {
            prompt.append("Parameter format: ").append(leafParameter.getFormat()).append(NEW_LINES);
        }

        // Add parameter description, if available and longer than 5 chars
        if (leafParameter.getDescription() != null && leafParameter.getDescription().length() > 5) {
            prompt.append("Parameter description: ").append(leafParameter.getDescription()).append(NEW_LINES);
        }

        List<Object> otherValues = new ArrayList<>(leafParameter.getExamples());
        otherValues.addAll(leafParameter.getEnumValues());
        if (leafParameter.getDefaultValue() != null) {
            otherValues.add(leafParameter.getDefaultValue());
        }

        // Add other example values, if any
        if (!otherValues.isEmpty()) {
            prompt.append("Other example values we already have (use them as an inspiration, do not return the same values): ").append(leafParameter.getExamples()).append(NEW_LINES);
        }

        // Prompt built!



        // Setting HTTP version to 1.1 for LM Studio compatibility
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1);
        JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder().httpClientBuilder(httpClientBuilder);

        ChatModel model = OpenAiChatModel.builder()
                .httpClientBuilder(jdkHttpClientBuilder)
                .timeout(Duration.ofMinutes(10))
                .baseUrl("http://localhost:1234/v1")
                .apiKey("demo")
                .modelName("qwen3-14b-mlx")
                .build();

        logger.info("Asking LLM for a parameter value for '{}' in operation {}.", leafParameter.getName(), leafParameter.getOperation());

        // Chat with the model
        String answer = model.chat(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(prompt.toString())
        ).aiMessage().text();

        // Remove reasoning (<think>) and trim answer
        String cleanAnswer = answer.replaceAll("(?s)<think>.*?</think>", "").trim();

        // Parse LLM response with Gson to fill list of values
        List<Object> llmValues = new ArrayList<>();
        try {
            JsonArray array = JsonParser.parseString(cleanAnswer).getAsJsonArray();
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    JsonPrimitive primitive = element.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        llmValues.add(primitive.getAsString());
                    } else if (primitive.isNumber()) {
                        llmValues.add(primitive.getAsNumber());
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            logger.warn("LLM returned a malformed JSON. Ignoring. Cleaned LLM answer was: {}", cleanAnswer);
        }

        return llmValues;
    }

    private List<Object> getValuesFromLlmDictionary(LeafParameter leafParameter) {

        String operation = leafParameter.getOperation().toString();
        String parameterName = leafParameter.getName().toString();

        if (llmDictionary != null) {
            if (llmDictionary.containsKey(operation)) {
                if (llmDictionary.get(operation).containsKey(parameterName)) {
                    List<Object> values = llmDictionary.get(operation).get(parameterName);
                    values = values.stream().filter(leafParameter::isValueCompliant).collect(Collectors.toList());
                    return values;
                }
            }
        }

        return List.of();
    }
}
