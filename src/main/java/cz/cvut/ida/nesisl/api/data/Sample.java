package main.java.cz.cvut.ida.nesisl.api.data;

import java.util.List;

/**
 * Created by EL on 7.3.2016.
 */
public interface Sample {

    public List<Value> getInput();
    public List<Value> getOutput();

}
