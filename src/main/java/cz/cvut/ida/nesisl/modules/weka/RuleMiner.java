package main.java.cz.cvut.ida.nesisl.modules.weka;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.application.Main;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.AccuracyTrimmer;
import weka.core.Instances;

/**
 * Created by EL on 27.1.2017.
 */
public class RuleMiner {

    public static RuleSet mineAndTrimmeRuleSet(int idx, Dataset nesislDataset, Instances wekaDataset) {
        System.out.println("\n\n--------- fold " + idx + ":\t rule set learning\n\n");
        RuleSet ruleSet = WekaJRip.create(wekaDataset,nesislDataset).getRuleSet();

        // popripade nejaky trimmer nebo relabelling
        // ruleSet = RuleTrimmer.createTest(ruleSet).getRuleSet();
        // RuleSet a1 = AntecedentsTrimmer.createTest(ruleSet).getRuleSet();
        // Dataset relabeled = Relabeling.createTest(nesislDataset, ruleSet).getDataset();
        System.out.println("\n\n--------- fold " + idx + ":\t ruleset trimming\n\n");
        ruleSet = AccuracyTrimmer.create(ruleSet, nesislDataset).getRuleSetWithTrimmedAccuracy(Main.percentualAccuracyOfOriginalDataset);
        return ruleSet;
    }

    public static RuleSet mineRuleSet(Dataset nesislDataset, Instances wekaDataset) {
        RuleSet ruleSet = WekaJRip.create(wekaDataset, nesislDataset).getRuleSet();
        return ruleSet;
    }

    public static RuleSet mineAndTrimmeRule(Dataset nesislDataset, Instances wekaDataset) {
        RuleSet ruleSet = WekaJRip.create(wekaDataset,nesislDataset).getRuleSet();

        // do not solve the consistency here
        //nesislDataset.makeConsistentStatefully(ruleSet);


        // popripade nejaky trimmer nebo relabelling
        // ruleSet = RuleTrimmer.createTest(ruleSet).getRuleSet();
        // RuleSet a1 = AntecedentsTrimmer.createTest(ruleSet).getRuleSet();
        // Dataset relabeled = Relabeling.createTest(nesislDataset, ruleSet).getDataset();
        ruleSet = AccuracyTrimmer.create(ruleSet, nesislDataset).getRuleSetWithTrimmedAccuracy(Main.percentualAccuracyOfOriginalDataset);
        return ruleSet;
    }
}
