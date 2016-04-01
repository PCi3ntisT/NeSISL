package main.java.cz.cvut.ida.nesisl.modules.experiments.generator;

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
        if(isTerminal() || isNegation() ){
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
        if(isNegation()){
            return false;
        }
        if (isTerminal() || (null != first && null != second && first.isTerminal() && second.isTerminal())) {
            return true;
        }
        if(first.getOperator() == Operator.IMPLICATION || second.getOperator() == Operator.IMPLICATION){
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
        if(isNegation()){
            return " ! ( " + first + " ) ";
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
        long count = 0;
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
                    || "!".equals(token) ) {
                continue;
            }
            count++;
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
}
