package main.java.cz.cvut.ida.nesisl.api.logic;

import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

/**
 * Created by EL on 2.3.2016.
 */
public class Literal {
    private final Fact fact;
    private final Boolean isPositive;

    public Literal(Fact fact, Boolean isPositive) {
        this.fact = fact;
        this.isPositive = isPositive;
    }

    public Literal(Pair<Fact, Boolean> pair) {
        this(pair.getLeft(),pair.getRight());
    }

    public Fact getFact() {
        return fact;
    }

    public Boolean isPositive() {
        return isPositive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Literal)) return false;

        Literal literal = (Literal) o;

        if (fact != null ? !fact.equals(literal.fact) : literal.fact != null) return false;
        if (isPositive != null ? !isPositive.equals(literal.isPositive) : literal.isPositive != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fact != null ? fact.hashCode() : 0;
        result = 31 * result + (isPositive != null ? isPositive.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Literal{" +
                "fact=" + fact +
                ", isPositive=" + isPositive +
                '}';
    }
}

