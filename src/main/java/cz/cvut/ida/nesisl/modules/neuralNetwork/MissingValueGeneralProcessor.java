package main.java.cz.cvut.ida.nesisl.modules.neuralNetwork;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.MissingValues;
import main.java.cz.cvut.ida.nesisl.api.data.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by EL on 8.3.2016.
 */
public class MissingValueGeneralProcessor implements MissingValues {
    private final Double nanDoubleValue;
    private final Value nanValueReplacement;

    public MissingValueGeneralProcessor(Double nanDoubleValue) {
        this.nanDoubleValue = nanDoubleValue;
        this.nanValueReplacement = new Value(nanDoubleValue);
    }

    @Override
    public List<Double> processMissingValues(List<Value> input) {
        List<Double> result = new ArrayList<>();
        input.forEach(this::processMissingValue);
        return result;
    }

    @Override
    public Double processMissingValue(Value input) {
        return processMissingValueToValue(input).getValue();
    }

    @Override
    public Value processMissingValueToValue(Value input) {
        //System.out.println(":"+input.getValue());
        if (input.isNaN()) {
            //System.out.println(nanValueReplacement.getValue());
            //System.exit(-10);
            return nanValueReplacement;
        }
        return input;
    }
}
