package main.java.cz.cvut.ida.nesisl.api.neuralNetwork;

import main.java.cz.cvut.ida.nesisl.api.data.Value;

import java.util.List;

/**
 * Created by EL on 8.3.2016.
 */
public interface MissingValues {

    public List<Double> processMissingValues(List<Value> input);
    public Double processMissingValue(Value input);
    public Value processMissingValueToValue(Value input);

}
