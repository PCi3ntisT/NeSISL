package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation.CascadeCorrelationSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation.CascadeCorrelation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.dynamicNodeCreation.DNCSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.dynamicNodeCreation.DynamicNodeCreation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.regent.Regent;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.regent.RegentSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGen;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGenSettings;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiCrossvalidation;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiRepresentationDataset;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Learnable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.experiments.RuleSetInitable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation.ExperimentResult;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.trepan.Trepan;
import main.java.cz.cvut.ida.nesisl.modules.trepan.TrepanResults;
import main.java.cz.cvut.ida.nesisl.modules.weka.WekaJRip;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.AccuracyTrimmer;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.RuleAccuracy;
import weka.core.Instances;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by EL on 9.2.2016.
 */
public class Main {

    // numeric attributes are not possible to predict in this version

    // general setting; TREPAN_RUN is needed to be true if you want to run the whole NSL cycle
    public static final boolean TREPAN_RUN = true;

    private static final double percentualAccuracyOfOriginalDataset = 0.7;


    // settings for random generator
    private static final Double SIGMA = 1d;
    private static final Double MU = 0.0d;
    private static final Integer SEED = 13;

    //
    private static final String CYCLE_TOKEN = "-cycle";

    public static void main(String arg[]) throws FileNotFoundException {
        Arrays.stream(arg).forEach(e -> System.out.println(e + "\t"));
        System.out.println();
        System.out.println("inner parameters");
        System.out.println("sigma:\t" + SIGMA);
        System.out.println("mu:\t" + MU);
        System.out.println("seed:\t" + SEED);
        System.out.println("percentualAccuracyOfOriginalDataset:\t" + percentualAccuracyOfOriginalDataset);
        System.out.println("TREPAN_RUN:\t" + TREPAN_RUN);
        System.out.println("end of info");
        System.out.println();

        if (arg.length < 4) {
            System.out.println("Not enough arguments. Right setting in form 'algorithmName numberOfRuns datasetFile weightLearningSettingFile [...]' or '" + CYCLE_TOKEN + " numberOfCycleRuns algorithmName numberOfFolds datasetFile weightLearningSettingFile [...]'. Possible algorithms KBANN, CasCor, DNC, SLSF, TopGen, REGENT; write as first argument to see more. TODO this MSG...");
            //System.out.println("Not enough arguments. Right setting in form 'algorithmName numberOfRuns datasetFile weightLearningSettingFile [...]'. Possible algorithms KBANN, CasCor, DNC, SLSF, TopGen, REGENT; write as first argument to see more. TODO this MSG...");
            System.exit(0);
        }

        RandomGeneratorImpl randomGenerator = new RandomGeneratorImpl(SIGMA, MU, SEED);

        int numberOfSingleCycles = 0;
        boolean singleCycle = CYCLE_TOKEN.equals(arg[0]);
        if (singleCycle) {
            numberOfSingleCycles = Tools.parseInt(arg[1], "The second argument (number of single cycle repeats) must be integer.\nArgument input instead '" + arg[1] + "'.");
            String[] swap = new String[arg.length - 2];
            final String[] finalArg = arg;
            IntStream.rangeClosed(2, arg.length).forEach(idx -> swap[idx] = finalArg[idx]);
            arg = swap;

            throw new UnsupportedOperationException("TODO");
        }


        int numberOfRepeats = Tools.parseInt(arg[1], "The second/fourth argument (number of repeats) must be integer.\nArgument input instead '" + arg[1] + "'.");
        File datasetFile = Tools.retrieveFile(arg[2], "The third/fifth argument (datasetFile) does not exist.\nArgument input instead '" + arg[2] + "'.");
        File wlsFile = Tools.retrieveFile(arg[3], "The fourth/sixth argument (weightLearningSettingFile) does not exist.\nArgument input instead '" + arg[3] + "'.");

        // turning off regularization (appendix)
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);
        if (!"SLSF".equals(arg[0])) {
            wls = WeightLearningSetting.turnOffRegularization(wls);
        }


        // TODO: add parameter for reading normalization
        boolean normalize = true; // not needed since only nominal input are used
        //Pair<Dataset, Instances> datasetsPair = DatasetImpl.parseAndGetDatasets(datasetFile, normalize);
        MultiRepresentationDataset multiRepre = DatasetImpl.parseMultiRepresentationDataset(datasetFile, normalize);

        Dataset nesislDataset = multiRepre.getNesislDataset();

        if (nesislDataset.getOutputFactOrder().size() < 2 && wls.isLearningWithCrossEntropy()) {
            wls = WeightLearningSetting.turnOffCrossentropyLearning(wls);
        }


        System.out.println("zmenit backprop na SGD");

        Main main = new Main();
        switch (arg[0]) {
            case "CasCor":
                main.runCasCor(arg, numberOfRepeats, multiRepre, wls, randomGenerator);
                break;
            case "DNC":
                main.runDNC(arg, numberOfRepeats, multiRepre, wls, randomGenerator);
                break;
            case "KBANN":
                main.runKBANN(arg, numberOfRepeats, multiRepre, wls, randomGenerator);
                break;
            case "TopGen":
                main.runTopGen(arg, numberOfRepeats, multiRepre, wls, randomGenerator);
                break;
            case "REGENT":
                main.runREGENT(arg, numberOfRepeats, multiRepre, wls, randomGenerator);
                break;
            //case "backprop":
            //    main.runBackprop(arg, numberOfRepeats, dataset, wls, randomGenerator);
            //    break;
            //case "fullyConnected":
            //    main.runFullyConnected(arg, numberOfRepeats, dataset, wls, randomGenerator);
            //    break;
            default:
                System.out.println("Unknown algorithm '" + arg[0] + "'.");
                break;
        }
    }

    private void runAndStoreExperiments(RuleSetInitable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, MultiCrossvalidation crossval, File settingFile, WeightLearningSetting wls) {
        List<ExperimentResult> results = runExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, wls);
        ExperimentResult.storeResults(results, algName, crossval.getOriginalFile(), settingFile, wls);
    }

    private List<ExperimentResult> runExperiments(RuleSetInitable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, MultiCrossvalidation crossval, File settingFile, WeightLearningSetting wls) {
        System.out.println(numberOfRepeats);
        return IntStream.range(0, numberOfRepeats)
                //.parallel()
                .mapToObj(idx -> {
                    System.out.println("\n\n--------- fold " + idx + ":\t dataset extraction\n\n");

                    Dataset nesislDataset = crossval.getDataset(idx);
                    Instances wekaDataset = crossval.getTrainWekaDataset(nesislDataset);

                    System.out.println("\n\n--------- fold " + idx + ":\t rule set learning\n\n");
                    RuleSet ruleSet = WekaJRip.create(wekaDataset).getRuleSet();

                    System.out.println(ruleSet.getTheory());
                    System.out.println(ruleSet.getComplexity());

                    // popripade nejaky trimmer nebo relabelling
                    // ruleSet = RuleTrimmer.create(ruleSet).getRuleSet();
                    // RuleSet a1 = AntecedentsTrimmer.create(ruleSet).getRuleSet();
                    // Dataset relabeled = Relabeling.create(nesislDataset, ruleSet).getDataset();
                    System.out.println("\n\n--------- fold " + idx + ":\t ruleset trimming\n\n");
                    ruleSet = AccuracyTrimmer.create(ruleSet, nesislDataset).getRuleSetWithTrimmedAccuracy(percentualAccuracyOfOriginalDataset);

                    System.out.println(ruleSet.getTheory());
                    System.out.println(ruleSet.getComplexity());
                    File ruleFile = Tools.storeToTemporaryFile(ruleSet.getTheory());
                    /* end of rule mining and trimming */


                    ExperimentResult currentResult = new ExperimentResult(idx, algName, crossval.getOriginalFile(), settingFile, wls);

                    System.out.println("\n\n--------- fold " + idx + ":\t network initialization\n\n");
                    NeuralNetworkOwner alg = initialize.initialize(ruleFile);
                    currentResult.setInitNetwork(alg.getNeuralNetwork().getCopy());

                    System.out.println("\n\n--------- fold " + idx + ":\t structure & weight learning\n\n");
                    long start = System.currentTimeMillis();
                    NeuralNetwork learnedNetwork = learn.learn(alg, nesislDataset);
                    long end = System.currentTimeMillis();

                    /*
                    Tools.printEvaluation(learnedNetwork, dataset);
                    System.out.println("\t" + learnedNetwork.getClassifier().getThreshold());
                    */


                    long ruleSetComplexity = (null == ruleSet) ? 0 : ruleSet.getComplexity();
                    double trainRuleAcc = (null == ruleSet) ? 0 : RuleAccuracy.create(ruleSet).computeTrainAccuracy(nesislDataset);
                    double testRuleAcc = (null == ruleSet) ? 0 : RuleAccuracy.create(ruleSet).computeTestAccuracy(nesislDataset);

                    System.out.println("\n\n--------- fold " + idx + ":\t result storing & TREPAN learning\n\n");
                    if (TREPAN_RUN) {
                        System.out.println("\n\n--------- fold " + idx + ":\t TREPAN learning \n\n");
                        TrepanResults trepan = Trepan.create(learnedNetwork, nesislDataset, algName, idx, currentResult.getMyAdress()).run();
                        currentResult.addExperiment(learnedNetwork, start, end, nesislDataset, trepan, ruleSetComplexity, trainRuleAcc, testRuleAcc);
                    } else {
                        currentResult.addExperiment(learnedNetwork, start, end, nesislDataset, ruleSetComplexity, trainRuleAcc, testRuleAcc);
                    }

                    // storing initial ruleSet
                    Tools.storeToFile(ruleSet.getTheory(), currentResult.getMyAdress() + File.separator + "initialTheory");
                    Tools.storeToFile(ruleSet.toString(), currentResult.getMyAdress() + File.separator + "ruleSet");


                    System.out.println("\n\n--------- fold " + idx + ":\t ending iteration\n\n");
                    return currentResult;
                }).collect(Collectors.toCollection(ArrayList::new));
    }

    private void runCasCor(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Cascade Correlation use 'CasCor   #ofRepeats  datasetFile  weightLearningSettingsFile  cascadeCorrelationSetting'");
        }
        String algName = "CasCor";

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        File settingFile = new File(arg[4]);
        CascadeCorrelationSetting ccSetting = CascadeCorrelationSetting.create(settingFile);

        RuleSetInitable<CascadeCorrelation> initialize = (ruleSetFile) -> CascadeCorrelation.create(dataset.getNesislDataset().getInputFactOrder(), dataset.getNesislDataset().getOutputFactOrder(), randomGenerator, new MissingValueKBANN(), wls.isLearningWithCrossEntropy());
        Learnable learn = (cascadeCorrelation, learningDataset) -> ((CascadeCorrelation) cascadeCorrelation).learn(learningDataset, finalWls, ccSetting);

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }


    private void runDNC(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Dynamic Node Creation use 'DNC   #ofRepeats  datasetFile  weightLearningSettingsFile  DNCSetting'");
        }
        String algName = "DNC";

        File settingFile = new File(arg[4]);
        DNCSetting dncSetting = DNCSetting.create(settingFile);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);

        RuleSetInitable<DynamicNodeCreation> initialize = (ruleSetFile) -> DynamicNodeCreation.create(dataset.getNesislDataset().getInputFactOrder(), dataset.getNesislDataset().getOutputFactOrder(), randomGenerator, new MissingValueKBANN(), wls.isLearningWithCrossEntropy());
        Learnable learn = (dnc, learningDataset) -> ((DynamicNodeCreation) dnc).learn(learningDataset, finalWls, dncSetting);

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

    public void runKBANN(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run KBANN use 'KBANN   #ofRepeats  datasetFile weightLearningSettingsFile  KBANNsetting    [ruleSpecificFile]'");
        }
        if (arg.length > 5) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }


        String algName = "KBANN";
        File settingFile = new File(arg[4]);
        KBANNSettings kbannSettings = KBANNSettings.create(randomGenerator, settingFile);

        List<Pair<Integer, ActivationFunction>> specificRules = new ArrayList<>();

        RuleSetInitable<KBANN> initialize = (ruleSetFile) -> KBANN.create(ruleSetFile, dataset.getNesislDataset(), specificRules, kbannSettings, wls.isLearningWithCrossEntropy());
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        Learnable learn = (kbann, learningDataset) -> ((KBANN) kbann).learn(learningDataset, finalWls);

        //Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

    private void runTopGen(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "TopGen";
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run TopGen use 'TopGen   #ofRepeats  datasetFile  weightLearningSettingsFile TopGenSettings'");
        }
        if (arg.length > 5) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        File settingFile = new File(arg[4]);
        TopGenSettings tgSetting = TopGenSettings.create(settingFile);

        RuleSetInitable<TopGen> initialize = (ruleSetFile) -> TopGen.create(ruleSetFile, specific, randomGenerator, tgSetting, dataset.getNesislDataset(), wls.isLearningWithCrossEntropy());
        Learnable learn = (topGen, learningDataset) -> ((TopGen) topGen).learn(learningDataset, finalWls, TopGenSettings.create(tgSetting));

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

    private void runREGENT(String[] arg, int numberOfRepeats, MultiRepresentationDataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "REGENT";
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run REGENT use 'REGENT   #ofRepeats  datasetFile  weightLearningSettingsFile RegentSetting'");
        }
        if (arg.length > 5) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);

        File settingFile = new File(arg[4]);
        RegentSetting regentSetting = RegentSetting.create(settingFile, randomGenerator);

        RuleSetInitable<Regent> initialize = (ruleSetFile) -> Regent.create(ruleSetFile, specific, randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting, dataset.getNesislDataset(), wls.isLearningWithCrossEntropy());
        Learnable learn = (regent, learningDataset) -> ((Regent) regent).learn(learningDataset, finalWls, RegentSetting.create(regentSetting), new KBANNSettings(randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting.getTopGenSettings().perturbationMagnitude()));
        //Learnable learn = (regent, learningDataset) -> ((Regent) regent).learn(learningDataset, finalWls, regentSetting, new KBANNSettings(randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting.getTopGenSettings().perturbationMagnitude()));

        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(dataset, randomGenerator, numberOfRepeats);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

}
