package main.java.cz.cvut.ida.nesisl.modules.weka.tools;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSetDescriptionLength;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSetDescriptionLengthFactor;

import java.util.List;
import java.util.Map;

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
        List<Map<Fact, Value>> data = dataset.getTrainRawData();
        RuleAccuracy originalAccuracyComputer = RuleAccuracy.create(ruleSet);
        double originalAccuracy = originalAccuracyComputer.computeAccuracy(data, dataset);
        long previousNumberOfConsistent = originalAccuracyComputer.numberOfConsistentClassifications(data, dataset);
        long trimmed = 0;
        RuleSet result = ruleSet.getCopy();
        System.out.println("Original ruleset of description length " + RuleSetDescriptionLengthFactor.getDefault().computeDescriptionLength(ruleSet) + " and accuracy " + originalAccuracy + ".");

        if(percentualAccuracyOfOriginalDataset >= 0.9999999999){
            System.out.println("Nothing trimmed since the the wanted decrease in accuracy is less than 10^-10.");
            return result;
        }

        while (true) {
            result = RulesTrimmer.create(result, 1);

            System.out.println("current result:\t" + result.toString());

            RuleAccuracy currentAccComputer = RuleAccuracy.create(result);
            //double currentAccuracy = currentAccComputer.computeAccuracy(dataset.getTrainRawData(), dataset);
            double currentAccuracy = currentAccComputer.computeAccuracy(data, dataset);
            long currentNumberOfConsistent = currentAccComputer.numberOfConsistentClassifications(data, dataset);
            if(currentAccuracy < percentualAccuracyOfOriginalDataset * originalAccuracy){
                System.out.println("One rule trimmed. Now, there is ruleset of description length " + RuleSetDescriptionLengthFactor.getDefault().computeDescriptionLength(result) + " and accuracy " + currentAccuracy + ".");
                   trimmed++;
            }
            if (currentAccuracy < percentualAccuracyOfOriginalDataset * originalAccuracy ||
                    previousNumberOfConsistent == currentNumberOfConsistent) {
                break;
            }
            trimmed++;
            System.out.println("One rule trimmed. Now, in the "+ trimmed+"-th step, there is ruleset of description length " + RuleSetDescriptionLengthFactor.getDefault().computeDescriptionLength(result) + " and accuracy " + currentAccuracy + ".");
            previousNumberOfConsistent = currentNumberOfConsistent;
        }
        System.out.println("Trimmed " + trimmed + " rules.");
        return result;
    }
}
