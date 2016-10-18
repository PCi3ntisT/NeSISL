package main.java.cz.cvut.ida.nesisl.modules.weka.rules;

import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by EL on 8.9.2016.
 */
public class RuleSet {

    private final List<Rule> rules;

    private RuleSet(List<Rule> rules) {
        this.rules = rules;
    }

    public List<Rule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public RuleSet getCopy() {
        return create(rules.stream()
                .map(rule -> rule.getCopy())
                .collect(Collectors.toList()));
    }

    public RuleSet addRule(Rule rule) {
        RuleSet copy = this.getCopy();
        copy.addStateful(rule);
        return copy;
    }

    private void addStateful(Rule rule) {
        rules.add(rule);
    }

    public int getNumberOfRules() {
        return rules.size();
    }

    public static RuleSet create() {
        return new RuleSet(new ArrayList<>());
    }

    public static RuleSet create(List<Rule> rules) {
        return new RuleSet(rules);
    }

    public static RuleSet create(String wekaOutput) {
        RuleSet result = RuleSet.create();
        String[] splitted = wekaOutput.split("\n");

        String lastTarget = null;
        Rule currentRule = null;

        for (int idx = 0; idx < splitted.length; idx++) {
            String rule = splitted[idx];
            if (!rule.contains("=>")) {
                continue;
            }

            String head = retrieveHead(rule);
            Implication implication = retrieveBody(rule);

            if (!head.equals(lastTarget)) {
                if (null != currentRule) {
                    result = result.addRule(currentRule);
                }
                currentRule = Rule.create(head, new ArrayList<>());
            }

            currentRule = currentRule.addImplication(implication);
            lastTarget = head;
        }
        if (null != currentRule) {
            result = result.addRule(currentRule);
        }
        return result;
    }

    // this does not parse real datasets
    private static Implication retrieveBody(String rule) {
        String body = rule.split("=>")[0];
        if(body.trim().length() < 1){
            return Implication.create(new ArrayList<>());
        }
        return Implication.create(Arrays.asList(body.split("and"))
                        .stream()
                        .map(literal -> {
                            String[] splitted = literal.split("=");
                            String attribute = splitted[0].replace("(","").trim();
                            String value = splitted[1].replace(")","").trim();
                            return Antecedent.create(attribute, value);
                        })
                        .collect(Collectors.toList())
        );
    }

    private static String retrieveHead(String rule) {
        int start = rule.lastIndexOf("=");
        int end = rule.lastIndexOf("(");
        return rule.substring(start + 1, end).trim();
    }

    public String getTheory(){
        return RuleSetToTheory.create(this).getTheory();
    }

    public boolean isBinaryClassClassification(){
        return 2 == rules.stream()
                .map(rule -> rule.getHead())
                .collect(Collectors.toSet())
                .size()

                ||
                1 == rules.stream()
                .map(rule -> rule.getHead())
                .collect(Collectors.toSet())
                .size() ;

        /*return 1 == rules.stream()
                .map(rule -> rule.getHead())
                .collect(Collectors.toSet())
                .size()

                && rules.stream()
                .filter(rule -> rule.getHead().equals(DatasetImpl.CLASS_TOKEN))
                .count() > 1;
                */
    }

    @Override
    public String toString() {
        return "RuleSet{" +
                "rules=" + rules +
                '}';
    }

    public RuleSet replaceRule(int selectedRule, Rule trimmedRule) {
        List<Rule> trimmedRules = new ArrayList<>(rules);
        trimmedRules.remove(selectedRule);
        trimmedRules.add(selectedRule,trimmedRule);
        return RuleSet.create(trimmedRules);
    }

    public int getComplexity(){
        return rules.stream()
                .mapToInt(rule -> rule.getNumberOfImplications())
                .sum();
    }
}
