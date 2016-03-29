package main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.export.neuralNetwork.tex.TikzExporter;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.tool.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Created by EL on 21.3.2016.
 */
public class TopGen {

    private NeuralNetwork network;
    private KBANN kbann;
    private final RandomGenerator randomGenerator;
    private Double networkError = Double.MAX_VALUE;

    public TopGen(KBANN kbann, RandomGeneratorImpl randomGenerator) {
        this.kbann = kbann;
        this.randomGenerator = randomGenerator;
        this.network = kbann.getNeuralNetwork();
    }

    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    public void setKbann(KBANN kbann) {
        this.kbann = kbann;
    }

    public void learn(Dataset dataset, WeightLearningSetting wls, TopGenSettings tgSettings, KBANNSettings kbannSetting) {
        // pairs <KBANN, (mean/average)dataSquaredError>
        Backpropagation.feedforwardBackpropagationStateful(network, dataset, wls);
        Double error = Tools.computeAverageSuqaredTotalError(network, dataset);

        Comparator<Triple<? extends Object, Double, Double>> comparator = (t1, t2) -> {
            if (t1.getT() == t2.getT() && t1.getW() == t2.getW()) {
                return 0;
            } else if (t1.getT() >= t2.getT()) {
                return -1;
            } else {
                return 1;
            }
        };
        PriorityQueue<Triple<NeuralNetwork, Double, Double>> queue = new PriorityQueue<>(comparator);
        queue.add(new Triple<>(network, error, 1.0));


        while (!queue.isEmpty()) {
            Triple<NeuralNetwork, Double, Double> current = queue.poll();
            updateNetwork(current);

            System.out.println(current.getT() + "\t < \t" + tgSettings.getThreshold());
            if (current.getT() < tgSettings.getThreshold()) {
                break;
            }

            List<Triple<NeuralNetwork, Double, Double>> successors = generateAndLearnSuccessors(current.getK(), dataset, tgSettings, wls, current.getW(), kbannSetting);
            queue.addAll(successors);

            Collections.sort(successors, comparator);
            System.out.println("\t" + successors.get(0).getT());


            if (queue.size() > tgSettings.getLengthOfOpenList()) {
                PriorityQueue<Triple<NeuralNetwork, Double, Double>> nextRound = new PriorityQueue<>(comparator);
                final PriorityQueue<Triple<NeuralNetwork, Double, Double>> finalQueue = queue;
                LongStream.range(0, tgSettings.getLengthOfOpenList()).forEach(i -> {
                    Triple<NeuralNetwork, Double, Double> move = finalQueue.poll();
                    nextRound.add(move);
                });
                queue = nextRound;
            }
        }
    }

    private void updateNetwork(Triple<NeuralNetwork, Double, Double> triple) {
        if (networkError > triple.getT()) {
            network = triple.getK();
            networkError = triple.getT();
        }
    }

    private List<Triple<NeuralNetwork, Double, Double>> generateAndLearnSuccessors(NeuralNetwork network, Dataset dataset, TopGenSettings tgSettings, WeightLearningSetting wls, Double previousLearningRate, KBANNSettings kbannSetting) {
        List<Triple<Pair<Node, Boolean>, Long, Long>> generated = generateSorted(network, dataset);
        Stream<Triple<Pair<Node, Boolean>, Long, Long>> cutted = generated.stream().limit(tgSettings.getNumberOfSuccessors());
        return cutted.parallel().map(triple -> addNodeAndLearnNetwork(triple.getK().getLeft(), triple.getK().getRight(), network, dataset, wls, previousLearningRate, kbannSetting)).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Triple<Pair<Node, Boolean>, Long, Long>> generateSorted(NeuralNetwork network, Dataset dataset) {
        Map<Sample, Results> results = Tools.evaluateAllAndGetResults(dataset, network);
        Map<Sample, Boolean> correctlyClassified = Tools.classify(results);

        List<Triple<Pair<Node, Boolean>, Long, Long>> generated = computeValues(network, results, correctlyClassified);

        // descending order
        Comparator<Triple<? extends Object, Long, Long>> comparator = (t1, t2) -> {
            if (t1.getT() == t2.getT()) {
                return -Long.compare(t1.getW(), t2.getW());
            }
            return -Long.compare(t1.getT(), t2.getT());
        };
        Collections.sort(generated, comparator);
        return generated;
    }

    private static List<Triple<Pair<Node, Boolean>, Long, Long>> computeValues(NeuralNetwork network, Map<Sample, Results> results, Map<Sample, Boolean> correctlyClassified) {
        return network.getHiddenNodes().parallelStream().map(node -> computeFPandFN(network, node, results, correctlyClassified)).flatMap(l -> l.stream()).collect(Collectors.toCollection(ArrayList::new));
    }

    public static NeuralNetwork generateSuccessor(NeuralNetwork network, Dataset dataset, int which, KBANNSettings kbannSettings, RandomGenerator randomGenerator) {
        NeuralNetwork currentNetwork = network.getCopy();
        List<Triple<Pair<Node, Boolean>, Long, Long>> generated = generateSorted(currentNetwork, dataset);
        Triple<Pair<Node, Boolean>, Long, Long> picked = generated.get(which);
        return addNode(picked.getK().getLeft(), picked.getK().getRight(), currentNetwork, kbannSettings, randomGenerator);
    }

    private Triple<NeuralNetwork, Double, Double> addNodeAndLearnNetwork(Node node, Boolean isFalsePositive, NeuralNetwork network, Dataset dataset, WeightLearningSetting wls, Double previousLearningRate, KBANNSettings kbannSetting) {
        network = addNode(node, isFalsePositive, network, kbannSetting, randomGenerator);

        double learningRate = previousLearningRate * wls.getLearningRate(); // misto wls.getLearningRate by tu melo byt neco ve smyslu decay, ktere je ale parametrem tgSettings
        WeightLearningSetting updatedWls = new WeightLearningSetting(wls.getEpsilonDifference(), learningRate, wls.getMaximumNumberOfHiddenNodes(), wls.getSizeOfCasCorPool(), wls.getMaxAlpha(), wls.getQuickpropEpsilon(), wls.getEpochLimit(), wls.getMomentumAlpha());

        network = Backpropagation.feedforwardBackpropagation(network, dataset, updatedWls);
        double error = Tools.computeAverageSuqaredTotalError(network, dataset);
        return new Triple<>(network, error, learningRate);
    }

    private static NeuralNetwork addNode(Node node, Boolean isFalsePositive, NeuralNetwork network, KBANNSettings kbannSetting, RandomGenerator randomGenerator) {
        boolean isAndNode = isAndNode(node, network);
        if ((isAndNode && !isFalsePositive) || (!isAndNode && isFalsePositive)) {
            return addAndNode(node, network, kbannSetting, randomGenerator);
        } else {
            return addOrNode(node, network, randomGenerator);
        }
    }

    public static boolean isAndNode(Node node, NeuralNetwork network) {
        Node bias = network.getBias();
        final NeuralNetwork finalNetwork = network;
        Double positiveWeightSum = network.getIncomingForwardEdges(node).parallelStream().filter(edge -> !bias.equals(edge.getSource()) && finalNetwork.getWeight(edge) >= 0).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();
        Double negativeWeightSum = network.getIncomingForwardEdges(node).parallelStream().filter(edge -> !bias.equals(edge.getSource()) && finalNetwork.getWeight(edge) < 0).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();
        Double biasWeight = network.getIncomingForwardEdges(node).parallelStream().filter(edge -> bias.equals(edge.getSource())).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();

        return Math.abs(positiveWeightSum - biasWeight) < Math.abs(negativeWeightSum - biasWeight);
    }

    private static NeuralNetwork addOrNode(Node node, NeuralNetwork network, RandomGenerator randomGenerator) {
        Pair<NeuralNetwork, Map<Node, Node>> copied = network.getCopyWithMapping();
        NeuralNetwork currentNetwork = copied.getLeft();
        Map<Node, Node> map = copied.getRight();
        Node currentParent = map.get(node);

        Long layerNumber = currentNetwork.getLayerNumber(currentParent);
        Node newNode = NodeFactory.create(Sigmoid.getFunction());
        currentNetwork.addNodeAtLayerStateful(newNode, layerNumber - 1);
        currentNetwork.addEdgeStateful(newNode, currentParent, randomGenerator.nextDouble(), Edge.Type.FORWARD);

        List<Node> inputs = new ArrayList<>();
        inputs.addAll(currentNetwork.getInputNodes());
        inputs.add(currentNetwork.getBias());

        Tools.makeFullInterLayerForwardConnections(inputs, newNode, currentNetwork, randomGenerator);

        return currentNetwork;
    }

    private static NeuralNetwork addAndNode(Node node, NeuralNetwork network, KBANNSettings kbannSetting, RandomGenerator randomGenerator) {
        Pair<NeuralNetwork, Map<Node, Node>> copied = network.getCopyWithMapping();
        NeuralNetwork currentNetwork = copied.getLeft();
        Map<Node, Node> map = copied.getRight();
        Node currentParent = map.get(node);

        Long layerNumber = currentNetwork.getLayerNumber(currentParent);
        Node newNode = NodeFactory.create(Sigmoid.getFunction());
        currentNetwork.addNodeAtLayerStateful(newNode, layerNumber);

        List<Node> inputs = new ArrayList<>();
        inputs.addAll(currentNetwork.getInputNodes());
        inputs.add(currentNetwork.getBias());

        Tools.makeFullInterLayerForwardConnections(inputs, newNode, currentNetwork, randomGenerator);

        Node newOr = NodeFactory.create(Sigmoid.getFunction());

        currentNetwork.insertIntermezzoNodeStateful(currentParent, newOr);

        currentNetwork.addEdgeStateful(newNode, newOr, 1.0d, Edge.Type.FORWARD);
        currentNetwork.addEdgeStateful(currentParent, newOr, 1.0d, Edge.Type.FORWARD);
        // -1*bias - because KBANN activation is s = (netInput_i - bias) and bias has value of 1 here
        currentNetwork.addEdgeStateful(currentNetwork.getBias(), newOr, -1 * kbannSetting.getOmega() / 2, Edge.Type.FORWARD);

        return currentNetwork;
    }

    private static List<Triple<Pair<Node, Boolean>, Long, Long>> computeFPandFN(NeuralNetwork network, Node node, Map<Sample, Results> results, Map<Sample, Boolean> correctlyClassified) {
        long falsePositive = results.keySet().stream().filter(sample -> !correctlyClassified.get(sample)).filter(sample ->
                        results.get(sample).getComputedValues().get(node) > 0
        ).count();

        long falseNegative = results.keySet().stream().filter(sample -> correctlyClassified.get(sample)).filter(sample ->
                        results.get(sample).getComputedValues().get(node) <= 0
        ).count();

        long layerIdx = network.getLayerNumber(node);

        ArrayList<Triple<Pair<Node, Boolean>, Long, Long>> list = new ArrayList<>();
        list.add(new Triple(new Pair<>(node, true), falsePositive, layerIdx));
        list.add(new Triple(new Pair<>(node, false), falseNegative, layerIdx));
        return list;
    }


    public static TopGen create(File file, List<Pair<Integer, ActivationFunction>> specific, RandomGeneratorImpl randomGenerator, Double omega) {
        KBANN kbann = new KBANN(file, specific, randomGenerator, omega);
        return new TopGen(kbann, randomGenerator);
    }


}
