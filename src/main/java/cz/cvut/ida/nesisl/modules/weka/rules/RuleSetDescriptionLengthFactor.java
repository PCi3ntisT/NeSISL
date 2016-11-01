package main.java.cz.cvut.ida.nesisl.modules.weka.rules;

/**
 * Created by EL on 31.10.2016.
 */
public class RuleSetDescriptionLengthFactor {

    private static RuleSetDescriptionLengthFactor computer = new RuleSetDescriptionLengthFactor();

    public static RuleSetDescriptionLengthFactor getDefault() {
        return computer;
    }

    public long computeDescriptionLength(RuleSet ruleset) {
        return -1 // -1 for that we do not need the last ending character to be \n :)
                + ruleset.getRules()
                .stream()
                .mapToInt(rule ->
                                rule.getImplications()
                                        .stream()
                                        .mapToInt(implication ->
                                                (implication.getNumberOfAntecedents() + 1 + 1) * 2 // +1 for implication mark (=>); +1 for class (rule.getHead()); 2* for whitespaces in between and in the end of the line
                                        )
                                        .sum()
                )
                .sum();
    }
}
