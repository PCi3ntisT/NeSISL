package main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation;

import java.util.stream.DoubleStream;

/**
 * Created by EL on 1.4.2016.
 */
@FunctionalInterface
public interface StoreableResults {
    public DoubleStream getValues();
}
