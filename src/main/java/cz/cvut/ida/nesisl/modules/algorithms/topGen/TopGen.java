package main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.regent.Regent;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.export.neuralNetwork.tex.TikzExporter;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
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
        int innerFolds = 3;
        Dataset crossValDataset = DatasetImpl.stratifiedSplit(dataset, randomGenerator, innerFolds);

        KBANNSettings kbannSetting = new KBANNSettings(randomGenerator, tgSettings.getOmega(), tgSettings.perturbationMagnitude());
        Backpropagation.feedforwardBackpropagationStateful(network, crossValDataset, wls);
        Double error = Tools.computeAverageSquaredTrainTotalErrorPlusEdgePenalty(network, crossValDataset, wls);

        Comparator<Triple<? extends Object, Double, Double>> comparator = (t1, t2) -> {
            if (t1.getT() < t2.getT()) {
                return -1;
            } else if (t1.getT() > t2.getT()) {
                return 1;
            }
            return 0;
        };
        PriorityQueue<Triple<NeuralNetwork, Double, Double>> queue = new PriorityQueue<>(comparator);
        queue.add(new Triple<>(network, error, 1.0));

        List<Double> errors = new ArrayList<>();
        long iteration = 0;
        while (!queue.isEmpty() && tgSettings.canContinue(iteration, errors)) {
            System.out.println("opening node\t" + errors.size() + "\t" + (!errors.isEmpty() ? errors.get(errors.size() - 1) : ""));

            Triple<NeuralNetwork, Double, Double> current = queue.poll();
            updateNetwork(current);
            System.out.println("current\t" + current.getT());

            if (current.getT() < tgSettings.getEpsilonLimit()) {
                break;
            }

            List<Triple<NeuralNetwork, Double, Double>> successors = generateAndLearnSuccessors(current.getK(), crossValDataset, tgSettings, wls, current.getW(), kbannSetting);
            queue.addAll(successors);

            //Collections.sort(successors, comparator);

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
        network.setClassifierStateful(ThresholdClassificator.create(network, crossValDataset));
        return network;
    }

    private void updateNetwork(Triple<NeuralNetwork, Double, Double> triple) {
        if (networkError > triple.getT()) {
            network = triple.getK();
            networkError = triple.getT();
        }
    }

    private List<Triple<NeuralNetwork, Double, Double>> generateAndLearnSuccessors(NeuralNetwork network, Dataset dataset, TopGenSettings tgSettings, WeightLearningSetting wls, Double previousLearningRate, KBANNSettings kbannSetting) {
        List<Triple<Pair<Node, Boolean>, Long, Long>> generated = generateSorted(network, dataset, tgSettings);
        Stream<Triple<Pair<Node, Boolean>, Long, Long>> cut = generated
                .stream()
                .limit(tgSettings.getNumberOfSuccessors());
        return cut
                //.parallel()
                .map(triple -> addNodeAndLearnNetwork(triple.getK().getLeft(), triple.getK().getRight(), network, dataset, wls, previousLearningRate, kbannSetting, tgSettings)).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Triple<Pair<Node, Boolean>, Long, Long>> generateSorted(NeuralNetwork network, Dataset dataset, TopGenSettings tgSetting) {
        if (null == network.getClassifier()) {
            network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        }

        Map<Sample, Results> results = Tools.evaluateOnAndGetResults(dataset.getNodeTrainData(network), network);
        Map<Sample, Boolean> correctlyClassified = Tools.classify(network.getClassifier(), results);

        List<Triple<Pair<Node, Boolean>, Long, Long>> generated = computeValues(network, results, correctlyClassified, tgSetting);

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

    private static List<Triple<Pair<Node, Boolean>, Long, Long>> computeValues(NeuralNetwork network, Map<Sample, Results> results, Map<Sample, Boolean> correctlyClassified, TopGenSettings tgSetting) {
        return network.getHiddenNodes()
                //.parallelStream()
                .stream()
                .map(node -> computeFPandFN(network, node, results, correctlyClassified, tgSetting))
                .flatMap(l -> l.stream()).collect(Collectors.toCollection(ArrayList::new));
    }

    public static NeuralNetwork generateSuccessor(NeuralNetwork network, Dataset dataset, int which, KBANNSettings kbannSettings, TopGenSettings tgSetting, RandomGenerator randomGenerator) {
        NeuralNetwork currentNetwork = network.getCopy();
        if (0 < network.getNumberOfHiddenNodes()) {
            List<Triple<Pair<Node, Boolean>, Long, Long>> generated = generateSorted(currentNetwork, dataset, tgSetting);
            Triple<Pair<Node, Boolean>, Long, Long> picked = generated.get(which);
            NeuralNetwork r = addNode(picked.getK().getLeft(), picked.getK().getRight(), currentNetwork, kbannSettings, randomGenerator);


            if (Regent.check(r, " genSucA")) {
                throw new IllegalStateException();
            }

            return r;
        } else {
            Node node = NodeFactory.create(Sigmoid.getFunction());
            currentNetwork.addNodeAtLayerStateful(node, 0);
            List<Node> inputsAndBias = new ArrayList<>();
            inputsAndBias.add(currentNetwork.getBias());
            inputsAndBias.addAll(currentNetwork.getInputNodes());
            Tools.makeFullInterLayerForwardConnections(inputsAndBias, node, currentNetwork, kbannSettings.getRandomGenerator());
            List<Node> from = new ArrayList<>();
            from.add(node);
            Tools.makeFullInterLayerForwardConnections(from, currentNetwork.getOutputNodes(), currentNetwork, kbannSettings.getRandomGenerator());


            ((NeuralNetworkImpl) currentNetwork).setMsg("by zero adding\t" + node);

            if (Regent.check(currentNetwork, " genSucB")) {
                throw new IllegalStateException();
            }

            return currentNetwork;
        }
    }

    private Triple<NeuralNetwork, Double, Double> addNodeAndLearnNetwork(Node node, Boolean isFalsePositive, NeuralNetwork network, Dataset dataset, WeightLearningSetting wls, Double previousLearningRate, KBANNSettings kbannSetting, TopGenSettings tgSettings) {
        network = addNode(node, isFalsePositive, network, kbannSetting, randomGenerator);

        double learningRate = previousLearningRate * tgSettings.getLearningRateDecay();
        WeightLearningSetting updatedWls = new WeightLearningSetting(wls.getFile(), wls.getEpsilonConvergent(), learningRate, wls.getMomentumAlpha(), wls.getEpochLimit(), wls.getShortTimeWindow(), wls.getLongTimeWindow(), wls.getPenaltyEpsilon(), wls.getSLFThreshold());

        network = Backpropagation.feedforwardBackpropagation(network, dataset, updatedWls);
        double error = Tools.computeAverageSquaredTrainTotalErrorPlusEdgePenalty(network, dataset, wls);
        network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        System.out.println("\t" + error);
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
        Double positiveWeightSum = network.getIncomingForwardEdges(node)
                //.parallelStream()
                .stream()
                .filter(edge -> !bias.equals(edge.getSource()) && finalNetwork.getWeight(edge) >= 0).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();
        Double negativeWeightSum = network.getIncomingForwardEdges(node)
                //.parallelStream()
                .stream()
                .filter(edge -> !bias.equals(edge.getSource()) && finalNetwork.getWeight(edge) < 0).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();
        Double biasWeight = network.getIncomingForwardEdges(node)
                //.parallelStream()
                .stream()
                .filter(edge -> bias.equals(edge.getSource())).mapToDouble(edge -> finalNetwork.getWeight(edge)).sum();

        // cause in AND case biasWeight is to be negative
        // and in OR case biasWeight is to be positive
        return Math.abs(positiveWeightSum + biasWeight) < Math.abs(-negativeWeightSum + biasWeight);
    }

    private static NeuralNetwork addOrNode(Node node, NeuralNetwork network, RandomGenerator randomGenerator) {
        Pair<NeuralNetwork, Map<Node, Node>> copied = network.getCopyWithMapping();
        NeuralNetwork currentNetwork = copied.getLeft();
        Map<Node, Node> map = copied.getRight();
        Node currentParent = map.get(node);

        //String orig = TikzExporter.exportToString(currentNetwork);

        Long layerNumber = currentNetwork.getLayerNumber(currentParent);
        Node newNode = NodeFactory.create(Sigmoid.getFunction());
        currentNetwork.addNodeAtLayerStateful(newNode, layerNumber - 1);
        currentNetwork.addEdgeStateful(newNode, currentParent, randomGenerator.nextDouble(), Edge.Type.FORWARD);

        List<Node> inputs = new ArrayList<>();
        inputs.addAll(currentNetwork.getInputNodes());
        inputs.add(currentNetwork.getBias());

        Tools.makeFullInterLayerForwardConnections(inputs, newNode, currentNetwork, randomGenerator);

        ((NeuralNetworkImpl) currentNetwork).setMsg("by orNode\t" + newNode + "\n to network " + ((NeuralNetworkImpl) network).getMsg());

        if (Regent.check(currentNetwork, " addOrNode")) {
            System.out.println("right now added\t" + newNode + "\n"
                            + "from\n"
                            + TikzExporter.exportToString(network)
                            //+ "produced\n" + orig
                            + "final\n" + TikzExporter.exportToString(currentNetwork)
            );
            throw new IllegalStateException();
        }

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

        ((NeuralNetworkImpl) currentNetwork).setMsg("by andNode\t" + newNode + "\t" + newOr + "\n to network " + ((NeuralNetworkImpl) network).getMsg());

        if (Regent.check(currentNetwork, " addAndNode")) {
            throw new IllegalStateException();
        }
        return currentNetwork;
    }

    private static List<Triple<Pair<Node, Boolean>, Long, Long>> computeFPandFN(NeuralNetwork network, Node node, Map<Sample, Results> results, Map<Sample, Boolean> correctlyClassified, TopGenSettings tgSettings) {
        long falsePositive = results.keySet()
                .stream().filter(sample -> !correctlyClassified.get(sample))
                .filter(sample ->
                                results.get(sample).getComputedValues().get(node) > 1 - tgSettings.getNodeActivationThreshold()
                ).count();

        long falseNegative = results.keySet()
                .stream().filter(sample -> correctlyClassified.get(sample))
                .filter(sample ->
                                results.get(sample).getComputedValues().get(node) < tgSettings.getNodeActivationThreshold()
                ).count();

        long layerIdx = network.getLayerNumber(node);

        ArrayList<Triple<Pair<Node, Boolean>, Long, Long>> list = new ArrayList<>();
        list.add(new Triple(new Pair<>(node, true), falsePositive, layerIdx));
        list.add(new Triple(new Pair<>(node, false), falseNegative, layerIdx));
        return list;
    }

    public static TopGen create(File ruleFile, List<Pair<Integer, ActivationFunction>> specific, RandomGeneratorImpl randomGenerator, TopGenSettings topGenSettings, Dataset dataset, boolean softmaxOutputs) {
        KBANN kbann = KBANN.create(ruleFile, dataset, specific, new KBANNSettings(randomGenerator, topGenSettings.getOmega(), topGenSettings.perturbationMagnitude()),softmaxOutputs);
        return new TopGen(kbann, randomGenerator);
    }

}
