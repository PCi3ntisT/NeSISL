package main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Parameters;

/**
 * Created by EL on 9.2.2016.
 */
public class Sigmoid implements ActivationFunction {

    private static Sigmoid sigmoid = null;

    public static ActivationFunction getFunction() {
        if (null == sigmoid) {
            sigmoid = new Sigmoid();
        }
        return sigmoid;
    }

    private Sigmoid() {
    }


    @Override
    public double getValueAt(double x, Parameters parameter) {
        return 1.0 / (1.0 + Math.exp(- x));
    }

    @Override
    public double getFirstDerivationAt(double x, Parameters parameter) {
        return getFirstDerivationAtFunctionValue(getValueAt(x,parameter),parameter);
    }

    @Override
    public double getFirstDerivationAtFunctionValue(double functionValue, Parameters parameter) {
        return functionValue * (1 - functionValue);
    }

    @Override
    public String getName() {
        return "sigmoid";
    }
}
