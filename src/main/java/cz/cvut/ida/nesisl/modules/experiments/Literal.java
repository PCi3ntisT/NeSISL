package main.java.cz.cvut.ida.nesisl.modules.experiments;

/**
 * Created by EL on 25.3.2016.
 */
public class Literal {
    private final String literals = "abcdefghijklmnopqrstuvwxyz";
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
