package main.java.cz.cvut.ida.nesisl.modules.weka.tools;

import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Implication;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Rule;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by EL on 8.9.2016.
 */
public class RulesTrimmer {

    private final RuleSet ruleSet;

    public RulesTrimmer(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * Trims the last rule with non-empty antecedents.
     *
     * @param ruleSet
     * @return
     */
    public static RulesTrimmer create(RuleSet ruleSet) {
        if (ruleSet.getRules().size() < 2) {
            throw new IllegalStateException("There is nothing to remove.");
        }
        List<Rule> result = new ArrayList<>(ruleSet.getRules());

        // awful, should be modularized
        if (ruleSet.isBinaryClassClassification()) {
            if (ruleSet.getRules().get(0).getNumberOfImplications() == 0) {
                throw new IllegalStateException("There is nothing to remove.");
            } else if (ruleSet.getRules().get(0).getNumberOfImplications() == 1) {
                Rule rule = ruleSet.getRules().get(0);
                ArrayList<Rule> ruleList = new ArrayList<>();
                ruleList.add(Rule.create(rule.getHead(), new ArrayList<>()));
                ruleList.add(ruleSet.getRules().get(1).getCopy());
                return new RulesTrimmer(RuleSet.create(ruleList));
            } else {
                List<Implication> implications = new ArrayList<>();
                Rule rule = ruleSet.getRules().get(0);
                implications.addAll(rule.getImplications().subList(0, rule.getNumberOfImplications() - 1));
                ArrayList<Rule> ruleList = new ArrayList<>();
                ruleList.add(Rule.create(rule.getHead(), implications));
                ruleList.add(ruleSet.getRules().get(1).getCopy());
                return new RulesTrimmer(RuleSet.create(ruleList));
            }
        }


        if (ruleSet.getRules().size() < 2) {
            throw new IllegalStateException("There is nothing to remove.");
        }

        for (int idx = result.size() - 1; idx >= 0; idx--) {
            Rule rule = result.get(idx);

            if (0 == rule.getNumberOfImplications()) {
                continue;
            } else if (1 == rule.getNumberOfImplications()) {
                if (0 == rule.getImplications().iterator().next().getNumberOfAntecedents()) {
                    continue;
                }
                result.remove(idx);
                Rule trimmedRule = Rule.create(rule.getHead(), new ArrayList<>());
                result.add(idx, trimmedRule);
            } else if (rule.getNumberOfImplications() > 1) {
                List<Implication> implications = new ArrayList<>(rule.getImplications());
                int last = implications.size() - 1;
                if (0 == implications.get(last).getNumberOfAntecedents()) {
                    implications.remove(last - 1);
                } else {
                    implications.remove(last);
                }
                Rule trimmedRule = Rule.create(rule.getHead(), implications);
                result.remove(idx);
                result.add(idx, trimmedRule);
            }

            System.out.println("rule trimmed");

            return new RulesTrimmer(RuleSet.create(result));
        }
        throw new IllegalStateException("There is nothing to remove.");
    }

    public static RuleSet create(RuleSet ruleSet, Integer howMany) {
        if (howMany < 1) {
            throw new IllegalStateException("Use natural number.");
        }
        for (int idx = 0; idx < howMany; idx++) {
            try {
                RuleSet trimmed = RulesTrimmer.create(ruleSet).getRuleSet();
                ruleSet = trimmed;
            } catch (Exception e) {
                return ruleSet;
            }
        }
        return ruleSet;
    }
}
