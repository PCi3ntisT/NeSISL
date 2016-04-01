package main.java.cz.cvut.ida.nesisl.modules.experiments;

/**
 * Created by EL on 1.4.2016.
 */
@FunctionalInterface
public interface Initable<T extends NeuralNetworkOwner> {
    public T initialize();
}
