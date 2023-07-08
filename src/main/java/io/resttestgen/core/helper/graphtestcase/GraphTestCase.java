package io.resttestgen.core.helper.graphtestcase;

import io.resttestgen.boot.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestSequence;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class GraphTestCase {

    private static final Configuration configuration = Environment.getInstance().getConfiguration();
    private static final Graph<String, ParameterEdge> graph = new DirectedMultigraph<>(ParameterEdge.class);
    private static final String staticSourceNode = "0: Static source";

    // Data just for graphical representation. Might be incomplete
    public static void generateGraph(TestSequence testSequence) {

        graph.addVertex(staticSourceNode);

        for (TestInteraction interaction : testSequence) {

            String targetNode = getNodeFromInteraction(testSequence, interaction);
            graph.addVertex(targetNode);

            for (LeafParameter leaf : interaction.getFuzzedOperation().getLeaves()) {
                if (leaf.getValue() instanceof LeafParameter) {
                    String sourceNode = getNodeFromInteraction(testSequence,
                            getInteractionFromOperation(testSequence, ((LeafParameter) leaf.getValue()).getOperation()));
                    ParameterEdge edge = new ParameterEdge((LeafParameter) leaf.getValue(), leaf);
                    graph.addEdge(sourceNode, targetNode, edge);
                } else {
                    ParameterEdge edge = new ParameterEdge(leaf, leaf);
                    graph.addEdge(staticSourceNode, targetNode, edge);
                }
            }
        }

        String previousNode = staticSourceNode;

        for (TestInteraction interaction : testSequence) {
            String currentNode = getNodeFromInteraction(testSequence, interaction);
            graph.addEdge(previousNode, currentNode, new ParameterEdge());
            previousNode = currentNode;
        }

        try {
            saveToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getNodeFromInteraction(TestSequence sequence, TestInteraction interaction) {
        if (sequence != null && interaction != null) {
            int number = sequence.indexOf(interaction) + 1;
            return number + ": " + interaction.getFuzzedOperation().toString();
        }
        return "Error";
    }

    private static TestInteraction getInteractionFromOperation(TestSequence sequence, Operation operation) {
        for (TestInteraction interaction : sequence) {
            if (interaction.getFuzzedOperation() == operation) {
                return interaction;
            }
        }
        return null;
    }

    private static void saveToFile() throws IOException {
        DOTExporter<String, ParameterEdge> exporter = new DOTExporter<>();
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v));
            return map;
        });
        exporter.setEdgeAttributeProvider((e) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(e.toString()));
            map.put("weight", DefaultAttribute.createAttribute(e.weight));
            map.put("style", DefaultAttribute.createAttribute(e.style));
            return map;
        });

        File file = new File(configuration.getOutputPath() + configuration.getTestingSessionName() + "/");
        file.mkdirs();

        Writer writer = new FileWriter(configuration.getOutputPath() + configuration.getTestingSessionName() + "/graphTestCase.dot");
        exporter.exportGraph(graph, writer);
        writer.flush();
        writer.close();

        String command = "dot -Tjpg " + configuration.getOutputPath() + configuration.getTestingSessionName() + "/graphTestCase.dot";

        Process process = Runtime.getRuntime().exec(command);

        File file2 = new File(configuration.getOutputPath() + configuration.getTestingSessionName() + "/graphTestCase.jpg");
        copyInputStreamToFile(process.getInputStream(), file2);
    }

    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {

        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[8192];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }

    }
}
