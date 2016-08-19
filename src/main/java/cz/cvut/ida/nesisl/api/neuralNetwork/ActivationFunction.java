package main.java.cz.cvut.ida.nesisl.api.neuralNetwork;

import java.util.List;

/**
 * Created by EL on 9.2.2016.
 */
public interface ActivationFunction {



    public double getValueAt(double x, Parameters parameter, List<Double> otherInGroups);
    public double getFirstDerivationAt(double x, Parameters parameter, List<Double> otherInGroups);
    public double getFirstDerivationAtFunctionValue(double functionValue, Parameters parameter, List<Double> otherInGroups);
    public String getName();

}
