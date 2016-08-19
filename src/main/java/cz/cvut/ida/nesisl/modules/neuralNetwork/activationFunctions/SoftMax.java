package main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Parameters;

import java.util.List;

/**
 * Created by EL on 15.8.2016.
 */
public class SoftMax implements ActivationFunction {


    private static SoftMax softmax = null;

    public static ActivationFunction getFunction() {
        if (null == softmax) {
            softmax = new SoftMax();
        }
        return softmax;
    }

    private SoftMax() {
    }


    @Override
    public double getValueAt(double x, Parameters parameter, List<Double> otherInGroups) {
        double myValue = Math.exp(x);
        return myValue / (myValue + otherInGroups.stream().mapToDouble(z -> Math.exp(z)).sum());
    }

    @Override
    public double getFirstDerivationAt(double x, Parameters parameter, List<Double> otherInGroups) {
        double myValue = getValueAt(x, parameter, otherInGroups);
        return getFirstDerivationAtFunctionValue(myValue, parameter, otherInGroups);
    }


    @Override
    public double getFirstDerivationAtFunctionValue(double functionValue, Parameters parameter, List<Double> otherInGroups) {
        return functionValue * (1 - functionValue);
    }

    public double getCrossentropyDerivationAtFunctionValue(double functionValue, double targetValue, Parameters parameters) {
        return functionValue - targetValue;
    }

    public double getCrossentropyDerivationAt(double x, double targetValue, Parameters parameters, List<Double> otherInGroups) {
        return getValueAt(x,parameters,otherInGroups) - targetValue;
    }

    @Override
    public String getName() {
        return "softmax";
    }
}
