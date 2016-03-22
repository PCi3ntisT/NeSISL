package main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by EL on 12.2.2016.
 */
public class Backpropagation {

    public static NeuralNetwork feedforwardBackpropagation(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        NeuralNetwork current = network.getCopy();
        feedforwardBackpropagationStateful(current, dataset, wls);
        return current;
    }

    public static void feedforwardBackpropagationStateful(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        learnWeights(Long.MAX_VALUE, network, dataset, wls);
    }

    private static void learnWeights(long numberOfLayersToBeLearned, NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {

        double error = Double.MAX_VALUE;
        double eps = Double.MAX_VALUE;

        long iteration = 0;

        while (iteration < wls.getEpochLimit()) { // eps > wls.getEpsilonDifference() // or other stopping criterion
            for (Sample sample : dataset.getTrainData(network)) {
                Pair<List<Double>, Results> resultDiff = Tools.computeErrorResults(network, sample.getInput(), sample.getOutput());
                updateWeights(network, resultDiff.getLeft(), resultDiff.getRight(), wls, numberOfLayersToBeLearned);
            }
            double currentError = Tools.computeSuqaredTotalError(network, dataset, wls);
            eps = Math.abs(error - currentError);
            error = currentError;
            iteration++;
        }

    }

    public static void updateWeights(NeuralNetwork network, List<Double> differenceExampleMinusOutput, Results results, WeightLearningSetting wls, long numberOfLayersToBeLearned) {
        Map<Node, Double> sigma = new HashMap<>();

        IntStream.range(0, differenceExampleMinusOutput.size()).forEach(idx -> {
            Node node = network.getOutputNodes().get(idx);
            Double value = node.getFirstDerivationAtX(results.getComputedOutputs().get(idx)) * differenceExampleMinusOutput.get(idx);
            sigma.put(node, value); // ten vypocet presunout do funkce, etc.
        });

        HashMap<Edge, Double> deltas = new HashMap<>();
        List<Node> nodeLayer;
        long layersComputed = 0;
        for (long layer = network.getMaximalNumberOfHiddenLayer() + 1; layer >= 0 && layersComputed < numberOfLayersToBeLearned; layer--) {

            if (layer > network.getMaximalNumberOfHiddenLayer()) {
                nodeLayer = network.getOutputNodes();
            } else {
                nodeLayer = network.getHiddenNodesInLayer(layer);
            }
            nodeLayer.forEach(node ->
                            network.getIncomingForwardEdges(node).stream().filter(Edge::isModifiable).forEach(edge -> {
                                Double value = wls.getLearningRate() * sigma.get(node) * results.getComputedValues().get(edge.getSource());
                                deltas.put(edge, value);
                            })
            );

            network.getHiddenNodesInLayer(layer - 1).forEach(node -> {
                Double sigmaSum = network.getOutgoingForwardEdges(node).stream().filter(Edge::isModifiable).mapToDouble(edge -> network.getWeight(edge) * sigma.get(edge.getTarget())).sum();
                sigma.put(node, sigmaSum * node.getFirstDerivationAtFunctionValue(results.getComputedValues().get(node)));
            });
            layersComputed++;
        }
        addDeltas(network, deltas);
    }

    private static NeuralNetwork addDeltas(NeuralNetwork network, Map<Edge, Double> deltas) {
        deltas.entrySet().forEach(entry -> {
            if (entry.getKey().isModifiable()) {
                Double value = network.getWeight(entry.getKey()) + entry.getValue();
                network.setEdgeWeight(entry.getKey(), value);
            }
        });
        return network;
    }


    public static void learnEdgesToOutputLayerOnlyStateful(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        learnWeights(1, network, dataset, wls);
    }
}
