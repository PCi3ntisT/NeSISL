package main.java.cz.cvut.ida.nesisl.modules.algorithms.tresholdClassificator;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.dataset.Value;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by EL on 29.3.2016.
 */
public class ThresholdClassificator {

    private final Double treshold;

    public ThresholdClassificator(Double treshold) {
        this.treshold = treshold;
    }

    public Double getTreshold() {
        return treshold;
    }

    public Boolean classify(Value value) {
        return classify(value.getValue());
    }

    private Boolean classify(Double value) {
        return value > treshold;
    }

    private Double classifyToDouble(Value value) {
        return classifyToDouble(value.getValue());
    }

    private Double classifyToDouble(Double value) {
        return classify(value) ? 1.0d : 0.0d;
    }

    public static ThresholdClassificator create(NeuralNetwork network, Dataset dataset) {
        return create(Tools.evaluateAllAndGetResults(dataset, network));
    }

    public static ThresholdClassificator create(Map<Sample, Results> results) {
        List<Pair<Double, Boolean>> pairs = makePairs(results);
        Comparator<? super Pair<Double, Boolean>> comparator = (a, b) -> a.getLeft().compareTo(b.getLeft());
        Collections.sort(pairs, comparator);
        Double treshold = computeThreshold(pairs);
        return new ThresholdClassificator(treshold);
    }

    private static Double computeThreshold(List<Pair<Double, Boolean>> sortedPairs) {
        long positives = sortedPairs.stream().filter(p -> p.getRight()).count();
        long negatives = sortedPairs.size() - positives;
        long size = sortedPairs.size();
        double accuracy = 0.0d;
        double threshold = 0.0d;
        long truePositives = positives;
        long trueNegatives = 0l;

        for (Pair<Double, Boolean> pair : sortedPairs) {
            if(pair.getRight()){
                truePositives--;
            }else{
                trueNegatives++;
            }
            double currentAccuracy = truePositives * (positives * 1.0) / size + trueNegatives * (negatives * 1.0) / size;
            if(currentAccuracy > accuracy){
                threshold = pair.getLeft();
                accuracy = currentAccuracy;
            }
        }

        return threshold;
    }

    private static List<Pair<Double, Boolean>> makePairs(Map<Sample, Results> results) {
        // can be parallel stream
        return results.entrySet().stream().map(ThresholdClassificator::makePair).flatMap(l -> l).collect(Collectors.toCollection(ArrayList::new));
    }

    // Pair<outputValue,sampleOutput>
    private static Stream<Pair<Double, Boolean>> makePair(Map.Entry<Sample, Results> sampleResultsEntry) {
        // can be done in parallelStream
        return IntStream.range(0, sampleResultsEntry.getKey().getOutput().size()).mapToObj(idx -> constructPair(sampleResultsEntry.getKey(), sampleResultsEntry.getValue(), idx));
    }

    private static Pair<Double, Boolean> constructPair(Sample sample, Results result, int idx) {
        return new Pair<>(result.getComputedOutputs().get(idx), !Tools.isZero(sample.getOutput().get(idx).getValue()));
    }
}
