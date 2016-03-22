package main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.logic.Literal;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by EL on 3.3.2016.
 */
public class Rules {
    private final Set<KBANNRule> finalRules;
    private final Set<Pair<Fact, Integer>> hierarchy;
    private final Set<Fact> inputFacts;
    private final Set<Fact> conclusionFacts;

    public Rules(Set<KBANNRule> finalRules, Set<Pair<Fact, Integer>> hierarchy, Set<Fact> inputFacts, Set<Fact> conclusionFacts) {
        this.finalRules = finalRules;
        this.hierarchy = hierarchy;
        this.inputFacts = inputFacts;
        this.conclusionFacts = conclusionFacts;
    }


    public Set<Pair<Fact, Integer>> getHierarchy() {
        return hierarchy;
    }

    public Set<Fact> getInputFacts() {
        return inputFacts;
    }

    public Set<Fact> getConclusionFacts() {
        return conclusionFacts;
    }

    public Set<KBANNRule> getFinalRules() {
        return finalRules;
    }

    public static Rules create(RuleFile ruleFile) {
        Set<KBANNRule> finalRules = makeFinalRules(ruleFile);
        Set<Pair<Fact, Integer>> hierarchy = makeHierarchy(finalRules,ruleFile);
        return new Rules(finalRules, hierarchy, ruleFile.getInputFacts(), ruleFile.getConclusionFacts());
    }

    private static Set<Pair<Fact, Integer>> makeHierarchy(Set<KBANNRule> finalRules, RuleFile ruleFile) {
        Map<Fact,Integer> map = new HashMap<>();
        ruleFile.getFacts().stream().forEachOrdered(fact ->
                assignLayerNumber(fact, map, ruleFile,
                        finalRules.stream().collect(Collectors.groupingBy(KBANNRule::getHead))));
        return map.entrySet().stream().filter(entry -> entry.getValue() >= 0).map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }

    private static void assignLayerNumber(Fact fact, Map<Fact, Integer> hierarchy, RuleFile ruleFile, Map<Fact, List<KBANNRule>> rules) {
        if(hierarchy.containsKey(fact)){
            return;
        }
        if(!rules.containsKey(fact) || (rules.containsKey(fact) && rules.get(fact).size() < 1)){
            if(ruleFile.getConclusionFacts().contains(fact)){
                hierarchy.put(fact,-2);
                return;
            }
            if(ruleFile.getInputFacts().contains(fact)){
                hierarchy.put(fact, -1);
                return;
            }
            hierarchy.put(fact, 0);
            return;
        }
        OptionalInt max = rules.get(fact).stream().map(rule -> {
            rule.getBody().forEach(literal -> assignLayerNumber(literal.getFact(), hierarchy, ruleFile, rules));
            return rule.getBody().stream().mapToInt(literal -> hierarchy.get(literal.getFact())).max();
        }).mapToInt(i -> i.getAsInt()).max();

        if(ruleFile.getConclusionFacts().contains(fact)){
            hierarchy.put(fact,-2);
            return;
        }
        hierarchy.put(fact,max.getAsInt() + 1);
    }

    private static Set<KBANNRule> makeFinalRules(RuleFile ruleFile) {
        Set<KBANNRule> set = new HashSet<>();
        ruleFile.getRules().forEach(
                (head, pair) -> {
                    if (pair.size() == 1) {
                        addAndRuleToSet(head, pair.iterator().next(), set);
                    } else if (pair.size() > 1) {
                        Boolean isModifiable = true;
                        Set<Literal> orSet = new HashSet<>();
                        for (Pair<Set<Literal>, Boolean> body : pair) {
                            isModifiable = isModifiable && body.getRight();
                            Fact fact = makeNextFact(head, ruleFile);
                            ruleFile.addFact(fact);
                            addAndRuleToSet(fact, body, set);
                            orSet.add(new Literal(fact,true));
                        }
                        addOrRuleToSet(head, new Pair<>(orSet, isModifiable), set);
                    }
                }
        );
        return set;
    }

    private static void addOrRuleToSet(Fact head, Pair<Set<Literal>, Boolean> body, Set<KBANNRule> set) {
        set.add(new KBANNRule(body.getLeft(), body.getRight(), KBANNRule.Type.DISJUNCTION, head));
    }

    private static void addAndRuleToSet(Fact head, Pair<Set<Literal>, Boolean> body, Set<KBANNRule> set) {
        set.add(new KBANNRule(body.getLeft(), body.getRight(), KBANNRule.Type.CONJUNCTION, head));
    }


    private static Fact makeNextFact(Fact origin, RuleFile ruleFile) {
        String factName = origin.getFact();
        Fact fact = new Fact(factName);
        Set<Fact> facts = ruleFile.getFacts();
        while (facts.contains(fact)) {
            factName += "'";
            fact = new Fact(factName);
        }
        return fact;
    }
}
