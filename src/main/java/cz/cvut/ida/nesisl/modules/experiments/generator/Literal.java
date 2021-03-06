package main.java.cz.cvut.ida.nesisl.modules.experiments.generator;

/**
 * Created by EL on 25.3.2016.
 */
public class Literal {
    public static final String literals = "abcdefghijklmnopqrstuvwxyz";
    public static final String outputLiterals = "xyzwvutsrqpomn";
    private final int idx;

    public Literal(int idx) {
        this.idx = idx;
    }

    @Override
    public String toString() {
        return "" + literals.charAt(idx);
    }

    public int getIdx() {
        return idx;
    }
}
