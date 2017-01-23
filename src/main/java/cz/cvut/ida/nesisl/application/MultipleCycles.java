package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.regent.Regent;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.regent.RegentSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGen;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGenSettings;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiCrossvalidation;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiRepresentationDataset;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Learnable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.experiments.RuleSetInitable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation.ExperimentResult;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.trepan.MofNTreeFactory;
import main.java.cz.cvut.ida.nesisl.modules.trepan.Trepan;
import main.java.cz.cvut.ida.nesisl.modules.trepan.TrepanResults;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTree;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTreeReader;
import main.java.cz.cvut.ida.nesisl.modules.weka.WekaJRip;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSetDescriptionLengthFactor;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.AccuracyTrimmer;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.RuleAccuracy;
import weka.core.Instances;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by EL on 1.11.2016.
 */
public class MultipleCycles {

    private final int numberOfCycles;

    public MultipleCycles(int numberOfSingleCycles) {
        this.numberOfCycles = numberOfSingleCycles;

        System.out.println("\nrunning cycle with settings:\nnumberOfCycles:\t" + numberOfCycles + "\n");

        System.out.println("number of cycles should be parametrized as it is in MAIN, not as it is here given by hardcoding");
    }

    // awful recopy :(
    public void runKBANN(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator, RuleSet ruleSet, File ruleFile) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run KBANN use 'KBANN   #ofRepeats  datasetFile backgroundData  weightLearningSettingsFile  KBANNsetting    [ruleSpecificFile]'");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }

        String algName = "KBANN";
        File settingFile = new File(arg[5]);
        KBANNSettings kbannSettings = KBANNSettings.create(randomGenerator, settingFile);

        List<Pair<Integer, ActivationFunction>> specificRules = new ArrayList<>();

        RuleSetInitable<KBANN> initialize = (ruleSetFile) -> KBANN.create(ruleSetFile, dataset.getNesislDataset(), specificRules, kbannSettings, wls.isLearningWithCrossEntropy());
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        Learnable learn = (kbann, learningDataset) -> ((KBANN) kbann).learn(learningDataset, finalWls);

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreCycles(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet, ruleFile);
    }

    public void runTopGen(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator, RuleSet ruleSet, File ruleFile) throws FileNotFoundException {
        String algName = "TopGen";
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run TopGen use 'TopGen   #ofRepeats  datasetFile   backgroundData  weightLearningSettingsFile TopGenSettings'");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        File settingFile = new File(arg[5]);
        TopGenSettings tgSetting = TopGenSettings.create(settingFile);

        RuleSetInitable<TopGen> initialize = (ruleSetFile) -> TopGen.create(ruleSetFile, specific, randomGenerator, tgSetting, dataset.getNesislDataset(), wls.isLearningWithCrossEntropy());
        Learnable learn = (topGen, learningDataset) -> ((TopGen) topGen).learn(learningDataset, finalWls, TopGenSettings.create(tgSetting));

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreCycles(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet, ruleFile);
    }

    public void runREGENT(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator, RuleSet ruleSet, File ruleFile) throws FileNotFoundException {
        String algName = "REGENT";
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run REGENT use 'REGENT   #ofRepeats  datasetFile   backgroundData    weightLearningSettingsFile RegentSetting'");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);

        File settingFile = new File(arg[5]);
        RegentSetting regentSetting = RegentSetting.create(settingFile, randomGenerator);

        RuleSetInitable<Regent> initialize = (ruleSetFile) -> Regent.create(ruleSetFile, specific, randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting, dataset.getNesislDataset(), wls.isLearningWithCrossEntropy());
        Learnable learn = (regent, learningDataset) -> ((Regent) regent).learn(learningDataset, finalWls, RegentSetting.create(regentSetting), new KBANNSettings(randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting.getTopGenSettings().perturbationMagnitude()));

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreCycles(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet, ruleFile);
    }

    private void runAndStoreCycles(RuleSetInitable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, MultiCrossvalidation crossval, File settingFile, WeightLearningSetting wls, RuleSet ruleSet, File ruleFile) {
        List<List<ExperimentResult>> results = runCycles(initialize, learn, numberOfRepeats, algName, crossval, settingFile, wls, ruleSet, ruleFile);
        ExperimentResult.storeCyclesResult(results, algName, crossval.getOriginalFile(), settingFile, wls);
    }

    private List<List<ExperimentResult>> runCycles(RuleSetInitable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, MultiCrossvalidation crossval, File settingFile, WeightLearningSetting wls, RuleSet ruleSet, File ruleFile) {
        System.out.println(numberOfRepeats);
        return IntStream.range(0, numberOfRepeats)
                //.parallel()
                .mapToObj(idx -> {
                    System.out.println("\n\n--------- fold " + idx + ":\t dataset extraction\n\n");

                    Dataset nesislDataset = crossval.getDataset(idx);


                    long ruleSetDescriptionLength = (null == ruleSet) ? 0 : RuleSetDescriptionLengthFactor.getDefault().computeDescriptionLength(ruleSet);
                    double trainRuleAcc = (null == ruleSet) ? 0 : RuleAccuracy.create(ruleSet).computeTrainAccuracy(nesislDataset);
                    double testRuleAcc = (null == ruleSet) ? 0 : RuleAccuracy.create(ruleSet).computeTestAccuracy(nesislDataset);

                    List<ExperimentResult> resultsList = neuralSybolicCycle(initialize, learn, algName, crossval, settingFile, wls, idx, nesislDataset, ruleSet, ruleFile, ruleSetDescriptionLength, trainRuleAcc, testRuleAcc);

                    System.out.println("\n\n--------- fold " + idx + ":\t ending iteration\n\n");

                    return resultsList;
                }).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<ExperimentResult> neuralSybolicCycle(RuleSetInitable<? extends NeuralNetworkOwner> initialize, Learnable learn, String algName, MultiCrossvalidation crossval, File settingFile, WeightLearningSetting wls, int idx, Dataset nesislDataset, RuleSet ruleSet, File ruleFile, long ruleSetDescriptionLength, double trainRuleAcc, double testRuleAcc) {
        List<ExperimentResult> result = new ArrayList<>();
        for (int cycleNumber = 0; cycleNumber < numberOfCycles; cycleNumber++) {
            ExperimentResult currentResult = new ExperimentResult(idx, algName, crossval.getOriginalFile(), settingFile, wls, cycleNumber);

            System.out.println("\n--------- fold " + idx + ", cycle " + cycleNumber + ":\t network initialization\n");
            NeuralNetworkOwner alg = initialize.initialize(ruleFile);
            currentResult.setInitNetwork(alg.getNeuralNetwork().getCopy());

            System.out.println("\n--------- fold " + idx + ", cycle " + cycleNumber + ":\t structure & weight learning\n");
            long start = System.currentTimeMillis();
            NeuralNetwork learnedNetwork = learn.learn(alg, nesislDataset);
            long end = System.currentTimeMillis();

            System.out.println("\n--------- fold " + idx + ", cycle " + cycleNumber + ":\t result storing & TREPAN learning\n\n");
            TrepanResults trepan;
            if (Main.TREPAN_RUN) {
                System.out.println("\n--------- fold " + idx + ", cycle " + cycleNumber + ":\t TREPAN learning \n");
                trepan = Trepan.create(learnedNetwork, nesislDataset, algName, idx, currentResult.getMyAdress()).run();
                currentResult.addExperiment(learnedNetwork, start, end, nesislDataset, trepan, ruleSetDescriptionLength, trainRuleAcc, testRuleAcc);
            } else {
                currentResult.addExperiment(learnedNetwork, start, end, nesislDataset, ruleSetDescriptionLength, trainRuleAcc, testRuleAcc);
            }

            if(0 == cycleNumber) {
                // storing initial ruleSet
                Tools.storeToFile(ruleSet.getTheory(), currentResult.getMyAdress() + File.separator + "initialTheory");
                Tools.storeToFile(ruleSet.toString(), currentResult.getMyAdress() + File.separator + "ruleSet");
            }else if(Main.TREPAN_RUN){
                DotTree tree = DotTreeReader.getDefault().create(trepan.getTreeFile());
                Tools.storeToFile(MofNTreeFactory.getDefault().getTheory(tree, currentResult.getFinalNetwork()), currentResult.getMyAdress() + File.separator + "initialTheory_" + cycleNumber);
                currentResult.setRuleSetDescriptionLength(currentResult.getMofNDecisionTreeDescriptionLength());
            }

            // computing & forwarding parameters for next cycle
            trainRuleAcc = currentResult.getTrepanTrainAcc();
            testRuleAcc = currentResult.getTrepanTestAcc();
            ruleSetDescriptionLength = currentResult.getMofNDecisionTreeDescriptionLength();

            DotTree tree = DotTreeReader.getDefault().create(currentResult.getMofNDecisionTreeFile());
            ruleFile = Tools.storeToTemporaryFile(MofNTreeFactory.getDefault().getTheory(tree, learnedNetwork));

            result.add(currentResult);
        }
        return result;
    }

    public static RuleSet mineAndTrimmeRuleSet(int idx, Dataset nesislDataset, Instances wekaDataset) {
        System.out.println("\n\n--------- fold " + idx + ":\t rule set learning\n\n");
        RuleSet ruleSet = WekaJRip.create(wekaDataset,nesislDataset).getRuleSet();

        // popripade nejaky trimmer nebo relabelling
        // ruleSet = RuleTrimmer.create(ruleSet).getRuleSet();
        // RuleSet a1 = AntecedentsTrimmer.create(ruleSet).getRuleSet();
        // Dataset relabeled = Relabeling.create(nesislDataset, ruleSet).getDataset();
        System.out.println("\n\n--------- fold " + idx + ":\t ruleset trimming\n\n");
        ruleSet = AccuracyTrimmer.create(ruleSet, nesislDataset).getRuleSetWithTrimmedAccuracy(Main.percentualAccuracyOfOriginalDataset);
        return ruleSet;
    }

    public static RuleSet mineAndTrimmeRule(Dataset nesislDataset, Instances wekaDataset) {
        RuleSet ruleSet = WekaJRip.create(wekaDataset,nesislDataset).getRuleSet();

        // do not solve the consistency here
        //nesislDataset.makeConsistentStatefully(ruleSet);


        // popripade nejaky trimmer nebo relabelling
        // ruleSet = RuleTrimmer.create(ruleSet).getRuleSet();
        // RuleSet a1 = AntecedentsTrimmer.create(ruleSet).getRuleSet();
        // Dataset relabeled = Relabeling.create(nesislDataset, ruleSet).getDataset();
        ruleSet = AccuracyTrimmer.create(ruleSet, nesislDataset).getRuleSetWithTrimmedAccuracy(Main.percentualAccuracyOfOriginalDataset);
        return ruleSet;
    }

}
