package main.java.cz.cvut.ida.nesisl.modules.weka.rules;

/**
 * Is implemented only for nominal attributes.
 *
 * Created by EL on 8.9.2016.
 */
public class Antecedent {

    private final String attribute;
    private final String value;

    private Antecedent(String attribute, String value) {
        this.attribute = attribute;
        this.value = value;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getValue() {
        return value;
    }

    public static Antecedent create(String attribute,String value){
        return new Antecedent(attribute,value);
    }

    @Override
    public String toString() {
        return "Antecedent{" +
                "attribute='" + attribute + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
