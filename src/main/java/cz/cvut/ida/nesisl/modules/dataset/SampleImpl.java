package main.java.cz.cvut.ida.nesisl.modules.dataset;

import main.java.cz.cvut.ida.nesisl.api.data.Sample;

import java.util.List;

/**
 * Created by EL on 7.3.2016.
 */
public class SampleImpl implements Sample {
    private final List<Value> input;
    private final List<Value> output;

    public SampleImpl(List<Value> input, List<Value> output) {
        this.input = input;
        this.output = output;
    }

    public List<Value> getInput() {
        return input;
    }

    public List<Value> getOutput() {
        return output;
    }
}
