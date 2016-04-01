package main.java.cz.cvut.ida.nesisl.api.classifiers;

import main.java.cz.cvut.ida.nesisl.api.data.Value;

/**
 * Created by EL on 30.3.2016.
 */
public interface Classifier {

    public Double getTreshold();

    public Boolean classify(Value value);

    public Boolean classify(Double value);

    public String classifyToOneZero(Value value);

    public String classifyToOneZero(double value);

    public Double classifyToDouble(Value value);

    public Double classifyToDouble(Double value);
}
