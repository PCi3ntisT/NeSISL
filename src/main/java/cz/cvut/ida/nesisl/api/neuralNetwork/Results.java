package main.java.cz.cvut.ida.nesisl.api.neuralNetwork;

import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.util.List;
import java.util.Map;

/**
 * Created by EL on 13.2.2016.
 */
public class Results extends Pair<List<Double>,Map<Node,Double>>{
    public Results(List<Double> outputs, Map<Node, Double> edgeDoubleHashMap) {
        super(outputs, edgeDoubleHashMap);
    }

    public List<Double> getComputedOutputs(){
        return super.getLeft();
    }

    public Map<Node,Double> getComputedValues(){
        return super.getRight();
    }
}
