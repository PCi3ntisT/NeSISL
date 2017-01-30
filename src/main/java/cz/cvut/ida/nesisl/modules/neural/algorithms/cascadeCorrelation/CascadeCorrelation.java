package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.cascadeCorrelation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.SoftMax;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Created by EL on 8.3.2016.
 */
public class CascadeCorrelation implements NeuralNetworkOwner {

    private NeuralNetwork network;
    private RandomGenerator randomGenerator;

    public CascadeCorrelation(NeuralNetwork network, RandomGenerator randomGenerator) {
        this.network = network;
        this.randomGenerator = randomGenerator;
    }

    public static NeuralNetwork constructNetwork(List<Fact> inputFactOrder, List<Fact> outputFactOrder, MissingValues missingValues, RandomGenerator randomGenerator, boolean softmaxOutputs) {
        List<Node> inputs = NodeFactory.generateNodes(inputFactOrder, Identity.getFunction());
        // automatci creation of softmax when multiclass classification
        ActivationFunction outputFce = (softmaxOutputs) ? SoftMax.getFunction() : Sigmoid.getFunction();
        //ActivationFunction outputFce = Sigmoid.getFunction();
        List<Node> output = NodeFactory.generateNodes(outputFactOrder, outputFce);
        NeuralNetwork network = new NeuralNetworkImpl(inputs, output, missingValues,softmaxOutputs);

        inputs.add(network.getBias());
        Tools.makeFullInterLayerForwardConnections(inputs, output, network, randomGenerator);

        return network;
    }

    /**
     * Returns neural network; (stateful).
     *
     * @return
     */
    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    public NeuralNetwork learn(Dataset dataset, WeightLearningSetting wls, CascadeCorrelationSetting cascadeCorrelationSetting) {
        long numberOfAddedNodes = 0;
        List<Double> errors = new ArrayList<>();

        while (true) {
            Backpropagation.learnEdgesToOutputLayerOnlyStateful(network, dataset, wls);
            double currentError = Tools.computeAverageSquaredTrainTotalError(network, dataset);
            errors.add(currentError);

            if (cascadeCorrelationSetting.stopCascadeCorrelation(numberOfAddedNodes, errors)) {
                break;
            }

            CandidateWrapper bestCandidate = LongStream
                    .range(0, cascadeCorrelationSetting.getSizeOfCasCorPool())
                    //.parallel()
                    .mapToObj(i -> makeAndLearnCandidate(network, dataset, randomGenerator, wls, cascadeCorrelationSetting))
                    .max(CandidateWrapper::compare).get();

            addCandidateToNetwork(bestCandidate, network);

            numberOfAddedNodes++;

            //System.out.println(numberOfAddedNodes + "\t" + currentError);
        }
        this.network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        return this.network;
    }

    private void addCandidateToNetwork(CandidateWrapper bestCandidate, NeuralNetwork network) {
        network.addNodeAtLayerStateful(bestCandidate.getNode(), network.getMaximalNumberOfHiddenLayer() + 1);
        bestCandidate.getEdgeWeightPairs().forEach(pair -> network.addEdgeStateful(pair.getLeft(), pair.getRight()));
        network.getOutputNodes().forEach(node -> network.addEdgeStateful(bestCandidate.getNode(), node, randomGenerator.nextDouble(), Edge.Type.FORWARD));
    }

    private static CandidateWrapper makeAndLearnCandidate(NeuralNetwork network, Dataset dataset, RandomGenerator randomGenerator, WeightLearningSetting wls, CascadeCorrelationSetting cascadeCorrelationSetting) {
        Node node = NodeFactory.create(Sigmoid.getFunction());
        Set<Pair<Edge, Double>> edges = makeConnectionsNonOutputNodesToCandidate(network, randomGenerator, node);

        Map<Sample, Results> cache = Tools.evaluateOnTrainDataAllAndGetResults(dataset, network);

        Pair<Set<Pair<Edge, Double>>, Double> result = learnCandidatesConnections(dataset, node, edges, network, cache, wls, cascadeCorrelationSetting);

        return new CandidateWrapper(result.getRight(), result.getLeft(), node);
    }

    private static Set<Pair<Edge, Double>> makeConnectionsNonOutputNodesToCandidate(NeuralNetwork network, RandomGenerator randomGenerator, Node node) {
        Set<Pair<Edge, Double>> edges = new HashSet<>();
        network.getHiddenNodes().forEach(source -> generateAndAddEdge(edges, source, node, randomGenerator));
        network.getInputNodes().forEach(source -> generateAndAddEdge(edges, source, node, randomGenerator));
        edges.add(generateRandomEdgeWeight(network.getBias(), node, randomGenerator));
        return edges;
    }

    private static Pair<Set<Pair<Edge, Double>>, Double> learnCandidatesConnections(Dataset dataset, Node node, Set<Pair<Edge, Double>> edges, NeuralNetwork network, Map<Sample, Results> cache, WeightLearningSetting wls, CascadeCorrelationSetting cascadeCorrelationSetting) {
        Set<Pair<Edge, Double>> currentEdges = edges;
        List<Double> correlations = new ArrayList<>();
        long iteration = 0l;
        Double correlation;
        while (true) {
            Pair<Double, Map<Sample, Double>> candidateAverageOutputs = computeAverage(currentEdges, cache, dataset, network);
            Pair<Double, Set<Pair<Edge, Double>>> current = computeCorrelationAndComputeWeights(node, candidateAverageOutputs.getLeft(), candidateAverageOutputs.getRight(), network, dataset, currentEdges, wls);

            correlation = current.getLeft();
            correlations.add(correlation);
            currentEdges = current.getRight();

            if (cascadeCorrelationSetting.canStopLearningCandidatConnection(correlations, iteration)) {
                break;
            }
            iteration++;
            //System.out.println("\t" + iteration + "\t" + correlation);
        }

        return new Pair<>(currentEdges, correlation);
    }


    private static Pair<Double, Map<Sample, Double>> computeAverage(Set<Pair<Edge, Double>> edges, Map<Sample, Results> cache, Dataset dataset, NeuralNetwork network) {
        Map<Sample, Double> map = new HashMap<>();

        dataset.getTrainData(network).forEach(sample -> {
            Double currentValue = computeCandidateValueForSampleAsInput(sample, edges, cache);
            map.put(sample, currentValue);
        });

        Double average = map.entrySet()
                //.parallelStream()
                .stream()
                .mapToDouble(entry -> entry.getValue())
                .average()
                .orElse(0);
        return new Pair<>(average, map);
    }

    private static Double computeCandidateValueForSampleAsInput(Sample sample, Set<Pair<Edge, Double>> edges, Map<Sample, Results> cache) {
        Results currentResults = cache.get(sample);
        return edges
                //.parallelStream()
                .stream()
                .mapToDouble(pair ->
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

        // correlation computation; possible, it could be computed after the weights are modified
        dataset.getTrainData(network)
                .stream()
                .map(sample -> computeCascadeError(averageCandidateOutput, sampleCandidateOutput, outputAverages, sample))
                .forEachOrdered(multiples ->
                        IntStream.range(0, multiples.size())
                                .forEach(idx -> {
                                    Double newValue = multiples.get(idx) + outputs.get(idx);
                                    outputs.put(idx, newValue);
                                }));
        double correlation = outputs.entrySet()
                //.parallelStream()
                .stream()
                .mapToDouble(entry -> Math.abs(entry.getValue()))
                .sum();

        Map<Sample, Results> cache = Tools.evaluateOnTrainDataAllAndGetResults(dataset, network);

        Map<Integer,Double> outputNodeCorrelation = computeCorrelationsAccordingToOutput(averageCandidateOutput, sampleCandidateOutput, outputAverages, cache);

        // adjusting weights (probably)
        originalEdges.forEach(pair -> {
            Double derivative = dataset.getTrainData(network)
                    //.parallelStream()
                    .stream()
                    .mapToDouble(sample -> {
                        double nodeInput = originalEdges
                                //.parallelStream()
                                .stream()
                                .mapToDouble(entry ->
                                                cache.get(sample).getComputedValues().get(entry.getLeft().getSource())
                                                        * entry.getRight()
                                ).sum();

                        return IntStream.range(0, sample.getOutput().size())
                                .mapToDouble(idx ->
                                                outputNodeCorrelation.get(idx)
                                                        * (sample.getOutput().get(idx).getValue() - outputAverages.get(idx))
                                                        * sample.getOutput().get(idx).getValue() - outputAverages.get(idx)
                                                        * node.getFirstDerivationAtX(nodeInput, null)
                                                        * cache.get(sample).getComputedValues().get(pair.getLeft().getSource())
                                ).sum();
                    }).sum();
            Double value = pair.getRight() + wls.getLearningRate() * derivative;
            edges.add(new Pair<>(pair.getLeft(), value));
        });
        return new Pair<>(correlation, edges);
    }

    // awful
    private static Map<Integer, Double> computeCorrelationsAccordingToOutput(Double averageCandidateOutput, Map<Sample, Double> sampleCandidateOutput, List<Double> outputAverages, Map<Sample, Results> cache) {
        Map<Integer,Double> map = new HashMap<>();
        IntStream.range(0,outputAverages.size())
                .forEach(outputNodeIdx -> {
                    double correlation =  sampleCandidateOutput.entrySet().stream()
                            .mapToDouble(entry -> {
                                Sample sample = entry.getKey();
                                double candidateSampleValue = entry.getValue();
                                double sampleOutputError = cache.get(sample).getComputedOutputs().get(outputNodeIdx);
                                return (candidateSampleValue - averageCandidateOutput)
                                        * (sampleOutputError - outputAverages.get(outputNodeIdx));
                            })
                            .sum();
                    map.put(outputNodeIdx,correlation);
                });
        return map;
    }

    // returns list of values of outputs
    private static List<Double> computeCascadeError(Double averageCandidateOutput, Map<Sample, Double> sampleCandidateOutput, List<Double> outputAverages, Sample sample) {
        Double deltaCandidate = sampleCandidateOutput.get(sample) - averageCandidateOutput;
        List<Double> list = new ArrayList<>();
        IntStream.range(0, outputAverages.size())
                .forEachOrdered(idx -> {
                    Double value = deltaCandidate * (sample.getOutput().get(idx).getValue() - outputAverages.get(idx));
                    list.add(value);
                });
        return list;
    }

    public static CascadeCorrelation create(List<Fact> inputFactOrder, List<Fact> outputFactOrder, RandomGeneratorImpl randomGenerator, MissingValues missingValuesProcessor, boolean softmaxOutputs) {
        NeuralNetwork network = constructNetwork(inputFactOrder, outputFactOrder, missingValuesProcessor, randomGenerator, softmaxOutputs);
        return new CascadeCorrelation(network, randomGenerator);
    }
}
