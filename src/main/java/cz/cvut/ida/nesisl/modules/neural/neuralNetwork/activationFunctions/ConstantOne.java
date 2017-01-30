package main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Parameters;

import java.util.List;

/**
 * Created by EL on 29.2.2016.
 */
public class ConstantOne implements ActivationFunction {
    private double VALUE = 1.0;

    private static ConstantOne constant = null;

    public static ActivationFunction getFunction() {
        if (null == constant) {
            constant = new ConstantOne();
        }
        return constant;
    }

    private ConstantOne() {
    }

    @Override
    public double getValueAt(double x, Parameters parameter, List<Double> otherInGroups) {
        return VALUE;
    }

    @Override
    public double getFirstDerivationAt(double x, Parameters parameter, List<Double> otherInGroups) {
        return 0;
    }

    @Override
    public double getFirstDerivationAtFunctionValue(double functionValue, Parameters parameter, List<Double> otherInGroups) {
        return 0;
    }

    @Override
    public String getName() {
        return "constant";
    }
}
