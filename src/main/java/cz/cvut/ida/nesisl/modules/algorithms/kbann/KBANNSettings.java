package main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann;

import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGenerator;

/**
 * Created by EL on 6.3.2016.
 */
public class KBANNSettings {
    private final RandomGenerator randomGenerator;
    private final Double omega;

    public KBANNSettings(RandomGenerator randomGenerator, Double omega) {
        this.randomGenerator = randomGenerator;
        this.omega = omega;
    }

    public RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public Double getOmega() {
        return omega;
    }

    @Override
    public String toString() {
        return "KBANNSettings{" +
                "randomGenerator=" + randomGenerator +
                ", omega=" + omega +
                '}';
    }
}
