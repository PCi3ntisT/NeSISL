package main.java.cz.cvut.ida.nesisl.modules.extraction.jripExtractor;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.application.MultipleCycles;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiCrossvalidation;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiRepresentationDataset;
import main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation.AccuracyCalculation;
import main.java.cz.cvut.ida.nesisl.modules.extraction.TrepanResults;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.weka.RuleMiner;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSetDescriptionLengthFactor;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.RuleAccuracy;
import weka.core.Instances;


/**
 * Created by EL on 27.1.2017.
 */
public class JRipExtraction {


    private final MultiRepresentationDataset overSampled;
    private final MultiRepresentationDataset originalDataset;
    private final int originalFold;
    private final NeuralNetwork network;

    public JRipExtraction(MultiRepresentationDataset overSampled, MultiRepresentationDataset nesislDataset, int idx, NeuralNetwork network) {
        this.overSampled = overSampled;
        this.originalDataset = nesislDataset;
        this.originalFold = idx;
        this.network = network;
    }

    public TrepanResults run() {
        Dataset overSampledTrainNesisl = overSampled.getNesislDataset();
        Dataset nesislDataset = originalDataset.getNesislDataset();
        Instances overSampledTrainWeka = MultiCrossvalidation.getTrainWekaDataset(overSampled.getNesislDataset(), overSampled);
        RuleSet ruleSet = RuleMiner.mineRuleSet(overSampledTrainNesisl, overSampledTrainWeka);

        System.out.println("\nNewly found ruleset:\n"+ruleSet.getTheory()+"\n");

        double jripTrainAcc = RuleAccuracy.create(ruleSet).computeTrainAccuracy(nesislDataset);
        double jripTestAcc = RuleAccuracy.create(ruleSet).computeTestAccuracy(nesislDataset);

        double jripTestAccOverSampled = RuleAccuracy.create(ruleSet).computeTestAccuracy(overSampledTrainNesisl);

        double networkTrainAcc = AccuracyCalculation.createTrain(network, nesislDataset).getAccuracy();
        double networkTestAcc = AccuracyCalculation.createTest(network, nesislDataset).getAccuracy();
        long descriptionLength = RuleSetDescriptionLengthFactor.getDefault().computeDescriptionLength(ruleSet);

        System.out.println(jripTrainAcc);
        System.out.println(jripTestAcc);
        System.out.println(jripTestAccOverSampled);
        System.out.println(networkTrainAcc);
        System.out.println(networkTestAcc);
        System.out.println(descriptionLength);

        return TrepanResults.create(0l,
                jripTrainAcc,
                jripTestAcc,
                jripTestAccOverSampled, // should be called what it really is
                0d,
                networkTrainAcc,
                networkTestAcc,
                descriptionLength,
                null);
    }

    public static JRipExtraction create(NeuralNetwork learnedNetwork, MultiRepresentationDataset dataset, int idx, RandomGenerator random) {
        MultiRepresentationDataset overSampled = SamplePerturbator.create(learnedNetwork, dataset, idx,random).run();
        return new JRipExtraction(overSampled, dataset, idx,learnedNetwork);
    }
}
