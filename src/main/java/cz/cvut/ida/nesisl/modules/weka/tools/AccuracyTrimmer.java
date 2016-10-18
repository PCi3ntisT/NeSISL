package main.java.cz.cvut.ida.nesisl.modules.weka.tools;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;

/**
 * Trims the accuracy by trimming rules (do not consider trimming antecedents only.
 * <p>
 * Created by EL on 20.9.2016.
 */
public class AccuracyTrimmer {
    private final Dataset dataset;
    private final RuleSet ruleSet;

    private AccuracyTrimmer(RuleSet ruleSet, Dataset dataset) {
        this.ruleSet = ruleSet;
        this.dataset = dataset;
    }

    public static AccuracyTrimmer create(RuleSet ruleSet, Dataset dataset) {
        return new AccuracyTrimmer(ruleSet, dataset);
    }

    public RuleSet getRuleSetWithTrimmedAccuracy(double percentualAccuracyOfOriginalDataset) {
        RuleAccuracy originalAccuracyComputer = RuleAccuracy.create(ruleSet);
        double originalAccuracy = originalAccuracyComputer.computeAccuracy(dataset.getRawData(), dataset);
        long previousNumberOfConsistent = originalAccuracyComputer.numberOfConsistentClassifications(dataset.getRawData(), dataset);
        long trimmed = 0;
        RuleSet result = ruleSet.getCopy();
        System.out.println("Original ruleset of complexity " + result.getComplexity() + " and accuracy " + originalAccuracy + ".");
        while (true) {
            result = RulesTrimmer.create(result, 1);
            RuleAccuracy currentAccComputer = RuleAccuracy.create(result);
            double currentAccuracy = currentAccComputer.computeAccuracy(dataset.getRawData(), dataset);
            long currentNumberOfConsistent = currentAccComputer.numberOfConsistentClassifications(dataset.getRawData(), dataset);
            if(currentAccuracy < percentualAccuracyOfOriginalDataset * originalAccuracy){
                System.out.println("One rule trimmed. Now, there is ruleset of complexity " + result.getComplexity() + " and accuracy " + currentAccuracy + ".");
                   trimmed++;
            }
            if (currentAccuracy < percentualAccuracyOfOriginalDataset * originalAccuracy ||
                    previousNumberOfConsistent == currentNumberOfConsistent) {
                break;
            }
            trimmed++;
            System.out.println("One rule trimmed. Now, there is ruleset of complexity " + trimmed + " " + result.getComplexity() + " and accuracy " + currentAccuracy + ".");
            previousNumberOfConsistent = currentNumberOfConsistent;
        }
        System.out.println("Trimmed " + trimmed + " rules.");
        return result;
    }
}
