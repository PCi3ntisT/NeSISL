package main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
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
public class TopGen implements NeuralNetworkOwner {

    private NeuralNetwork network;
    private final RandomGenerator randomGenerator;
    private Double networkError = Double.MAX_VALUE;

    public TopGen(KBANN kbann, RandomGeneratorImpl randomGenerator) {
        this.randomGenerator = randomGenerator;
        this.network = kbann.getNeuralNetwork();
    }

    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    public NeuralNetwork learn(Dataset dataset, WeightLearningSetting wls, TopGenSettings tgSettings) {
        KBANNSettings kbannSetting = new KBANNSettings(randomGenerator, tgSettings.getOmega());
        Backpropagation.feedforwardBackpropagationStateful(network, dataset, wls);
        Double error = Tools.computeAverageSquaredTrainTotalErrorPlusEdgePenalty(network, dataset, wls);

        Comparator<Triple<? extends Object, Double, Double>> comparator = (t1, t2) -> {
            if (Tools.isZero(t1.getT() - t2.getT()) && Tools.isZero(t1.getW() - t2.getW())) {
                return 0;
            } else if (t1.getT() >= t2.getT()) {
                return -1;
            } else {
                return 1;
            }
        };
        PriorityQueue<Triple<NeuralNetwork, Double, Double>> queue = new PriorityQueue<>(comparator);
        queue.add(new Triple<>(network, error, 1.0));

        List<Double> errors = new ArrayList<>();
        long iteration = 0;
        while (!queue.isEmpty() && tgSettings.canContinue(iteration, errors)) {
            Triple<NeuralNetwork, Double, Double> current = queue.poll();
            updateNetwork(current);

            if (current.getT() < tgSettings.getEpsilonLimit()) {
                break;
            }

            List<Triple<NeuralNetwork, Double, Double>> successors = generateAndLearnSuccessors(current.getK(), dataset, tgSettings, wls, current.getW(), kbannSetting);
            queue.addAll(successors);

            Collections.sort(successors, comparator);

            if (queue.size() > tgSettings.getLengthOfOpenList()) {
                PriorityQueue<Triple<NeuralNetwork, Double, Double>> nextRound = new PriorityQueue<>(comparator);
                final PriorityQueue<Triple<NeuralNetwork, Double, Double>> finalQueue = queue;
                LongStream.range(0, tgSettings.getLengthOfOpenList()).forEach(i -> {
                    Triple<NeuralNetwork, Double, Double> move = finalQueue.poll();
                    nextRound.add(move);
                });
                queue = nextRound;
            }
            iteration++;
            errors.add(networkError);
        }
        network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        return network;
    }

    private void updateNetwork(Triple<NeuralNetwork, Double, Double> triple) {
        if (networkError > triple.getT()) {
            network = triple.getK();
            networkError = triple.getT();
        }
    }

    private List<Triple<NeuralNetwork, Double, Double>> generateAndLearnSuccessors(NeuralNetwork network, Dataset dataset, TopGenSettings tgSettings, WeightLearningSetting wls, Double previousLearningRate, KBANNSettings kbannSetting) {
        List<Triple<Pair<Node, Boolean>, Long, Long>> generated = generateSorted(network, dataset);
        Stream<Triple<Pair<Node, Boolean>, Long, Long>> cut = generated.stream().limit(tgSettings.getNumberOfSuccessors());
        return cut.parallel().map(triple -> addNodeAndLearnNetwork(triple.getK().getLeft(), triple.getK().getRight(), network, dataset, wls, previousLearningRate, kbannSetting, tgSettings)).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Triple<Pair<Node, Boolean>, Long, Long>> generateSorted(NeuralNetwork network, Dataset dataset) {
        if (null == network.getClassifier()) {
            network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        }

        Map<Sample, Results> results = Tools.evaluateOnAndGetResults(dataset.getNodeTrainData(network), network);
        Map<Sample, Boolean> correctlyClassified = Tools.classify(network.getClassifier(), results);

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
        return network.getHiddenNodes()
                .parallelStream()
                .map(node -> computeFPandFN(network, node, results, correctlyClassified))
                .flatMap(l -> l.stream()).collect(Collectors.toCollection(ArrayList::new));
    }

    public static NeuralNetwork generateSuccessor(NeuralNetwork network, Dataset dataset, int which, KBANNSettings kbannSettings, RandomGenerator randomGenerator) {
        NeuralNetwork currentNetwork = network.getCopy();
        List<Triple<Pair<Node, Boolean>, Long, Long>> generated = generateSorted(currentNetwork, dataset);
        Triple<Pair<Node, Boolean>, Long, Long> picked = generated.get(which);
        return addNode(picked.getK().getLeft(), picked.getK().getRight(), currentNetwork, kbannSettings, randomGenerator);
    }

    private Triple<NeuralNetwork, Double, Double> addNodeAndLearnNetwork(Node node, Boolean isFalsePositive, NeuralNetwork network, Dataset dataset, WeightLearningSetting wls, Double previousLearningRate, KBANNSettings kbannSetting, TopGenSettings tgSettings) {
        network = addNode(node, isFalsePositive, network, kbannSetting, randomGenerator);

        double learningRate = previousLearningRate * tgSettings.getLearningRateDecay();
        WeightLearningSetting updatedWls = new WeightLearningSetting(wls.getFile(), wls.getEpsilonConvergent(), learningRate, wls.getMomentumAlpha(), wls.getEpochLimit(), wls.getShortTimeWindow(), wls.getLongTimeWindow(), wls.getPenaltyEpsilon(), wls.getSLFThreshold());

        network = Backpropagation.feedforwardBackpropagation(network, dataset, updatedWls);
        double error = Tools.computeAverageSquaredTrainTotalErrorPlusEdgePenalty(network, dataset, wls);
        network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
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
        long falsePositive = results.keySet()
                .stream().filter(sample -> !correctlyClassified.get(sample))
                .filter(sample ->
                                results.get(sample).getComputedValues().get(node) > network.getClassifier().getTreshold()
                ).count();

        long falseNegative = results.keySet()
                .stream().filter(sample -> correctlyClassified.get(sample))
                .filter(sample ->
                                results.get(sample).getComputedValues().get(node) <= network.getClassifier().getTreshold()
                ).count();

        long layerIdx = network.getLayerNumber(node);

        ArrayList<Triple<Pair<Node, Boolean>, Long, Long>> list = new ArrayList<>();
        list.add(new Triple(new Pair<>(node, true), falsePositive, layerIdx));
        list.add(new Triple(new Pair<>(node, false), falseNegative, layerIdx));
        return list;
    }


    public static TopGen create(File ruleFile, List<Pair<Integer, ActivationFunction>> specific, RandomGeneratorImpl randomGenerator, TopGenSettings topGenSettings) {
        KBANN kbann = KBANN.create(ruleFile, specific, new KBANNSettings(randomGenerator, topGenSettings.getOmega()));
        return new TopGen(kbann, randomGenerator);
    }


}
