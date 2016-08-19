package main.java.cz.cvut.ida.nesisl.api.classifiers;

import main.java.cz.cvut.ida.nesisl.api.data.Value;

import java.util.List;

/**
 * Created by EL on 30.3.2016.
 */
public interface Classifier {

    public Double getThreshold();

    public Boolean classify(Value value);

    public Boolean classify(Double value);

    public String classifyToOneZero(Value value);

    public String classifyToOneZero(double value);

    public Double classifyToDouble(Value value);

    public Double classifyToDouble(Double value);

    public Boolean isCorrectlyClassified(List<Value> sampleOutputs, List<Double> computedOutputs);
}
