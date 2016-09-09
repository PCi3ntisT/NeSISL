package main.java.cz.cvut.ida.nesisl.api.data;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.ClassAttribute;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by EL on 13.2.2016.
 */
public interface Dataset {

    public List<Sample> getTrainData(NeuralNetwork network);
    public List<Sample> getTestData(NeuralNetwork network);

    public List<Fact> getInputFactOrder();

    public List<Fact> getOutputFactOrder();

    public List<Double> getAverageOutputs(NeuralNetwork network);
    public List<Double> getTrainNodeAverageOutputs(NeuralNetwork network);

    public File getOriginalFile();

    public List<Sample> getNodeTrainData(NeuralNetwork network);

    /**
     * Returns train data only.
     * @return
     */
    public List<Map<Fact,Value>> getRawData();

    public ClassAttribute getClassAttribute();

    public String cannonicalOutput(Map<Fact, Value> sample);
}
