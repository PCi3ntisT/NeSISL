package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
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
import main.java.cz.cvut.ida.nesisl.modules.dataset.Crossvalidation;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Initable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Learnable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation.ExperimentResult;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.extraction.trepan.Trepan;
import main.java.cz.cvut.ida.nesisl.modules.extraction.TrepanResults;
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
public class MainJRipOnWholeData {

    // numeric attributes are not possible to predict in this version

    public static final boolean TREPAN_RUN = true;

    private Integer numberOfFolds = 10;

    // automatic version with JRip and others
    public static void main(String arg[]) throws FileNotFoundException {

        Arrays.stream(arg).forEach(e -> System.out.println(e + "\t"));
        System.out.println();

        if (arg.length < 4) {
            System.out.println("Not enough arguments. Right setting in form 'algorithmName numberOfRuns datasetFile weightLearningSettingFile [...]'. Possible algorithms KBANN, CasCor, DNC, SLSF, TopGen, REGENT; write as first argument to see more. TODO this MSG");
            System.exit(0);
        }

        double simga = 1d;
        double mu = 0.0d;
        int seed = 13;
        RandomGeneratorImpl randomGenerator = new RandomGeneratorImpl(simga, mu, seed);

        int numberOfRepeats = Tools.parseInt(arg[1], "The second argument (number of repeats) must be integer.\nArgument input instead '" + arg[1] + "'.");
        File datasetFile = Tools.retrieveFile(arg[2], "The third argument (datasetFile) does not exist.\nArgument input instead '" + arg[2] + "'.");
        File wlsFile = Tools.retrieveFile(arg[3], "The fourth argument (weightLearningSettingFile) does not exist.\nArgument input instead '" + arg[3] + "'.");

        // vypinani regularizace (appendix)
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile, randomGenerator.getRandom());
        if (!"SLSF".equals(arg[0])) {
            wls = WeightLearningSetting.turnOffRegularization(wls);
        }


        // TODO NACITANI NORMALIZACE ZAJISTIT :)
        boolean normalize = true; // not needed since only nominal input are used
        Pair<Dataset, Instances> datasetsPair = DatasetImpl.parseAndGetDatasets(datasetFile, normalize);

        Dataset nesislDataset = datasetsPair.getLeft();
        Instances wekaDataset = datasetsPair.getRight();

        if (nesislDataset.getOutputFactOrder().size() < 2 && wls.isLearningWithCrossEntropy()) {
            wls = WeightLearningSetting.turnOffCrossentropyLearning(wls);
        }

        // TODO upravit nastaveni
        RuleSet ruleSet = WekaJRip.create(wekaDataset,nesislDataset).getRuleSet();


        System.out.println(ruleSet.getTheory());
        System.out.println(ruleSet.getComplexity());

        // popripade nejaky trimmer nebo relabelling
        // ruleSet = RuleTrimmer.createTest(ruleSet).getRuleSet();
        // RuleSet a1 = AntecedentsTrimmer.createTest(ruleSet).getRuleSet();
        // Dataset relabeled = Relabeling.createTest(nesislDataset, ruleSet).getDataset();
        Dataset dataset = nesislDataset;
        double percentualAccuracyOfOriginalDataset = 0.7;
        ruleSet = AccuracyTrimmer.create(ruleSet, nesislDataset).getRuleSetWithTrimmedAccuracy(percentualAccuracyOfOriginalDataset);

        System.out.println(ruleSet.getTheory());
        System.out.println(ruleSet.getComplexity());


        /*System.out.println(ruleSet.getTheory());

        System.out.println("tady jeste predelat odrezavani ten pravidel - je to trochu jine kdyz je pouze jedna trida");
        System.out.println("zaroven to predelat tak aby to accuracy orezaneho byla, dejme tomu, 75% puvodni accuracy");
        System.out.println("kolik mel ten JRip ruleset pre oreyanim accuracy?");

        ruleSet = RulesTrimmer.createTest(ruleSet, 3);
        System.out.println(ruleSet.getTheory());
        */

        MainJRipOnWholeData main = new MainJRipOnWholeData();
        switch (arg[0]) {
            case "CasCor":
                main.runCasCor(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "DNC":
                main.runDNC(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "KBANN":
                main.runKBANN(arg, numberOfRepeats, dataset, wls, ruleSet, randomGenerator);
                break;
            case "TopGen":
                main.runTopGen(arg, numberOfRepeats, dataset, wls, ruleSet, randomGenerator);
                break;
            case "REGENT":
                main.runREGENT(arg, numberOfRepeats, dataset, wls, ruleSet, randomGenerator);
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

    private void runAndStoreExperiments(Initable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, Crossvalidation crossval, File settingFile, WeightLearningSetting wls, RuleSet ruleSet) {
        List<ExperimentResult> results = runExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, wls, ruleSet);
        ExperimentResult.storeResults(results, algName, crossval.getOriginalFile(), settingFile, wls);
    }

    private List<ExperimentResult> runExperiments(Initable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, Crossvalidation crossval, File settingFile, WeightLearningSetting wls, RuleSet ruleSet) {
        System.out.println(numberOfRepeats);
        return IntStream.range(0, numberOfRepeats)
                //.parallel()
                .mapToObj(idx -> {
                    ExperimentResult currentResult = new ExperimentResult(idx, algName, crossval.getOriginalFile(), settingFile, wls);
                    NeuralNetworkOwner alg = initialize.initialize();
                    currentResult.setInitNetwork(alg.getNeuralNetwork().getCopy());

                    Dataset dataset = crossval.getDataset(idx);

                    long start = System.currentTimeMillis();
                    NeuralNetwork learnedNetwork = learn.learn(alg, dataset);
                    long end = System.currentTimeMillis();

                    /*
                    Tools.printEvaluation(learnedNetwork, dataset);
                    System.out.println("\t" + learnedNetwork.getClassifier().getThreshold());
                    */

                    long ruleSetComplexity = (null == ruleSet) ? 0 : ruleSet.getComplexity();
                    double trainRuleAcc = (null == ruleSet) ? 0 : RuleAccuracy.create(ruleSet).computeTrainAccuracy(dataset);
                    double testRuleAcc = (null == ruleSet) ? 0 : RuleAccuracy.create(ruleSet).computeTestAccuracy(dataset);

                    if (TREPAN_RUN) {
                        TrepanResults trepan = Trepan.create(learnedNetwork, dataset, algName, idx, currentResult.getMyAdress()).run();
                        currentResult.addExperiment(learnedNetwork, start, end, dataset, trepan, ruleSetComplexity, trainRuleAcc, testRuleAcc);
                    } else {
                        currentResult.addExperiment(learnedNetwork, start, end, dataset, ruleSetComplexity, trainRuleAcc, testRuleAcc);
                    }
                    return currentResult;
                }).collect(Collectors.toCollection(ArrayList::new));
    }

    private void runCasCor(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Cascade Correlation use 'CasCor   #ofRepeats  datasetFile  weightLearningSettingsFile  cascadeCorrelationSetting'");
        }
        String algName = "CasCor";

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        File settingFile = new File(arg[4]);
        CascadeCorrelationSetting ccSetting = CascadeCorrelationSetting.create(settingFile);

        Initable<CascadeCorrelation> initialize = () -> CascadeCorrelation.create(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator, new MissingValueKBANN(), wls.isLearningWithCrossEntropy());
        Learnable learn = (cascadeCorrelation, learningDataset) -> ((CascadeCorrelation) cascadeCorrelation).learn(learningDataset, finalWls, ccSetting);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, null);
    }

    private void runDNC(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Dynamic Node Creation use 'DNC   #ofRepeats  datasetFile  weightLearningSettingsFile  DNCSetting'");
        }
        String algName = "DNC";

        File settingFile = new File(arg[4]);
        DNCSetting dncSetting = DNCSetting.create(settingFile);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);

        Initable<DynamicNodeCreation> initialize = () -> DynamicNodeCreation.create(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator, new MissingValueKBANN(), wls.isLearningWithCrossEntropy());
        Learnable learn = (dnc, learningDataset) -> ((DynamicNodeCreation) dnc).learn(learningDataset, finalWls, dncSetting);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, null);
    }

    public void runKBANN(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RuleSet ruleSet, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
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

        File ruleFile = Tools.storeToTemporaryFile(ruleSet.getTheory());

        Initable<KBANN> initialize = () -> KBANN.create(ruleFile, dataset, specificRules, kbannSettings, wls.isLearningWithCrossEntropy());
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        Learnable learn = (kbann, learningDataset) -> ((KBANN) kbann).learn(learningDataset, finalWls);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet);
    }

    private void runTopGen(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RuleSet ruleSet, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
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
        File ruleFile = Tools.storeToTemporaryFile(ruleSet.getTheory());

        Initable<TopGen> initialize = () -> TopGen.create(ruleFile, specific, randomGenerator, tgSetting, dataset, wls.isLearningWithCrossEntropy());
        Learnable learn = (topGen, learningDataset) -> ((TopGen) topGen).learn(learningDataset, finalWls, TopGenSettings.create(tgSetting));

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet);
    }

    private void runREGENT(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RuleSet ruleSet, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "REGENT";
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run REGENT use 'REGENT   #ofRepeats  datasetFile  weightLearningSettingsFile RegentSetting'");
        }
        if (arg.length > 5) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);

        File ruleFile = Tools.storeToTemporaryFile(ruleSet.getTheory());
        File settingFile = new File(arg[4]);
        RegentSetting regentSetting = RegentSetting.create(settingFile, randomGenerator);

        Initable<Regent> initialize = () -> Regent.create(ruleFile, specific, randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting, dataset, wls.isLearningWithCrossEntropy());
        Learnable learn = (regent, learningDataset) -> ((Regent) regent).learn(learningDataset, finalWls, RegentSetting.create(regentSetting), new KBANNSettings(randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting.getTopGenSettings().perturbationMagnitude()));
        //Learnable learn = (regent, learningDataset) -> ((Regent) regent).learn(learningDataset, finalWls, regentSetting, new KBANNSettings(randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting.getTopGenSettings().perturbationMagnitude()));

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, ruleSet);
    }

    /* not updated
    private void runBackprop(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run backprop only on KBANN use 'backprop   #ofRepeats  datasetFile weightLearningSettingsFile  ruleFile   '");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }

        String algName = "backprop";
        File ruleFile = new File(arg[4]);
        File settingFile = new File(arg[5]);
        KBANNSettings kbannSettings = new KBANNSettings(randomGenerator, 1.0, true);

        List<Pair<Integer, ActivationFunction>> specificRules = new ArrayList<>();

        Initable<KBANN> initialize = () -> KBANN.createTest(ruleFile, dataset, specificRules, kbannSettings, SOFTMAX);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        Learnable learn = (kbann, learningDataset) -> ((KBANN) kbann).learn(learningDataset, finalWls);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, fakeNumberTheoryComplexityTodo);
    }
    */
    /* not updated
    private void runFullyConnected(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run backpropagation on fully connected KBANN's network with edges only between adjacent layers  use 'fullyConnected   #ofRepeats  datasetFile weightLearningSettingsFile  ruleFile   '");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }

        String algName = "fullyConnected";
        File ruleFile = new File(arg[4]);
        File settingFile = new File(arg[5]);
        KBANNSettings kbannSettings = new KBANNSettings(randomGenerator, 1.0, true, true);

        List<Pair<Integer, ActivationFunction>> specificRules = new ArrayList<>();

        Initable<KBANN> initialize = () -> KBANN.createTest(ruleFile, dataset, specificRules, kbannSettings, SOFTMAX);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        Learnable learn = (kbann, learningDataset) -> ((KBANN) kbann).learn(learningDataset, finalWls);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls, fakeNumberTheoryComplexityTodo);
    }
    */
}
