package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by EL on 9.3.2016.
 */
public class Quickprop {

    public static NeuralNetwork learnEdgesToOutputLayerOnly(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        NeuralNetwork current = network.getCopy();
        learnEdgesToOutputLayerOnlyStateful(current, dataset, wls);
        return current;
    }

    public static Double learnEdgesToOutputLayerOnlyStateful(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        double error = Double.MAX_VALUE;
        double previousError = 0.0d;
        double eps = Double.MAX_VALUE;
        Map<Edge, Double> previousPartialDerivates = new HashMap<>();
        Map<Edge, Double> zeros = new HashMap<>();

        double initPartialDerivates = 0.0d;
        Double initialDeltas = 0.0d;
        for (Map.Entry<Edge, Double> entry : network.getWeights().entrySet()) {
            previousPartialDerivates.put(entry.getKey(), initPartialDerivates);
            zeros.put(entry.getKey(), initialDeltas);
        }


        long iteration = 0;

        while (eps > wls.getEpsilonConvergent()) { // pridat stopku po urcitem poctu iteraci (to same pridat do BP)
            iteration++;
            System.out.println("iteration\t" + iteration);

            Map<Edge, Double> previousWeightDeltas = zeros;

            for (Sample sample : dataset.getTrainData(network)) {
                Pair<List<Double>, Results> resultDiff = Tools.computeErrorResults(network, sample.getInput(), sample.getOutput());
                Pair<Map<Edge, Double>, Map<Edge, Double>> current = updateWeights(network, resultDiff.getLeft(), resultDiff.getRight(), previousPartialDerivates, previousWeightDeltas, wls);
                previousPartialDerivates = current.getLeft();
                previousWeightDeltas = current.getRight();
            }

            throw new NotImplementedException();
            //error = Tools.computeSquaredTrainTotalError(network, dataset, wls);
            //System.out.println(error);

            //eps = Math.abs(error - previousError);
            //previousError = error;
            //System.out.println(iteration + "it:\t" + error);
        }
        return error;
    }

    private static Pair<Map<Edge, Double>, Map<Edge, Double>> updateWeights(NeuralNetwork network, List<Double> differenceSampleMinusOutput, Results results, Map<Edge, Double> previousPartialDerivates, Map<Edge, Double> previousWeightDeltas, WeightLearningSetting wls) {
        Map<Node, Double> sigmas = new HashMap<>();
        Map<Edge, Double> partialDerivatives = new HashMap<>();
        IntStream.range(0, differenceSampleMinusOutput.size()).forEach(idx -> {
            Node node = network.getOutputNodes().get(idx);
            // just blaffing
            Double partial = node.getFirstDerivationAtX(node.getFirstDerivationAtX(results.getComputedOutputs().get(idx),null),null) * differenceSampleMinusOutput.get(idx);
            sigmas.put(node, partial); // ten vypocet presunout do funkce, etc.
        });

        Map<Edge, Double> weightDeltas = new HashMap<>();
        List<Node> nodeLayer = network.getOutputNodes();
        nodeLayer.forEach(node ->
                        network.getIncomingForwardEdges(node).forEach(edge -> {
                            Double currentPartialDerivative = sigmas.get(node) * results.getComputedValues().get(edge.getSource());
                            partialDerivatives.put(edge, currentPartialDerivative);
                            updateWeight(edge, currentPartialDerivative, previousPartialDerivates.get(edge), previousWeightDeltas.get(edge), weightDeltas, network, wls);
                        })
        );

        return new Pair<>(partialDerivatives, weightDeltas);
    }

    private static void updateWeight(Edge edge, Double currentPartialDerivative, Double previousPartialDerivatives, Double previousWeightDelta, Map<Edge, Double> weightDeltas, NeuralNetwork network, WeightLearningSetting wls) {
        throw new NotImplementedException();
        /*
        Double alfa = currentPartialDerivative / (previousPartialDerivatives - currentPartialDerivative);

        if (alfa > wls.getMaxAlpha() || alfa * previousWeightDelta * currentPartialDerivative > 0) {
            alfa = wls.getMaxAlpha();
        }

        Double epsilon = 0.0d;
        if (currentPartialDerivative * previousWeightDelta < 0 || Tools.isZero(previousWeightDelta)) {
            epsilon = wls.getQuickpropEpsilon();
        }

        double edgeUpdate = -epsilon * currentPartialDerivative + alfa * previousWeightDelta;
        double finalValue = network.getWeight(edge) + edgeUpdate;
        // tady by mohla byt jeste ochrana proti moc velkym hodnotam
        weightDeltas.put(edge, edgeUpdate);
        network.setEdgeWeight(edge, finalValue);
        */
    }

}
