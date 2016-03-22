package main.java.cz.cvut.ida.nesisl.api.neuralNetwork;


import main.java.cz.cvut.ida.nesisl.modules.dataset.Value;

/**
 * Created by EL on 9.2.2016.
 */
public interface Node {

    public Double getValue(Value value);

    public Double getValue(double x);

    public double getFirstDerivationAtX(double x);

    public double getFirstDerivationAtFunctionValue(double x);

    public Parameters getParameters();

    public String getName();

    public void setName(String name);

    public void setParameters(Parameters parameters);

    public ActivationFunction getActivationFunction();

    public Long getIndex();

    public void setModifiability(Boolean modifiable);

    public Boolean isModifiable();
}
