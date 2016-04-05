package main.java.cz.cvut.ida.nesisl.modules.experiments.generator;

/**
 * Created by EL on 25.3.2016.
 */
public enum Operator {
    AND ("&"),
    OR ("|"),
    XOR ("XOR"),
    IMPLICATION ("->");

    private final String operator;

    Operator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return operator;
    }
}
