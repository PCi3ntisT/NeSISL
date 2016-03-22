package main.java.cz.cvut.ida.nesisl.api.logic;

/**
 * Created by EL on 1.3.2016.
 */
public class Fact {

    private final String fact;

    public Fact(String fact) {
        this.fact = fact;
    }

    public String getFact() {
        return fact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fact)) return false;

        Fact fact1 = (Fact) o;

        if (!fact.equals(fact1.fact)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return fact.hashCode();
    }

    @Override
    public String toString() {
        return "Fact{" +
                "fact='" + fact + '\'' +
                '}';
    }

}




