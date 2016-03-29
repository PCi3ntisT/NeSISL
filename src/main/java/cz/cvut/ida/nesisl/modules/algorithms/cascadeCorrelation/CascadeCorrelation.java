package main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by EL on 8.3.2016.
 */
public class CascadeCorrelation {

    private NeuralNetwork network;
    private RandomGenerator randomGenerator;

    public CascadeCorrelation(List<Fact> inputFactOrder, List<Fact> outputFactOrder, RandomGenerator randomGenerator) {
        this.network = constructNetwork(inputFactOrder, outputFactOrder, new MissingValueKBANN(), randomGenerator);
        this.randomGenerator = randomGenerator;
    }

    public static NeuralNetwork constructNetwork(List<Fact> inputFactOrder, List<Fact> outputFactOrder, MissingValues missingValues, RandomGenerator randomGenerator) {
        List<Node> inputs = NodeFactory.generateNodes(inputFactOrder, Identity.getFunction());
        List<Node> output = NodeFactory.generateNodes(outputFactOrder, Sigmoid.getFunction());
        NeuralNetwork network = new NeuralNetworkImpl(inputs, output, missingValues);

        inputs.add(network.getBias());
        Tools.makeFullInterLayerForwardConnections(inputs,output,network,randomGenerator);

        return network;
    }

    /**
     * Returns neural network; (statefull).
     *
     * @return
     */
    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    public void learn(Dataset dataset, WeightLearningSetting wls, CascadeCorrelationSetting cascadeCorrelationSetting) {

        long numberOfAddedNodes = 0;
        double epochDifference = Double.MAX_VALUE;
        double error = Double.MAX_VALUE;
        double currentError = Double.MAX_VALUE;
        while (true) {

            Backpropagation.learnEdgesToOutputLayerOnlyStateful(network, dataset, wls);
            currentError = Tools.computeSuqaredTotalError(network, dataset, wls);
            epochDifference = Math.abs(error - currentError);
            error = currentError;

            if (numberOfAddedNodes > cascadeCorrelationSetting.getMaximumNumberOfHiddenNodes() || epochDifference < wls.getEpsilonDifference()) {
                break;
            }

            // pridani dalsiho uzlu
            CandidateWrapper bestCandidate = IntStream.range(0, cascadeCorrelationSetting.getSizeOfCasCorPool()).parallel().mapToObj(i -> makeAndLearnCandidate(network, dataset, randomGenerator, wls))
                    .max(CandidateWrapper::compare).get();

            this.network.addNodeAtLayerStateful(bestCandidate.getNode(), this.network.getMaximalNumberOfHiddenLayer() + 1);
            bestCandidate.getEdgeWeightPairs().forEach(pair -> this.network.addEdgeStateful(pair.getLeft(), pair.getRight()));
            this.network.getOutputNodes().forEach(node -> this.network.addEdgeStateful(bestCandidate.getNode(), node, randomGenerator.nextDouble(), Edge.Type.FORWARD));

            numberOfAddedNodes++;
        }
    }

    private static CandidateWrapper makeAndLearnCandidate(NeuralNetwork network, Dataset dataset, RandomGenerator randomGenerator, WeightLearningSetting wls) {
        Node node = NodeFactory.create(Sigmoid.getFunction());
        Set<Pair<Edge, Double>> edges = new HashSet<>();

        network.getHiddenNodes().forEach(source -> generateAndAddEdge(edges, source, node, randomGenerator));
        network.getInputNodes().forEach(source -> generateAndAddEdge(edges, source, node, randomGenerator));
        edges.add(generateRandomEdgeWeight(network.getBias(), node, randomGenerator));

        Pair<Set<Pair<Edge, Double>>, Double> result = learnCandidatesConnections(dataset, node, edges, network, wls);

        return new CandidateWrapper(result.getRight(), result.getLeft(), node);
    }

    private static Pair<Set<Pair<Edge, Double>>, Double> learnCandidatesConnections(Dataset dataset, Node node, Set<Pair<Edge, Double>> edges, NeuralNetwork network, WeightLearningSetting wls) {
        Double correlation = Double.MIN_VALUE;
        Map<Sample, Results> cache = Tools.evaluateAllAndGetResults(dataset, network); // tohle by asi nebylo od veci cachovat nekde na vyssi urovni, kdyz to stejnak pouzivaji vsichni
        Set<Pair<Edge, Double>> currentEdges = edges;
        Double epsilon = Double.MAX_VALUE;
        while (true) {
            Pair<Double, Map<Sample, Double>> candidateAverageOutputs = computeAverage(edges, cache, dataset, network);
            Pair<Double, Set<Pair<Edge, Double>>> current = computeCorrelationAndComputeWeights(node, candidateAverageOutputs.getLeft(), candidateAverageOutputs.getRight(), network, dataset, currentEdges, wls);

            epsilon = Math.abs(correlation - current.getLeft());
            correlation = current.getLeft();

            currentEdges = current.getRight();

            if (wls.getEpsilonDifference() > epsilon) {
                break;
            }
        }

        return new Pair<>(currentEdges, correlation);
    }


    private static Pair<Double, Map<Sample, Double>> computeAverage(Set<Pair<Edge, Double>> edges, Map<Sample, Results> cache, Dataset dataset, NeuralNetwork network) {
        Map<Sample, Double> map = new HashMap<>();

        dataset.getTrainData(network).forEach(sample -> {
            Double currentValue = computeCandidateValueForSampleAsInput(sample, edges, cache);
            map.put(sample, currentValue);
        });

        Double average = map.entrySet().parallelStream().mapToDouble(entry -> entry.getValue()).average().orElse(0);
        return new Pair<>(average, map);
    }

    private static Double computeCandidateValueForSampleAsInput(Sample sample, Set<Pair<Edge, Double>> edges, Map<Sample, Results> cache) {
        Results currentResults = cache.get(sample);
        return edges.parallelStream().mapToDouble(pair ->
                        currentResults.getComputedValues().get(pair.getLeft().getSource()) * pair.getRight()
        ).sum();
    }

    private static void generateAndAddEdge(Set<Pair<Edge, Double>> edges, Node source, Node target, RandomGenerator randomGenerator) {
        edges.add(generateRandomEdgeWeight(source, target, randomGenerator));
    }

    private static Pair<Edge, Double> generateRandomEdgeWeight(Node source, Node target, RandomGenerator randomGenerator) {
        return new Pair<>(new Edge(source, target, Edge.Type.FORWARD, true), randomGenerator.nextDouble());
    }


    private static Pair<Double, Set<Pair<Edge, Double>>> computeCorrelationAndComputeWeights(Node node, Double averageCandidateOutput, Map<Sample, Double> sampleCandidateOutput, NeuralNetwork network, Dataset dataset, Set<Pair<Edge, Double>> originalEdges, WeightLearningSetting wls) {
        List<Double> outputAverages = dataset.getAverageOutputs(network);
        Map<Integer, Double> outputs = new HashMap<>();
        Set<Pair<Edge, Double>> edges = new HashSet<>();
        IntStream.range(0, outputAverages.size()).forEach(idx -> outputs.put(idx, 0.0d));
        dataset.getTrainData(network).parallelStream()
                .map(sample -> computeCascadeError(averageCandidateOutput, sampleCandidateOutput, outputAverages, sample))
                .forEachOrdered(multiples -> IntStream.range(0, multiples.size()).forEach(idx -> {
                    Double newValue = multiples.get(idx) + outputs.get(idx);
                    outputs.put(idx, newValue);
                }));
        double correlation = outputs.entrySet().parallelStream().mapToDouble(entry -> Math.abs(entry.getValue())).sum();

        Map<Sample, Results> cache = Tools.evaluateAllAndGetResults(dataset, network);

        originalEdges.forEach(pair -> {
            Double derivative = dataset.getTrainData(network).parallelStream().mapToDouble(sample -> {
                double nodeInput = originalEdges.parallelStream().mapToDouble(entry ->
                                cache.get(sample).getComputedValues().get(entry.getLeft().getSource()) * entry.getRight()
                ).sum();

                return IntStream.range(0, sample.getOutput().size()).mapToDouble(idx ->
                                Math.signum(sample.getOutput().get(idx).getValue() - outputAverages.get(idx)) * node.getFirstDerivationAtX(nodeInput) * cache.get(sample).getComputedValues().get(pair.getLeft().getSource())
                ).sum();
            }).sum();
            Double value = pair.getRight() + wls.getLearningRate() * derivative;
            edges.add(new Pair<>(pair.getLeft(), value));
        });

        return new Pair<>(correlation, edges);
    }

    // vraci list hodnot outputu
    private static List<Double> computeCascadeError(Double averageCandidateOutput, Map<Sample, Double> sampleCandidateOutput, List<Double> outputAverages, Sample sample) {
        Double deltaCandidate = sampleCandidateOutput.get(sample) - averageCandidateOutput;
        List<Double> list = new ArrayList<>();
        IntStream.range(0, outputAverages.size()).forEachOrdered(idx -> {
            Double value = deltaCandidate * (sample.getOutput().get(idx).getValue() - outputAverages.get(idx));
            list.add(value);
        });
        return list;
    }

}
