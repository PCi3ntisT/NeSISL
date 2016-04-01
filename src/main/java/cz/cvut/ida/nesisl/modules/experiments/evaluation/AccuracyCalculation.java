package main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

/**
 * Created by EL on 1.4.2016.
 */
public class AccuracyCalculation {

    private final double accuracy;

    public AccuracyCalculation(double accuracy) {
        this.accuracy = accuracy;
    }

    public static AccuracyCalculation create(NeuralNetwork network, Dataset dataset) {
        return create(network, Tools.evaluateOnTestAllAndGetResults(dataset, network));
    }

    public static AccuracyCalculation create(NeuralNetwork network, Map<Sample, Results> evaluation) {
        int size = evaluation.size();
        if (!evaluation.isEmpty()) {
            size *= evaluation.entrySet().iterator().next().getKey().getOutput().size();
        }

        ToIntFunction<? super Map.Entry<Sample, Results>> accuracyComputer = (entry) ->
                (int) IntStream.range(0, entry.getKey().getOutput().size()).filter(idx -> Tools.isZero(entry.getKey().getOutput().get(idx).getValue() - network.getClassifier().classifyToDouble(entry.getValue().getComputedOutputs().get(idx)))).count();

        double acc = evaluation.entrySet().parallelStream().mapToInt(accuracyComputer).sum() / (1.0 * size);
        return new AccuracyCalculation(acc);
    }

    public double getAccuracy() {
        return accuracy;
    }
}
