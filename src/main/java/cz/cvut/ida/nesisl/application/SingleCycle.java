package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.extraction.jripExtractor.JRipExtraction;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.cascadeCorrelation.CascadeCorrelation;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.cascadeCorrelation.CascadeCorrelationSetting;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.dynamicNodeCreation.DNCSetting;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.dynamicNodeCreation.DynamicNodeCreation;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.regent.Regent;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.regent.RegentSetting;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.topGen.TopGen;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.topGen.TopGenSettings;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiCrossvalidation;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiRepresentationDataset;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Learnable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.experiments.RuleSetInitable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation.ExperimentResult;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.extraction.trepan.Trepan;
import main.java.cz.cvut.ida.nesisl.modules.extraction.TrepanResults;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSetDescriptionLengthFactor;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.RuleAccuracy;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.pyramid.Pyramid;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by EL on 2.11.2016.
 */
public class SingleCycle {

    private final double percentualAccuracyOfOriginalDataset;

    public SingleCycle(double percentualAccuracyOfOriginalDataset) {
        this.percentualAccuracyOfOriginalDataset = percentualAccuracyOfOriginalDataset;
    }

    private void runAndStoreExperiments(RuleSetInitable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, MultiCrossvalidation crossval, File settingFile, WeightLearningSetting wls, RuleSet ruleSet, File ruleFile, RandomGenerator randomGenerator) {
        List<ExperimentResult> results = runExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, wls, ruleSet, ruleFile,randomGenerator);
        ExperimentResult.storeResults(results, algName, crossval.getOriginalFile(), settingFile, wls);
    }

    private List<ExperimentResult> runExperiments(RuleSetInitable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, MultiCrossvalidation crossval, File settingFile, WeightLearningSetting wls, RuleSet ruleSet, File ruleFile, RandomGenerator randomGenerator) {
        System.out.println(numberOfRepeats);
        return IntStream.range(0, numberOfRepeats)
                //.parallel()
                .mapToObj(idx -> {
                    System.out.println("\n--------- fold " + idx + ":\t dataset extraction\n");

                    Dataset nesislDataset = crossval.getDataset(idx);
                    //Instances wekaDataset = crossval.getTrainWekaDataset(nesislDataset);

                    //System.out.println("\n--------- fold " + idx + ":\t rule set learning\n");
                    //RuleSet ruleSet = WekaJRip.createTest(wekaDataset).getRuleSet();

                    // popripade nejaky trimmer nebo relabelling
                    // ruleSet = RuleTrimmer.createTest(ruleSet).getRuleSet();
                    // RuleSet a1 = AntecedentsTrimmer.createTest(ruleSet).getRuleSet();
                    // Dataset relabeled = Relabeling.createTest(nesislDataset, ruleSet).getDataset();
                    //System.out.println("\n--------- fold " + idx + ":\t ruleset trimming\n");
                    //ruleSet = AccuracyTrimmer.createTest(ruleSet, nesislDataset).getRuleSetWithTrimmedAccuracy(percentualAccuracyOfOriginalDataset);

                    //File ruleFile = Tools.storeToTemporaryFile(ruleSet.getTheory());
                    /* end of rule mining and trimming */


                    ExperimentResult currentResult = new ExperimentResult(idx, algName, crossval.getOriginalFile(), settingFile, wls);

                    System.out.println("\n--------- fold " + idx + ":\t network initialization\n");
                    NeuralNetworkOwner alg = initialize.initialize(ruleFile);
                    currentResult.setInitNetwork(alg.getNeuralNetwork().getCopy());

                    System.out.println("\n--------- fold " + idx + ":\t structure & weight learning\n");
                    long start = System.currentTimeMillis();
                    NeuralNetwork learnedNetwork = learn.learn(alg, nesislDataset);
                    long end = System.currentTimeMillis();


                    long ruleSetDescriptionLength = (null == ruleSet) ? 0 : RuleSetDescriptionLengthFactor.getDefault().computeDescriptionLength(ruleSet);
                    double trainRuleAcc = (null == ruleSet) ? 0 : RuleAccuracy.create(ruleSet).computeTrainAccuracy(nesislDataset);
                    double testRuleAcc = (null == ruleSet) ? 0 : RuleAccuracy.create(ruleSet).computeTestAccuracy(nesislDataset);

                    System.out.println("\n--------- fold " + idx + ":\t result storing & TREPAN learning\n");
                    switch (Main.RULE_EXTRACTOR) {
                        case TREPAN:
                        System.out.println("\n--------- fold " + idx + ":\t TREPAN learning \n");
                        TrepanResults trepan = Trepan.create(learnedNetwork, nesislDataset, algName, idx, currentResult.getMyAdress()).run();
                        currentResult.addExperiment(learnedNetwork, start, end, nesislDataset, trepan, ruleSetDescriptionLength, trainRuleAcc, testRuleAcc);
                            break;
                        case JRIP:
                            MultiRepresentationDataset multi = crossval.getMultiRepresentationDataset(idx);
                            TrepanResults jrip = JRipExtraction.create(learnedNetwork, multi, idx, randomGenerator).run();
                            currentResult.addExperiment(learnedNetwork, start, end, nesislDataset, jrip, ruleSetDescriptionLength, trainRuleAcc, testRuleAcc);
                            break;
                        default:
                            System.out.printf("Unknow rule extractor");
                            currentResult.addExperiment(learnedNetwork, start, end, nesislDataset, ruleSetDescriptionLength, trainRuleAcc, testRuleAcc);
                    }

                    // storing initial ruleSet
                    if (null != ruleSet) {
                        Tools.storeToFile(ruleSet.getTheory(), currentResult.getMyAdress() + File.separator + "initialTheory");
                        Tools.storeToFile(ruleSet.toString(), currentResult.getMyAdress() + File.separator + "ruleSet");
                    }

                    System.out.println("\n--------- fold " + idx + ":\t ending iteration\n");
                    return currentResult;
                }).collect(Collectors.toCollection(ArrayList::new));
    }

    public void runCasCor(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run Cascade Correlation use 'CasCor   #ofRepeats  datasetFile  backgroundData  weightLearningSettingsFile  cascadeCorrelationSetting'");
        }
        String algName = "CasCor";

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        File settingFile = new File(arg[5]);
        CascadeCorrelationSetting ccSetting = CascadeCorrelationSetting.create(settingFile);

        RuleSetInitable<CascadeCorrelation> initialize = (ruleSetFile) -> CascadeCorrelation.create(dataset.getNesislDataset().getInputFactOrder(), dataset.getNesislDataset().getOutputFactOrder(), randomGenerator, new MissingValueKBANN(), wls.isLearningWithCrossEntropy());
        Learnable learn = (cascadeCorrelation, learningDataset) -> ((CascadeCorrelation) cascadeCorrelation).learn(learningDataset, finalWls, ccSetting);

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, null, null,randomGenerator);
    }

    public void runDNC(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run Dynamic Node Creation use 'DNC   #ofRepeats  datasetFile   backgroundData  weightLearningSettingsFile  DNCSetting'");
        }
        String algName = "DNC";

        File settingFile = new File(arg[5]);
        DNCSetting dncSetting = DNCSetting.create(settingFile);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);

        RuleSetInitable<DynamicNodeCreation> initialize = (ruleSetFile) -> DynamicNodeCreation.create(dataset.getNesislDataset().getInputFactOrder(), dataset.getNesislDataset().getOutputFactOrder(), randomGenerator, new MissingValueKBANN(), wls.isLearningWithCrossEntropy());
        Learnable learn = (dnc, learningDataset) -> ((DynamicNodeCreation) dnc).learn(learningDataset, finalWls, dncSetting);

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls,null,null, randomGenerator);
    }

    public void runPYRAMID(String[] arg, int numberOfRepeats, MultiRepresentationDataset multiRepre, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator, RuleSet ruleSet, File ruleFile) {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run PYRAMID use 'PYRAMID   #ofRepeats  datasetFile   backgroundData  weightLearningSettingsFile #ofHiddenLayers'");
        }
        String algName = "PYRAMID";

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        int numberOfHiddenLayers = Integer.parseInt(arg[5]);

        RuleSetInitable<Pyramid> initialize = (ruleSetFile) -> Pyramid.create(multiRepre.getNesislDataset().getInputFactOrder(), multiRepre.getNesislDataset().getOutputFactOrder(), randomGenerator, new MissingValueKBANN(), wls.isLearningWithCrossEntropy(),numberOfHiddenLayers);
        Learnable learn = (dnc, learningDataset) -> ((Pyramid) dnc).learn(learningDataset, finalWls);

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(multiRepre, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, wls.getFile(), finalWls,null,null, randomGenerator);
    }


    public void runKBANN(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator, RuleSet ruleSet, File ruleFile) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run KBANN use 'KBANN   #ofRepeats  datasetFile backgroundData weightLearningSettingsFile  KBANNsetting    [ruleSpecificFile]'");
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

        //Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet, ruleFile, randomGenerator);
    }

    public void runTopGen(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator, RuleSet ruleSet, File ruleFile) throws FileNotFoundException {
        String algName = "TopGen";
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run TopGen use 'TopGen   #ofRepeats  datasetFile  backgroundData   weightLearningSettingsFile TopGenSettings'");
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
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet, ruleFile, randomGenerator);
    }

    public void runREGENT(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator, RuleSet ruleSet, File ruleFile) throws FileNotFoundException {
        String algName = "REGENT";
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run REGENT use 'REGENT   #ofRepeats  datasetFile  backgroundData   weightLearningSettingsFile RegentSetting'");
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
        //Learnable learn = (regent, learningDataset) -> ((Regent) regent).learn(learningDataset, finalWls, regentSetting, new KBANNSettings(randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting.getTopGenSettings().perturbationMagnitude()));

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet, ruleFile, randomGenerator);
    }



}
