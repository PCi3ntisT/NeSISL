package main.java.cz.cvut.ida.nesisl.api.neuralNetwork;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by EL on 9.2.2016.
 */
public class Parameters {

    private final Map<String,Double> map = new HashMap<String, Double>();

    public Parameters() {
    }

    public Parameters(Parameters parameters) {
        if(null != parameters && null != parameters.getParameters()) {
            map.putAll(parameters.getParameters());
        }
    }

    public void addParameter(String parameterName,Double value){
        map.put(parameterName,value);
    }

    public Double getValue(String name){
        if(containParameter(name)){
            return map.get(name);
        }
        try {
            throw new Exception("Uninitialized parameter '"+name+"' queried.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String,Double> getParameters(){
        return new HashMap<>(map);
    }

    public boolean containParameter(String name){
        return map.containsKey(name);
    }
}
