package main.java.cz.cvut.ida.nesisl.modules.weka.tools;

import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Implication;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Rule;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by EL on 8.9.2016.
 */
public class RuleTrimmer {

    private final RuleSet ruleSet;

    public RuleTrimmer(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * Trims the last rule with non-empty antecedents.
     * @param ruleSet
     * @return
     */
    public static RuleTrimmer create(RuleSet ruleSet){
        if(ruleSet.getRules().size() < 2){
            throw new IllegalStateException("There is nothing to remove.");
        }
        List<Rule> result = new ArrayList<>(ruleSet.getRules());
        int idx = result.size() - 2;
        Rule rule = result.get(idx);
        if(rule.getNumberOfImplications() > 1){
            List<Implication> implications = new ArrayList<>(rule.getImplications());
            implications.remove(implications.size()-1);
            Rule trimmedRule = Rule.create(rule.getHead(), implications);
            result.remove(idx);
            result.add(idx,trimmedRule);
        }else{
            result.remove(idx);
        }
        return new RuleTrimmer(RuleSet.create(result));
    }
}
