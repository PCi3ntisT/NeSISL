package main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.ArrayList;
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
        long iteration = 0;

        Map<Sample, Map<Edge, Double>> previousDeltas = initPreviousDeltas(dataset, network);
        List<Double> errors = new ArrayList<>();

        while (wls.canContinueBackpropagation(iteration, errors)) {
            for (Sample sample : dataset.getTrainData(network)) {
                Pair<List<Double>, Results> resultDiff = Tools.computeErrorResults(network, sample.getInput(), sample.getOutput());
                Map<Edge, Double> currentDeltas = updateWeights(network, resultDiff.getLeft(), resultDiff.getRight(), wls, numberOfLayersToBeLearned, previousDeltas.get(sample));
                previousDeltas.put(sample, currentDeltas);
            }
            double currentError = Tools.computeSquaredTrainTotalError(network, dataset) + Tools.computePenalty(network, wls.getPenaltyEpsilon(), wls.getSLFThreshold());
            errors.add(currentError);
            iteration++;
        }
    }

    public static Map<Sample, Map<Edge, Double>> initPreviousDeltas(Dataset dataset, NeuralNetwork network) {
        Map<Edge, Double> inner = new HashMap<>();
        network.getWeights().keySet().stream().forEach(edge -> inner.put(edge, 0.0d));

        /*long nullWeights = network.getWeights().keySet().stream().filter(edge -> network.getWeight(edge) == null).count();
        if(nullWeights > 0){
            throw new IllegalStateException();
        }*/

        Map<Sample, Map<Edge, Double>> result = new HashMap<>();
        dataset.getTrainData(network).forEach(sample -> result.put(sample, new HashMap<>(inner)));

        return result;
    }

    public static Map<Edge, Double> updateWeights(NeuralNetwork network, List<Double> differenceExampleMinusOutput, Results results, WeightLearningSetting wls, long numberOfLayersToBeLearned, Map<Edge, Double> previousDeltas) {
        Map<Node, Double> sigma = new HashMap<>();

        IntStream.range(0, differenceExampleMinusOutput.size()).forEach(idx -> {
            Node node = network.getOutputNodes().get(idx);
            Double value = node.getFirstDerivationAtX(results.getComputedOutputs().get(idx)) * differenceExampleMinusOutput.get(idx);
            sigma.put(node, value);
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
                                double previousDelta = 0.0d;
                                if (previousDeltas.containsKey(edge)) {
                                    previousDelta = previousDeltas.get(edge);
                                }
                                // delta with learning and momentum
                                Double value = wls.getLearningRate() * sigma.get(node) * results.getComputedValues().get(edge.getSource())
                                        + wls.getMomentumAlpha() * previousDelta;

                                // edge zeoring
                                Double edgeWeight = network.getWeight(edge);
                                Double edgeAbsoluteWeight = Math.abs(edgeWeight);
                                if (edgeAbsoluteWeight < wls.getSLFThreshold()) {
                                    Double penaltyTerm = wls.getLearningRate() * wls.getPenaltyEpsilon() * Math.signum(edgeWeight);
                                    value = value - penaltyTerm;
                                }

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
        return deltas;
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
