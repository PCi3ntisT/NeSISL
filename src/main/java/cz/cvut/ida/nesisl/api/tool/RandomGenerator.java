package main.java.cz.cvut.ida.nesisl.api.tool;

import java.util.Random;

/**
 * Created by EL on 6.3.2016.
 */
public interface RandomGenerator {

    public Double nextDouble();

    public Integer nextInteger();

    public Integer nextIntegerFromRange(int start, int end);

    public Integer nextIntegerTo(int end);

    public Long nextLongFromRange(long start, long end);

    public Long nextLongTo(long end);

    public Boolean isProbable(Double psi);

    public Random getRandom();
}
