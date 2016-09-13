package main.java.cz.cvut.ida.nesisl.modules.weka.tools;

import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Implication;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Rule;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;

/**
 * Created by EL on 8.9.2016.
 */
public class AntecedentsTrimmer {

    private final RuleSet ruleSet;

    private AntecedentsTrimmer(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * Throws away antecedents from the last rule which has at least one non-empty implication.
     *
     * @param ruleSet
     * @return
     */
    public static AntecedentsTrimmer create(RuleSet ruleSet) {
        if (ruleSet.getRules().size() < 2) {
            throw new IllegalStateException("Nothing to trim.");
        }
        Rule rule;
        int selectedRule = -1;
        int selectedImplication = -1;
        for (int ruleIdx = ruleSet.getNumberOfRules() - 1; -1 == selectedRule ; ruleIdx--) {
            if (ruleIdx < 0) {
                throw new IllegalStateException("Nothing to trim.");
            }
            rule = ruleSet.getRules().get(ruleIdx);
            for (int implicationIdx = rule.getNumberOfImplications() - 1; implicationIdx >= 0; implicationIdx--) {
                Implication implication = rule.getImplications().get(implicationIdx);
                if (implication.getNumberOfAntecedents() > 1) {
                    selectedRule = ruleIdx;
                    selectedImplication = implicationIdx;
                    break;
                }
            }
        }
        rule = ruleSet.getRules().get(selectedRule);
        Implication trimmedImplication = Implication.create(rule.getImplications().get(selectedImplication).getBody().subList(0,rule.getImplications().get(selectedImplication).getNumberOfAntecedents()-1));
        Rule trimmedRule = rule.replaceImplication(selectedImplication, trimmedImplication);
        RuleSet trimmedRuleSet = ruleSet.replaceRule(selectedRule, trimmedRule);
        return new AntecedentsTrimmer(trimmedRuleSet);
    }

    public static RuleSet create(RuleSet ruleSet,Integer howMany) {
        if(howMany < 1){
            throw new IllegalStateException("Use natural number.");
        }
        for (int idx = 0; idx < howMany; idx++) {
            try{
                RuleSet trimmed = AntecedentsTrimmer.create(ruleSet).getRuleSet();
                ruleSet = trimmed;
            }catch (Exception e){
                return ruleSet;
            }
        }
        return ruleSet;
    }
}
