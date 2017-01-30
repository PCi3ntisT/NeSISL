package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.tresholdClassificator;

import main.java.cz.cvut.ida.nesisl.api.classifiers.Classifier;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;

import java.util.List;
import java.util.Map;

/**
 * Created by EL on 15.8.2016.
 */
public class SoftMaxClassifier implements Classifier {

    private SoftMaxClassifier() {
    }

    public static Classifier create(Map<Sample, Results> sampleResultsMap) {
        return new SoftMaxClassifier();
    }

    @Override
    public Double getThreshold() {
        return null;
    }

    @Override
    public Boolean classify(Value value) {
        return null;
    }

    @Override
    public Boolean classify(Double value) {
        return null;
    }

    @Override
    public String classifyToOneZero(Value value) {
        return null;
    }

    @Override
    public String classifyToOneZero(double value) {
        return null;
    }

    @Override
    public Double classifyToDouble(Value value) {
        return null;
    }

    @Override
    public Double classifyToDouble(Double value) {
        return null;
    }

    @Override
    public Boolean isCorrectlyClassified(List<Value> sampleOutputs, List<Double> computedOutputs) {
        int maxIdx = 0;
        double max = Double.MIN_VALUE;
        for (int idx = 0; idx < sampleOutputs.size(); idx++) {
            Double value = sampleOutputs.get(idx).getValue();
            if (value > max) {
                max = value;
                maxIdx = idx;
            }
        }

        for (int idx = 0; idx < computedOutputs.size(); idx++) {
            if (maxIdx != idx && computedOutputs.get(idx) > computedOutputs.get(maxIdx)) {
                return false;
            }
        }
        return true;
    }


}
