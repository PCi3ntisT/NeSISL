package main.java.cz.cvut.ida.nesisl.modules.weka.rules;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by EL on 8.9.2016.
 */
public class Rule {
    private final String head;
    private final List<Implication> implications;

    private Rule(String head, List<Implication> body) {
        this.head = head;
        this.implications = body;
    }

    public String getHead() {
        return head;
    }

    public List<Implication> getImplications() {
        return Collections.unmodifiableList(implications);
    }

    public Rule getCopy(){
        return create(head,new ArrayList<>(implications));
    }

    public Rule addImplication(Implication implication){
        Rule copy = getCopy();
        copy.addImplicationStateful(implication);
        return copy;
    }

    private void addImplicationStateful(Implication mplication) {
        implications.add(mplication);
    }

    public int getNumberOfImplications(){
        return implications.size();
    }

    public static Rule create(String head, List<Implication> body) {
        return new Rule(head,body);
    }

    @Override
    public String toString() {
        return "Rule{" +
                "head='" + head + '\'' +
                ", implications=" + implications +
                '}';
    }

    public Rule replaceImplication(int selectedImplication, Implication trimmedImplication) {
        List<Implication> impl = new ArrayList<>(implications);
        impl.remove(selectedImplication);
        impl.add(selectedImplication,trimmedImplication);
        return Rule.create(getHead(),impl);
    }
}
