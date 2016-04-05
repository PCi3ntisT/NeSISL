package main.java.cz.cvut.ida.nesisl.modules.experiments.generator;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by EL on 25.3.2016.
 */
public class Formula {
    private final Formula first;
    private final Formula second;
    private final Literal literal;
    private final Operator operator;
    private final boolean negation;
    public final static String NEGATION_TOKEN = "~";

    public Formula(Formula formula) {
        this.first = formula;
        this.second = null;
        this.operator = null;
        this.negation = true;
        this.literal = null;
    }

    public Formula(Literal literal) {
        this.first = null;
        this.second = null;
        this.literal = literal;
        this.operator = null;
        this.negation = false;
    }

    public Formula(Formula first, Formula second, Operator operator) {
        this.first = first;
        this.second = second;
        this.literal = null;
        this.operator = operator;
        this.negation = false;
    }

    public int getWidth() {
        if (isTerminal() || isNegation()) {
            return 1;
        }
        if (canBeFlattened()) {
            return first.getWidth() + second.getWidth();
        }
        return 2;
    }

    public int getDepth() {
        if (isTerminal()) {
            return 0;
        }
        int flattenAddition = 1;

        if (canBeFlattened()) {
            flattenAddition = 0;
        }

        return Math.max(first.getDepth(), second.getDepth()) + flattenAddition;
    }

    private boolean canBeFlattened() {
        if (isNegation()) {
            return false;
        }
        if (isTerminal() || (null != first && null != second && first.isTerminal() && second.isTerminal())) {
            return true;
        }
        if(first.getOperator() == Operator.XOR || second.getOperator() == Operator.XOR || Operator.XOR == operator ){
            return false;
        }
        if (first.getOperator() == Operator.IMPLICATION || second.getOperator() == Operator.IMPLICATION) {
            return false;
        }
        if ((first.isTerminal() && operator == second.getOperator()) ||
                (second.isTerminal() && operator == first.getOperator())) {
            return true;
        }
        if (operator == first.getOperator() && operator == second.getOperator()) {
            return true;
        }
        return false;
    }

    public boolean isTerminal() {
        return null != literal;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        if (isTerminal()) {
            return " " + literal + " ";
        }
        if (isNegation()) {
            return " " + NEGATION_TOKEN + " ( " + first + " ) ";
        }
        if (canBeFlattened()) {
            return first + " " + operator + " " + second;
        }
        return "(" + first + ") " + operator + " (" + second + ") ";
    }

    public Formula getSecond() {
        return second;
    }

    public Formula getFirst() {
        return first;
    }

    public long getScore() {
        String formula = this.toString();

        if ('(' != formula.charAt(0)) {
            formula = "( " + formula + " )";
        }

        StringTokenizer tokenizer = new StringTokenizer(formula);
        return processScore(tokenizer);
    }

    private long processScore(StringTokenizer tokenizer) {
        if (!tokenizer.hasMoreTokens()) {
            return 0;
        }
        long count = 1;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (")".equals(token)) {
                break;
            }

            if ("(".equals(token)) {
                count += processScore(tokenizer);
                continue;
            }

            if (Operator.AND.toString().equals(token)
                    || Operator.OR.toString().equals(token)
                    || Operator.IMPLICATION.toString().equals(token)
                    || Operator.XOR.toString().equals(token)
                    || NEGATION_TOKEN.equals(token)) {
                continue;
            }
        }
        return count * count;
    }

    public Boolean isTrue(List<Boolean> input) {
        if (isTerminal()) {
            return input.get(literal.getIdx());
        }
        if (isNegation()) {
            return !first.isTrue(input);
        }
        if (Operator.AND == operator) {
            return first.isTrue(input) && second.isTrue(input);
        }
        if (Operator.OR == operator) {
            return first.isTrue(input) || second.isTrue(input);
        }
        if (Operator.XOR == operator) {
            return first.isTrue(input) != second.isTrue(input);
        }
        if (Operator.IMPLICATION == operator) {
            return !first.isTrue(input) || second.isTrue(input);
        }
        return false; // unknown state possible
    }

    public boolean isNegation() {
        return negation;
    }

    private final Long baseWeight = 1l;
    private final Long negationWeight = 1l;
    private final Long orWeight = 1l;
    private final Long andWeight = 2l;
    private final Long xorWeight = orWeight + 2 * (andWeight + negationWeight);
    private final Long implicationWeight = negationWeight + orWeight + andWeight;

    public Long getFlattenedWeightedScore(Operator operator) {
        if(canBeFlattened() && this.operator == operator){
            return first.getFlattenedWeightedScore(this.operator) + second.getFlattenedWeightedScore(this.operator);
        }
        return getWeightedScore();
    }


    public Long getWeightedScore() {
        if (isTerminal()) {
            return baseWeight;
        }
        if (isNegation()) {
            return negationWeight;
        }

        Long weight = 0l;
        if (canBeFlattened()) {
            weight = first.getFlattenedWeightedScore(operator) + second.getFlattenedWeightedScore(operator);
        } else {
            weight = first.getWeightedScore() + second.getWeightedScore();
        }

        switch (operator) {
            case OR:
                return weight * andWeight;
            case AND:
                return weight * andWeight;
            case XOR:
                return weight * xorWeight;
            case IMPLICATION:
                return weight * implicationWeight;
            default:
                throw new IllegalStateException("Unknown type of operator");
        }
    }

}
