package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.kbann;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.logic.Literal;

import java.util.Set;

/**
 * Created by EL on 3.3.2016.
 */


public class KBANNRule {

    public enum Type {
        CONJUNCTION, type, DISJUNCTION, N_TRUE
    }

    private final Type type;
    private final Fact head;
    private final Set<Literal> body;
    private Boolean isModifiable;
    private final int nTrue;

    public KBANNRule(Set<Literal> body, Boolean isModifiable, Type type, Fact head) {
        this.body = body;
        this.isModifiable = isModifiable;
        this.type = type;
        this.head = head;
        this.nTrue = 0;
    }

    public KBANNRule(Set<Literal> body, Boolean isModifiable, Type type, Fact head, int nTrue) {
        this.body = body;
        this.isModifiable = isModifiable;
        this.type = type;
        this.head = head;
        this.nTrue = nTrue;
    }

    public Boolean isModifiable() {
        return isModifiable;
    }

    public void setModifiable(Boolean isModifiable) {
        this.isModifiable = isModifiable;
    }

    public Type getType() {
        return type;
    }

    public Fact getHead() {
        return head;
    }

    public Set<Literal> getBody() {
        return body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KBANNRule)) return false;

        KBANNRule kbannRule = (KBANNRule) o;

        if (!body.equals(kbannRule.body)) return false;
        if (!head.equals(kbannRule.head)) return false;
        if (type != kbannRule.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + head.hashCode();
        result = 31 * result + body.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "KBANNRule{" +
                "type=" + type +
                ", head=" + head +
                ", body=" + body +
                ", isModifiable=" + isModifiable +
                '}';
    }

    public String readRule(){
        StringBuilder sb = new StringBuilder(head.getFact() + " " + RuleFile.CHANGABLE_RULE + " ");
        body.forEach(literal -> sb.append(literal.getFact().getFact() + ", "));
        sb.append(".");
        return sb.toString();
    }

    public int getNTrue() {
        return nTrue;
    }
}
