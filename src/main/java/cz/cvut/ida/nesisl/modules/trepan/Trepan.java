package main.java.cz.cvut.ida.nesisl.modules.trepan;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by EL on 16.8.2016.
 */
public class Trepan {

    // path to trepan executables !!!! (currently hardcoded)
    private String trepanExe = (System.getProperty("os.name").contains("Windows"))
            ? "H:\\skola\\phd\\propositional_NSI\\TrepanWin\\TrepanWin.exe"
            : "/mnt/storage-brno6/home/svatoma1/trepan/TrepanWin.exe";


    private final NeuralNetwork learnedNetwork;
    private final Dataset dataset;
    private final String algName;
    private final int runIdx;
    private final String folderName;

    public Trepan(NeuralNetwork learnedNetwork, Dataset dataset, String algName, int runIdx, String folderName) {
        this.learnedNetwork = learnedNetwork;
        this.dataset = dataset;
        this.algName = algName;
        this.runIdx = runIdx;
        this.folderName = folderName + File.separator + "trepan" + File.separator;
    }

    public static Trepan create(NeuralNetwork learnedNetwork, Dataset dataset, String algName, int runIdx, String folderName) {
        return new Trepan(learnedNetwork, dataset, algName, runIdx, folderName);
    }

    public TrepanResults run() {
        File folder = createFolder();
        createFiles();
        System.out.println("run\t" + folderName);
        runTrepan(folder);
        return extractResults();
    }

    private TrepanResults extractResults() {
        File tree = new File(folderName + "trepan.tree");
        File fidelity = new File(folderName + "trepan.fidelity");
        File acc = new File(folderName + "trepan.acc");
        return TrepanResults.create(tree, fidelity, acc);
    }

    private void runTrepan(File folder) {
        runTrepanAcc(folder);
        runTrepanComputation(folder);
    }

    private int runTrepanAcc(File folder) {
        ProcessBuilder builder;
        if (System.getProperty("os.name").contains("Windows")) {
            builder = new ProcessBuilder(trepanExe, folderName + "acc.cmd");//, ">", folderName  + "trepan.acc");
        } else {
            // grid
            builder = new ProcessBuilder("wine", trepanExe, folderName + "acc.cmd");//, ">", folderName  + "trepan.acc");
        }
        System.out.println("original directory\t" + builder.directory());
        builder = builder.directory(folder);
        //builder = builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder = builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder = builder.redirectOutput(new File(folderName + "trepan.acc"));
        Process process = null;
        System.out.println("starting directory\t" + builder.directory());
        try {
            process = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return process.exitValue();
    }

    private int runTrepanComputation(File folder) {
        ProcessBuilder builder;
        if (System.getProperty("os.name").contains("Windows")) {
            builder = new ProcessBuilder(trepanExe, folderName + "trepan.cmd");
        } else {
            // grid
            builder = new ProcessBuilder("wine", trepanExe, folderName + "trepan.cmd");
        }
        builder = builder.directory(folder);
        builder = builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return process.exitValue();
    }

    private void createFiles() {
        Tools.storeToFile(cmdContent(), folderName + File.separator + "trepan.cmd");
        Tools.storeToFile(cmdContentAcc(), folderName + File.separator + "acc.cmd");
        Tools.storeToFile(trainData(), folderName + File.separator + "trepan.train.pat");
        Tools.storeToFile(testData(), folderName + File.separator + "trepan.test.pat");
        Tools.storeToFile(network(), folderName + File.separator + "trepan.net");
        Tools.storeToFile(weights(), folderName + File.separator + "trepan.wgt");
        Tools.storeToFile(attributes(), folderName + File.separator + "trepan.attr");
        Tools.storeToFile(attributesMapping(), folderName + File.separator + "trepan.attr.mapping");
    }

    private String cmdContentAcc() {
        String multiclass = (learnedNetwork.getNumberOfOutputNodes() < 2) ? "" : "set classification_function one_of_n\n";
        String multiclassOutput = (learnedNetwork.getNumberOfOutputNodes() < 2) ? "" : "get attribute_mappings\t\t" + folderName + "trepan.attr.mapping\n";
        return "get attributes  " + folderName + "trepan.attr\n" +
                multiclassOutput +
                "get training_examples  " + folderName + "trepan.train.pat\n" +
                "get test_examples  " + folderName + "trepan.test.pat\n" +
                "get network  " + folderName + "trepan\n" +
                "\n" +
                multiclass +
                "classify_using_network\n" +
                "quit";
    }

    private String attributesMapping() {
        return attributesMapping(learnedNetwork, false);
    }

    /**
     * If includeClasses is true, then returns "class   bits    0 0 1   0 1 0   ...\n class bits    classs==1   class==2 ...". Otherwise returns only the first row.
     *
     * @param network
     * @param includeClasses
     * @return
     */
    public static String attributesMapping(NeuralNetwork network, boolean includeClasses) {
        StringBuilder sb = new StringBuilder();
        sb.append("class\t " + network.getNumberOfOutputNodes() + " \t");
        for (int idx = 0; idx < network.getOutputNodes().size(); idx++) {
            String version = "";
            for (int outputs = 0; outputs < network.getOutputNodes().size(); outputs++) {
                version += (outputs == idx) ? "1 " : "0 ";
            }
            sb.append(version + "\t");
        }

        if (includeClasses) {
            sb.append("\nclass\t " + network.getNumberOfOutputNodes() + " \t");
            for (int idx = 0; idx < network.getOutputNodes().size(); idx++) {
                sb.append(network.getOutputNodes().get(idx).getName() + "\t");
            }
        }
        return sb.toString();
    }

    private String cmdContent() {
        String multiclass = (learnedNetwork.getNumberOfOutputNodes() < 2) ? "" : "set classification_function one_of_n\n";
        String multiclassOutput = (learnedNetwork.getNumberOfOutputNodes() < 2) ? "" : "get attribute_mappings\t\t" + folderName + "trepan.attr.mapping\n";
        return "get attributes  " + folderName + "trepan.attr\n" +
                multiclassOutput +
                "get training_examples  " + folderName + "trepan.train.pat\n" +
                "get test_examples  " + folderName + "trepan.test.pat\n" +
                "get network  " + folderName + "trepan\n" +
                "\n" +
                multiclass +
                "set seed\t\t\t\t10\n" +
                "set tree_size_limit\t\t15\n" +
                "set min_sample\t\t\t250\n" +
                "\n" +
                "disjunctive_trepan  " + folderName + "trepan.fidelity\n" +
                "test_fidelity\n" +
                "test_correctness\n" +
                "print_tree\n" +
                "draw_tree  " + folderName + "trepan.tree\n" +
                "\n" +
                "quit";
    }

    private String testData() {
        return formatedData(dataset.getTestData(learnedNetwork), "s");
    }

    private String trainData() {
        return formatedData(dataset.getTrainData(learnedNetwork), "r");
    }

    private String formatedData(List<Sample> data, String what) {
        Set<Integer> booleans = new HashSet<>();
        IntStream.range(0, learnedNetwork.getInputNodes().size())
                .forEach(idx -> {
                    if (isFactBoolean(learnedNetwork.getInputFactOrder().get(idx))) {
                        booleans.add(idx);
                    }
                });
        IntStream.range(learnedNetwork.getInputNodes().size(), learnedNetwork.getInputNodes().size() + learnedNetwork.getOutputNodes().size())
                .forEach(idx -> booleans.add(idx));

        StringBuilder sb = new StringBuilder();
        IntStream.range(0, data.size())
                .forEach(idx -> {
                    sb.append(what + idx + "\t");
                    Sample sample = data.get(idx);
                    IntStream.range(0, sample.getInput().size())
                            .forEach(attributeIdx -> {
                                Value value = sample.getInput().get(attributeIdx);
                                if (booleans.contains(attributeIdx)) {
                                    if (Tools.isZero(value.getValue() - 0)) {
                                        sb.append("f");
                                    } else if (Tools.isZero(value.getValue() - 1)) {
                                        sb.append("t");
                                    } else {
                                        sb.append("?");
                                    }
                                } else {
                                    sb.append(value.getValue());
                                }
                                sb.append("\t");
                            });
                    if (learnedNetwork.getNumberOfOutputNodes() > 1) {
                        sample.getOutput()
                                .forEach(value -> sb.append(((value.getValue() > 0.5) ? "1" : "0")));
                    } else {
                        if (sample.getOutput().get(0).getValue() > 0.5) {
                            sb.append("t");
                        } else {
                            sb.append("f");
                        }
                    }
                    sb.append("\n");
                });

        return sb.toString();
    }

    private String attributes() {
        StringBuilder sb = new StringBuilder();
        learnedNetwork.getInputFactOrder()
                .forEach(fact -> {
                    sb.append(fact.getFact() + "\t" + (isFactBoolean(fact) ? "B" : "R") + "\n");
                });

        /*learnedNetwork.getOutputFactOrder()
                .forEach(fact -> {
                    sb.append(fact.getFact() + "\t B" + "\n");
                });*/
        //sb.append("class\t V\t " + (learnedNetwork.getOutputNodes().size()-1) + "\t");
        if (learnedNetwork.getNumberOfOutputNodes() > 1) {
            sb.append("class\t N \t");
            for (int idx = 0; idx < learnedNetwork.getOutputNodes().size(); idx++) {
                String version = "";
                for (int outputs = 0; outputs < learnedNetwork.getOutputNodes().size(); outputs++) {
                    version += (outputs == idx) ? "1" : "0";
                }
                sb.append(version + "\t");
            }
        } else {
            sb.append("class\t B");
        }

        sb.append("\n");
        return sb.toString();
    }

    private boolean isFactBoolean(Fact fact) {
        return fact.getFact().contains(DatasetImpl.ATTRIBUTE_VALUE_DELIMITER);
    }

    private String network() {
        StringBuilder connections = new StringBuilder();

        Pair<Map<Node, Integer>, Map<Integer, Node>> pair = fillIndexMapping();
        Map<Node, Integer> nodeToIdx = pair.getLeft();
        Map<Integer, Node> idxToNode = pair.getRight();
        Node bias = learnedNetwork.getBias();
        IntStream.range(0, idxToNode.size())
                .forEach(idx -> {
                    Node node = idxToNode.get(idx);
                    Set<Edge> edges = learnedNetwork.getIncomingForwardEdges(node);
                    Integer min = edges.stream().filter(edge -> !edge.getSource().equals(bias)).mapToInt(edge -> nodeToIdx.get(edge.getSource())).min().orElse(0);
                    Integer max = edges.stream().filter(edge -> !edge.getSource().equals(bias)).mapToInt(edge -> nodeToIdx.get(edge.getSource())).max().orElse(0);
                    if (0 != max) {
                        connections.append("%r " + idx + " 1 " + min + " " + (max - min + 1) + "\n");
                    }
                });

        long numberOfInputs = learnedNetwork.getNumberOfInputNodes();
        long numberOfOutputs = learnedNetwork.getNumberOfOutputNodes();
        long numberOfUnits = (learnedNetwork.getNumberOfHiddenNodes() + numberOfInputs + numberOfOutputs);
        return "definitions:\n" +
                "nunits   " + numberOfUnits + "\n" +
                "ninputs  " + numberOfInputs + "\n" +
                "noutputs " + numberOfOutputs + "\n" +
                "end\n" +
                "network:\n" +
                connections.toString() +
                "end\n" +
                "biases:\n" +
                "end\n";
    }

    private String weights() {
        Pair<Map<Node, Integer>, Map<Integer, Node>> mapping = fillIndexMapping();
        Map<Node, Integer> nodeToIdx = mapping.getLeft();
        Map<Integer, Node> idxToNode = mapping.getRight();
        StringBuilder builder = new StringBuilder();


        Node bias = learnedNetwork.getBias();

        // connections, only forward
        IntStream.range(0, idxToNode.size())
                .forEach(idx -> {
                    Node node = idxToNode.get(idx);
                    Set<Edge> edges = learnedNetwork.getIncomingForwardEdges(node);
                    Integer min = edges.stream().filter(edge -> !edge.getSource().equals(bias)).mapToInt(edge -> nodeToIdx.get(edge.getSource())).min().orElse(0);
                    Integer max = edges.stream().filter(edge -> !edge.getSource().equals(bias)).mapToInt(edge -> nodeToIdx.get(edge.getSource())).max().orElse(0);

                    if (0 != max) {
                        IntStream.range(min, max + 1)
                                .forEach(sourceIdx -> {
                                    Node sourceNode = idxToNode.get(sourceIdx);
                                    Edge edge = new Edge(sourceNode, node, Edge.Type.FORWARD);
                                    if (builder.length() > 0) {
                                        builder.append("\n");
                                    }
                                    if (!learnedNetwork.getWeights().containsKey(edge)) {
                                        builder.append("0.0");
                                    } else {
                                        builder.append("" + learnedNetwork.getWeight(edge));
                                    }
                                });
                    }
                });


        // biases
        IntStream.range(0, nodeToIdx.size())
                .forEach(idx -> {
                    builder.append("\n");
                    Node node = idxToNode.get(idx);
                    Edge biasConnection = new Edge(bias, node, Edge.Type.FORWARD);
                    if (!learnedNetwork.getWeights().containsKey(biasConnection)) {
                        builder.append("0.0");
                    } else {
                        builder.append("" + learnedNetwork.getWeight(biasConnection));
                    }
                });
        return builder.toString();
    }

    private Pair<Map<Node, Integer>, Map<Integer, Node>> fillIndexMapping() {
        Map<Node, Integer> nod = new HashMap<>();
        Map<Integer, Node> idx = new HashMap<>();

        learnedNetwork.getInputNodes().forEach(node -> {
            nod.put(node, nod.size());
            idx.put(nod.get(node), node);
        });
        learnedNetwork.getHiddenNodes().forEach(node -> {
            nod.put(node, nod.size());
            idx.put(nod.get(node), node);
        });
        learnedNetwork.getOutputNodes().forEach(node -> {
            nod.put(node, nod.size());
            idx.put(nod.get(node), node);
        });
        return new Pair<>(nod, idx);
    }

    private File createFolder() {
        File folder = new File(folderName);
        folder.mkdirs();
        return folder;
    }
}
