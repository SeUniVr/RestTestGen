package io.resttestgen.core.helper;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HTTPStatusCode;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.dictionary.DictionaryEntry;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.operationdependencygraph.OperationDependencyGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;

public class ResponseAnalyzer {

    private static final Logger logger = LogManager.getLogger(ResponseAnalyzer.class);

    private boolean responseUpdateGraph = true;
    private boolean responseDataIsAddedToDictionary = true;
    private final OperationDependencyGraph graph = Environment.getInstance().getOperationDependencyGraph();
    private final Dictionary globalDictionary = Environment.getInstance().getGlobalDictionary();
    private Dictionary localDictionary;

    public boolean doesResponseUpdateGraph() {
        return responseUpdateGraph;
    }

    public void setResponseUpdateGraph(boolean responseUpdateGraph) {
        this.responseUpdateGraph = responseUpdateGraph;
    }

    public boolean isResponseDataAddedToDictionary() {
        return responseDataIsAddedToDictionary;
    }

    public void setResponseDataIsAddedToDictionary(boolean responseDataIsAddedToDictionary) {
        this.responseDataIsAddedToDictionary = responseDataIsAddedToDictionary;
    }

    /**
     * Process the response in order to update the ODG and the response dictionary.
     * @param responseStatusCode the obtained status code.
     * @param responseBody the body of the obtained response.
     */
    public void analyzeResponse(Operation operation, HTTPStatusCode responseStatusCode, String responseBody) {

        if (responseStatusCode.isSuccessful() && responseUpdateGraph) {
            graph.setOperationAsTested(operation);
        }

        parseJsonBody(responseBody);
    }


    private void parseJsonBody(String responseBody) {
        StringBuilder jsonPath = new StringBuilder();
        Gson gson = new Gson();
        try {
            Object parsedJSON = gson.fromJson(responseBody, Object.class);

            LinkedList<Object> queue = new LinkedList<>();
            queue.add(parsedJSON);

            while (!queue.isEmpty()) {
                Object o = queue.getFirst();

                if (o instanceof LinkedTreeMap) {
                    Set<String> keys = (Set<String>) ((LinkedTreeMap<?, ?>) o).keySet();
                    for (String key : keys) {

                        Object val = ((LinkedTreeMap<?, ?>) o).get(key);
                        if (val instanceof String) {
                            DictionaryEntry entry = new DictionaryEntry(new ParameterName(key), null, (String) val);
                            globalDictionary.addEntry(entry);
                            if (localDictionary != null) {
                                localDictionary.addEntry(entry);
                            }
                        } else if (val instanceof Double) {
                            if (((Double) val % 1) == 0) {
                                DictionaryEntry entry = new DictionaryEntry(new ParameterName(key), null, ((Double) val).intValue());
                                globalDictionary.addEntry(entry);
                                if (localDictionary != null) {
                                    localDictionary.addEntry(entry);
                                }
                            } else {
                                DictionaryEntry entry = new DictionaryEntry(new ParameterName(key), null, (Double) val);
                                globalDictionary.addEntry(entry);
                                if (localDictionary != null) {
                                    localDictionary.addEntry(entry);
                                }
                            }
                        } else if (val instanceof Boolean) {
                            DictionaryEntry entry = new DictionaryEntry(new ParameterName(key), null, (Boolean) val);
                            globalDictionary.addEntry(entry);
                            if (localDictionary != null) {
                                localDictionary.addEntry(entry);
                            }
                        } else if (val instanceof ArrayList) {
                            for (Object arrayElement : (ArrayList) val) {
                                if (arrayElement instanceof String) {
                                    DictionaryEntry entry = new DictionaryEntry(new ParameterName(key), null, (String) arrayElement);
                                    globalDictionary.addEntry(entry);
                                    if (localDictionary != null) {
                                        localDictionary.addEntry(entry);
                                    }
                                } else if (arrayElement instanceof Double) {
                                    if (((Double) val % 1) == 0) {
                                        DictionaryEntry entry = new DictionaryEntry(new ParameterName(key), null, ((Double) arrayElement).intValue());
                                        globalDictionary.addEntry(entry);
                                        if (localDictionary != null) {
                                            localDictionary.addEntry(entry);
                                        }
                                    } else {
                                        DictionaryEntry entry = new DictionaryEntry(new ParameterName(key), null, (Double) arrayElement);
                                        globalDictionary.addEntry(entry);
                                        if (localDictionary != null) {
                                            localDictionary.addEntry(entry);
                                        }
                                    }
                                } else if (arrayElement instanceof Boolean) {
                                    DictionaryEntry entry = new DictionaryEntry(new ParameterName(key), null, (Boolean) arrayElement);
                                    globalDictionary.addEntry(entry);
                                    if (localDictionary != null) {
                                        localDictionary.addEntry(entry);
                                    }
                                } else if (arrayElement instanceof LinkedTreeMap) {
                                    queue.add(arrayElement);
                                }
                            }
                        } else if (val instanceof LinkedTreeMap) {
                            queue.add(val);
                        }
                    }
                } else if (o instanceof ArrayList) {
                    for (Object arrayElement : (ArrayList) o) {
                        if (arrayElement instanceof LinkedTreeMap) {
                            queue.add(arrayElement);
                        }
                    }
                }

                queue.remove(o);
            }
        } catch (Exception e) {
            logger.warn("Could not analyze response body. Body is not in JSON format.");
        }
    }

    /**
     * Changes the local dictionary on which the response analyzer operates.
     * @param dictionary the new local dictionary to use.
     */
    public void setLocalDictionary(Dictionary dictionary) {
        this.localDictionary = dictionary;
    }

    /**
     * Get the current local dictionary.
     * @return the current local dictionary.
     */
    public Dictionary getLocalDictionary() {
        return localDictionary;
    }
}
