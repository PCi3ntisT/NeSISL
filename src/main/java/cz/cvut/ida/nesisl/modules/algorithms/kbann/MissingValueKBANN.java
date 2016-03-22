package main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.MissingValues;
import main.java.cz.cvut.ida.nesisl.modules.dataset.Value;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.MissingValueGeneralProcessor;

import java.util.List;

/**
 * Created by EL on 8.3.2016.
 */
public class MissingValueKBANN implements MissingValues {

    private final MissingValueGeneralProcessor processor;

    public MissingValueKBANN() {
        this.processor = new MissingValueGeneralProcessor(0.5d);
    }

    @Override
    public List<Double> processMissingValues(List<Value> input) {
        return processor.processMissingValues(input);
    }

    @Override
    public Double processMissingValue(Value input) {
        return processor.processMissingValue(input);
    }

    @Override
    public Value processMissingValueToValue(Value input) {
        return processor.processMissingValueToValue(input);
    }
}
