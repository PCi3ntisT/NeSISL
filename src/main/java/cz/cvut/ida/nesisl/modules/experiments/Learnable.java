package main.java.cz.cvut.ida.nesisl.modules.experiments;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;

/**
 * Created by EL on 1.4.2016.
 */
@FunctionalInterface
public interface Learnable {
    public NeuralNetwork learn(NeuralNetworkOwner owner, Dataset dataset);
}
