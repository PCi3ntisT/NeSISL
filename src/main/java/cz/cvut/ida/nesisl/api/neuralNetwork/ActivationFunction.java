package main.java.cz.cvut.ida.nesisl.api.neuralNetwork;

/**
 * Created by EL on 9.2.2016.
 */
public interface ActivationFunction {

    public double getValueAt(double x, Parameters parameter);
    public double getFirstDerivationAt(double x, Parameters parameter);
    public double getFirstDerivationAtFunctionValue(double functionValue, Parameters parameter);
    public String getName();

}
