package main.java.cz.cvut.ida.nesisl.modules.neuralNetwork;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Parameters;
import main.java.cz.cvut.ida.nesisl.modules.dataset.Value;

/**
 * Created by EL on 9.2.2016.
 */
public class NodeImpl implements Node {

    private final ActivationFunction function;
    private Parameters parameters = new Parameters();
    private final long index;
    private String name = "";
    private Boolean isModifiable = true;

    public NodeImpl(ActivationFunction activationFunction, Parameters parameters, long index) {
        this.function = activationFunction;
        this.parameters = parameters;
        this.index = index;
    }

    /*public NodeImpl(ActivationFunction function) {
        this.function = function;
    }

    public NodeImpl(ActivationFunction function, Parameters parameters) {
        this.function = function;
        this.parameters = parameters;
    }*/

    @Override
    public Double getValue(Value value) {
        return getValue(value.getValue());
    }

    @Override
    public Double getValue(double x) {
        return function.getValueAt(x, parameters);
    }

    @Override
    public double getFirstDerivationAtX(double x) {
        return function.getFirstDerivationAt(x, parameters);
    }

    @Override
    public double getFirstDerivationAtFunctionValue(double x) {
        return function.getFirstDerivationAtFunctionValue(x,parameters);
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public ActivationFunction getActivationFunction() {
        return this.function;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeImpl)) return false;

        NodeImpl node = (NodeImpl) o;

        if (index != node.index) return false;
        if (!function.equals(node.function)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = function.hashCode();
        result = 31 * result + (int) (index ^ (index >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "NodeImpl{" +
                "function=" + function +
                //", parameters=" + parameters +
                ", index=" + index +
                ", name='" + name + '\'' +
                ", isModifiable=" + isModifiable +
                '}';
    }

    public Long getIndex() {
        return index;
    }

    @Override
    public void setModifiability(Boolean modifiable) {
        this.isModifiable = modifiable;
    }

    @Override
    public Boolean isModifiable() {
        return this.isModifiable;
    }
}
