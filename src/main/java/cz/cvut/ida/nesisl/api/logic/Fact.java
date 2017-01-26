package main.java.cz.cvut.ida.nesisl.api.logic;

/**
 * Created by EL on 1.3.2016.
 */
public class Fact {

    private final String fact;
    private boolean isBoolean;

    /**
     * awfull statefullnes of the boolean (whether it is boolean or not)
     * @param fact
     * @param isBoolean
     */
    public Fact(String fact, boolean isBoolean) {
        this.fact = fact;
        this.isBoolean = isBoolean;
    }

    public Fact(String fact) {
        this(fact, false);
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
                ", isBoolean=" + isBoolean +
                '}';
    }

    public boolean isBoolean() {
        return isBoolean;
    }

    /**
     * awfull statefullnes
     * @param isBoolean
     */
    public void setBoolean(boolean isBoolean) {
        this.isBoolean = isBoolean;
    }
}




