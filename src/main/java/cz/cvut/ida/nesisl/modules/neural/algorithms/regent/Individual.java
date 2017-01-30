package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.regent;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

/**
 * Created by EL on 23.3.2016.
 */
public class Individual extends Pair<NeuralNetwork,Double> {

    public Individual(NeuralNetwork neuralNetwork, Double aDouble) {
        super(neuralNetwork, aDouble);
    }

    public NeuralNetwork getNeuralNetwork(){
        return super.getLeft();
    }

    public Double getFitness(){
        return super.getRight();
    }
}
