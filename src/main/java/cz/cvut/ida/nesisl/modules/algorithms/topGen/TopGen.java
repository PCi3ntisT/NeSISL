package main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
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

            if (current.getT() < tgSettings.getThreshold()) {
                break;
            }

            List<Triple<NeuralNetwork, Double, Double>> successors = generateAndLearnSuccessors(current.getK(), dataset, tgSettings, wls, current.getW(), kbannSetting);
            queue.addAll(successors);

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
        Map<Sample, Results> results = Tools.evaluateAllAndGetResults(dataset, network);
        Map<Sample, Boolean> correctlyClassified = Tools.classify(results);

        List<Triple<Node, Long, Long>> generated = network.getHiddenNodes().parallelStream().map(node -> computeFPandFN(network, node, results, correctlyClassified)).flatMap(l -> l.stream()).collect(Collectors.toCollection(ArrayList::new));

        Comparator<Triple<? extends Object, Long, Long>> comparator = (t1, t2) -> {
            if (t1.getT() == t2.getT() && t1.getW() == t2.getW()) {
                return 0;
            } else if (t1.getT() >= t2.getT()) {
                return -1;
            } else {
                return 1;
            }
        };
        Collections.sort(generated, comparator);
        Stream<Triple<Node, Long, Long>> cutted = generated.stream().limit(tgSettings.getNumberOfSuccessors());
        return cutted.parallel().map(triple -> addNodeAndLearnNetwork(triple.getK(), network, dataset, wls, previousLearningRate, kbannSetting)).collect(Collectors.toCollection(ArrayList::new));
    }

    private Triple<NeuralNetwork, Double, Double> addNodeAndLearnNetwork(Node node, NeuralNetwork network, Dataset dataset, WeightLearningSetting wls, Double previousLearningRate, KBANNSettings kbannSetting) {
        Node bias = network.getBias();
        final NeuralNetwork finalNetwork = network;
        Double positiveWeightSum = network.getIncomingForwardEdges(node).parallelStream().filter(edge -> !bias.equals(edge.getSource()) && finalNetwork.getWeight(edge) >= 0).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();
        Double negativeWeightSum = network.getIncomingForwardEdges(node).parallelStream().filter(edge -> !bias.equals(edge.getSource()) && finalNetwork.getWeight(edge) < 0).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();
        Double biasWeight = network.getIncomingForwardEdges(node).parallelStream().filter(edge -> bias.equals(edge.getSource())).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();

        if (Math.abs(positiveWeightSum - biasWeight) < Math.abs(negativeWeightSum - biasWeight)) {
            network = addAndNode(node, network, kbannSetting);
        } else {
            network = addOrNode(node, network);
        }

        double learningRate = previousLearningRate * wls.getLearningRate(); // misto wls.getLearningRate by tu melo byt neco ve smyslu decay, ktere je ale parametrem tgSettings
        WeightLearningSetting updatedWls = new WeightLearningSetting(wls.getEpsilonDifference(), learningRate, wls.getMaximumNumberOfHiddenNodes(), wls.getSizeOfCasCorPool(), wls.getMaxAlpha(), wls.getQuickpropEpsilon(), wls.getEpochLimit());

        network = Backpropagation.feedforwardBackpropagation(network, dataset, updatedWls);
        double error = Tools.computeAverageSuqaredTotalError(network, dataset);
        return new Triple<>(network, error, learningRate);
    }

    private NeuralNetwork addOrNode(Node node, NeuralNetwork network) {
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

    private NeuralNetwork addAndNode(Node node, NeuralNetwork network, KBANNSettings kbannSetting) {
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

    private List<Triple<Node, Long, Long>> computeFPandFN(NeuralNetwork network, Node node, Map<Sample, Results> results, Map<Sample, Boolean> correctlyClassified) {
        long falsePositive = results.keySet().stream().filter(sample -> !correctlyClassified.get(sample)).filter(sample ->
                        results.get(sample).getComputedValues().get(node) > 0
        ).count();

        long falseNegative = results.keySet().stream().filter(sample -> correctlyClassified.get(sample)).filter(sample ->
                        results.get(sample).getComputedValues().get(node) <= 0
        ).count();

        long layerIdx = network.getLayerNumber(node);

        ArrayList<Triple<Node, Long, Long>> list = new ArrayList<>();
        list.add(new Triple(node, falsePositive, layerIdx));
        list.add(new Triple(node, falseNegative, layerIdx));
        return list;
    }


    public static TopGen create(File file, List<Pair<Integer, ActivationFunction>> specific, RandomGeneratorImpl randomGenerator, Double omega) {
        KBANN kbann = new KBANN(file, specific, randomGenerator, omega);
        return new TopGen(kbann, randomGenerator);
    }
}
