package main.java.cz.cvut.ida.nesisl.modules.tool;

import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Random;

/**
 * Created by EL on 6.3.2016.
 */
public class RandomGeneratorImpl implements RandomGenerator {
    private final Random generator;
    private final double sigma;
    private final double mu;
    private final long seed;

    public RandomGeneratorImpl(double sigma, double mu, long seed) {
        this.generator = new Random(seed);
        this.sigma = sigma;
        this.mu = mu;
        this.seed = seed;
    }

    @Override
    public Double nextDouble() {
        return 2 * sigma * generator.nextDouble() + mu - sigma;
    }

    @Override
    public Integer nextInteger() {
        throw new NotImplementedException();
    }

    @Override
    public Integer nextIntegerFromRange(int start, int end) {
        return Math.abs(generator.nextInt()) % (end - start) + start;
    }

    @Override
    public Long nextLongFromRange(long start, long end) {
        return Math.abs(generator.nextLong()) % (end - start) + start;
    }

    @Override
    public Integer nextIntegerTo(int end) {
        return nextIntegerFromRange(0,end);
    }

    @Override
    public Long nextLongTo(long end) {
        return nextLongFromRange(0, end);
    }

    @Override
    public Boolean isProbable(Double psi) {
        return psi > generator.nextDouble();
    }

    @Override
    public Random getRandom() {
        return generator;
    }


}
