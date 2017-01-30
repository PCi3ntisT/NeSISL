package main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.ArrayList;
import java.util.List;
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

    public static AccuracyCalculation createTest(NeuralNetwork network, Dataset dataset) {
        return create(network, Tools.evaluateOnTestAllAndGetResults(dataset, network));
    }

    public static AccuracyCalculation createTrain(NeuralNetwork network, Dataset dataset) {
        return create(network, Tools.evaluateOnTrainDataAllAndGetResults(dataset, network));
    }

    public static AccuracyCalculation createAll(NeuralNetwork network, Dataset dataset) {
        List<Sample> all = new ArrayList<>(dataset.getTrainData(network));
        all.addAll(dataset.getTestData(network));
        return create(network, Tools.evaluateAllAndGetResults(all,network));
    }

    public static AccuracyCalculation create(NeuralNetwork network, Map<Sample, Results> evaluation) {
        int size = evaluation.size();

        long correct = evaluation.entrySet()
                //.parallelStream()
                .stream()
                .filter(entry -> network.getClassifier().isCorrectlyClassified(entry.getKey().getOutput(),entry.getValue().getComputedOutputs()))
                .count();

        /*evaluation.entrySet()
                .stream()
                .forEach(entry -> {
                    entry.getKey().getOutput().forEach(e -> System.out.print("\t" + e.getValue()));
                    System.out.println();
                    entry.getValue().getComputedOutputs().forEach(e -> System.out.print("\t" + e));
                    System.out.println();
                    System.out.println("\t" + network.getClassifier().isCorrectlyClassified(entry.getKey().getOutput(),entry.getValue().getComputedOutputs()));
                });*/


        double acc = correct / (1.0 * size);
        return new AccuracyCalculation(acc);
    }

    /*public static AccuracyCalculation createTest(NeuralNetwork network, Map<Sample, Results> evaluation) {
        int size = evaluation.size();
        if (!evaluation.isEmpty()) {
            size *= evaluation.entrySet().iterator().next().getKey().getOutput().size();
        }

        ToIntFunction<? super Map.Entry<Sample, Results>> accuracyComputer = (entry) ->
                (int) IntStream.range(0, entry.getKey().getOutput().size())
                        .filter(idx -> Tools.isZero(entry.getKey().getOutput().get(idx).getValue() - network.getClassifier().classifyToDouble(entry.getValue().getComputedOutputs().get(idx))))
                        .count();

        double acc = evaluation.entrySet().parallelStream().mapToInt(accuracyComputer).sum() / (1.0 * size);
        return new AccuracyCalculation(acc);
    }*/

    public double getAccuracy() {
        return accuracy;
    }
}
