package main.java.cz.cvut.ida.nesisl.modules.weka;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.ClassAttribute;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import weka.classifiers.rules.JRip;
import weka.core.Instances;


/**
 * Created by EL on 7.9.2016.
 */
public class WekaJRip {

    private final RuleSet ruleSet;

    public WekaJRip(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    public static WekaJRip create(Instances dataset,Dataset nesislDataset) {
        JRip jrip = new JRip();
        String[] args = new String[]{};//{"-F","10"};
        System.out.println("TODO add parameters to JRIP");
        try {
            jrip.setOptions(args);
            jrip.buildClassifier(dataset);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        ArrayList<Rule> rules = jrip.getRuleset();
        System.out.println("exctracted rules");
        //rules.forEach(rule -> System.out.println(rule.getRevision()));
        //rules.forEach(rule -> System.out.println(((JRip.RipperRule)rule).toString(dataset.classAttribute())));
        rules.forEach(rule -> System.out.println(((JRip.RipperRule)rule).toString()));
        System.out.println(jrip.toString());
        */

        System.out.println(jrip.toString());

        RuleSet ruleSet = RuleSet.create(jrip.toString(),nesislDataset);
        return new WekaJRip(ruleSet);
    }

}
