package main.java.cz.cvut.ida.nesisl.api.data;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;

import java.util.List;

/**
 * Created by EL on 13.2.2016.
 */
public interface Dataset {

    public List<Sample> getTrainData(NeuralNetwork network);

    public List<Fact> getInputFactOrder();
    public List<Fact> getOutputFactOrder();

    public List<Double> getAverageOutputs(NeuralNetwork network);

    public List<Sample> getTestData(NeuralNetwork network);
}
