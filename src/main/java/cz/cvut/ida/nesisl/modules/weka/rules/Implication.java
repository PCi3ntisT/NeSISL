package main.java.cz.cvut.ida.nesisl.modules.weka.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by EL on 8.9.2016.
 */
public class Implication {

    private final List<Antecedent> body;

    private Implication(List<Antecedent> body) {
        this.body = body;
    }

    public List<Antecedent> getBody() {
        return Collections.unmodifiableList(body);
    }

    public int getNumberOfAntecedents(){
        return body.size();
    }

    public Implication addAntecedent(Antecedent antecedent){
        Implication copy = this.getCopy();
        copy.addAntecedentStateful(antecedent);
        return copy;
    }

    private void addAntecedentStateful(Antecedent antecedent) {
        body.add(antecedent);
    }

    public Implication getCopy() {
        return create(new ArrayList<>(body));
    }

    public static Implication create(List<Antecedent> antecedents) {
        return new Implication(antecedents);
    }

    @Override
    public String toString() {
        return "Implication{" +
                "body=" + body +
                '}';
    }
}

