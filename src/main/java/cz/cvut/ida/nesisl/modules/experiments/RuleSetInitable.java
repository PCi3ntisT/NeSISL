package main.java.cz.cvut.ida.nesisl.modules.experiments;

import java.io.File;

/**
 * Created by EL on 1.4.2016.
 */
@FunctionalInterface
public interface RuleSetInitable<T extends NeuralNetworkOwner> {
    public T initialize(File ruleSetFile);
}

