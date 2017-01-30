package main.java.cz.cvut.ida.nesisl.modules.export.neuralNetwork.tex;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.modules.export.texFile.TexFile;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.ConstantOne;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.Sigmoid;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by EL on 12.2.2016.
 */
public class TikzExporter {

    private static final String DOCUMENT_HEAD = "\\documentclass{standalone}\n" +
            "\\usepackage{tikz}\n" +
            "\\begin{document}\n";
    private static final String DOCUMENT_TAIL = "\\end{document}";
    private static final String LAYER_DISTANCE = "2.5cm";
    private static HashMap<Object, Object> nodesDescriptions = new HashMap<>();


    public static TexFile export(NeuralNetwork network) {
        return new TexFile(exportToString(network));
    }

    public static String exportToString(NeuralNetwork network) {
        StringBuilder mainSB = new StringBuilder();
        mainSB.append(documentTexHead());
        mainSB.append(convertToTikz(network));
        mainSB.append(documentTexTail());
        //return mainSB.toString();
        return mainSB.toString().replaceAll("==", "--");
    }

    private static void initDescriptions() {
        synchronized (nodesDescriptions) {
            nodesDescriptions.put(Identity.getFunction().getName(), "\\tikzstyle{" + Identity.getFunction().getName() + "}=[neuron, fill=yellow!50];");
            nodesDescriptions.put(Identity.getFunction().getName(), "\\tikzstyle{" + Identity.getFunction().getName() + "}=[neuron, fill=green!50];");
            nodesDescriptions.put(Sigmoid.getFunction().getName(), "\\tikzstyle{" + Sigmoid.getFunction().getName() + "}=[neuron, fill=red!50];");
            nodesDescriptions.put(ConstantOne.getFunction().getName(), "\\tikzstyle{" + ConstantOne.getFunction().getName() + "}=[neuron, fill=white!50];");
        }
    }

    public static String convertToTikz(NeuralNetwork network) {
        initDescriptions();
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{tikzpicture}[shorten >=1pt,->,draw=black!,node distance=" + LAYER_DISTANCE + "]\n" // 50, node distance=\layersep
                + "\\tikzstyle{neuron}=[circle,fill=black!25,minimum size=17pt,inner sep=0pt]\n");
        sb.append(nodesDescription(network));
        sb.append(networkStructure(network));
        sb.append(edgesPart(network));
        sb.append("\\end{tikzpicture}\n");
        return sb.toString();
    }

    private static String networkStructure(NeuralNetwork network) {
        StringBuilder sb = new StringBuilder();

        Node layerLeader = null;
        Node previous = null;
        String position = "";
        List<Node> inputLayer = new ArrayList<>();
        inputLayer.addAll(network.getInputNodes());
        inputLayer.add(network.getBias());
        for (Node node : inputLayer) {
            String mark = nodeMark(node);
            if (null == previous) {
                layerLeader = node;
                position = "";
            } else {
                position = ",below of=" + nodeMark(previous);
            }
            sb.append("\\node [" + node.getActivationFunction().getName() + "" + position + "] (" + mark + ") {" + nodeText(node) + "};\n");
            previous = node;
        }

        for (int layerId = 0; layerId <= network.getMaximalNumberOfHiddenLayer(); layerId++) {
            previous = null;
            for (Node node : network.getHiddenNodesInLayer(layerId)) {
                String mark = nodeMark(node);
                if (null == previous) {
                    position = ",right of=" + nodeMark(layerLeader);
                    layerLeader = node;
                } else {
                    position = ",below of=" + nodeMark(previous);
                }
                sb.append("\\node [" + node.getActivationFunction().getName() + "" + position + "] (" + mark + ") {" + nodeText(node) + "};\n");
                previous = node;
            }
        }

        previous = null;
        for (Node node : network.getOutputNodes()) {
            String mark = nodeMark(node);
            if (null == previous) {
                position = ",right of=" + nodeMark(layerLeader);
                layerLeader = node;
            } else {
                position = ",below of=" + nodeMark(previous);
            }
            sb.append("\\node [" + node.getActivationFunction().getName() + "" + position + "] (" + mark + ") {" + nodeText(node) + "};\n");
            previous = node;
        }

        return sb.toString();
    }

    private static String nodesDescription(NeuralNetwork network) {
        Set<String> types = filterActivationFunctions(network);
        StringBuilder sb = new StringBuilder();
        types.forEach(type -> {
            if (nodesDescriptions.containsKey(type)) {
                sb.append(nodesDescriptions.get(type) + "\n");
            }
        });
        return sb.toString();
    }

    private static Set<String> filterActivationFunctions(NeuralNetwork network) {
        Set<String> set = network.getNodes().stream().map(Node::getActivationFunction).map(ActivationFunction::getName).collect(Collectors.toSet());
        return set;
    }

    private static String edgesPart(NeuralNetwork network) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\path[every node/.style={sloped,anchor=south,auto=false}]\n");
        network.getNodes().forEach(node ->
                network.getOutgoingEdges(node).forEach(edge -> sb.append(edgeToTikz(edge, network.getWeight(edge)))));
        sb.append(";");
        return sb.toString();
    }

    private static String edgeToTikz(Edge edge, Double weight) {
        return "(" + nodeMark(edge.getSource()) + ") edge node {" + new DecimalFormat("#.##").format(weight) + "} (" + nodeMark(edge.getTarget()) + ")\n";
    }

    private static String nodeText(Node node) {
        if (null == node.getName() || node.getName().equals("")) {
            return "n" + node.getIndex();
        }
        return node.getName();
    }

    private static String nodeMark(Node node) {
        if (null == node.getName() || node.getName().equals("")) {
            return "n" + node.getIndex();
        }
        return node.getName() + "-" + node.getIndex();
    }

    private static String documentTexHead() {
        return DOCUMENT_HEAD;
    }

    private static String documentTexTail() {
        return DOCUMENT_TAIL;
    }


}
