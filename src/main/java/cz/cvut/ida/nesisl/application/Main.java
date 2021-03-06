package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.modules.extraction.RuleExtractor;
import main.java.cz.cvut.ida.nesisl.modules.extraction.jripExtractor.SamplePerturbator;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiCrossvalidation;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiRepresentationDataset;
import main.java.cz.cvut.ida.nesisl.modules.experiments.generator.PropositionalFormulaeGenerator;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.weka.RuleMiner;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import weka.core.Instances;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Currently, this version takes as arguments paths to data for background knowledge learner and the rest separately.
 * <p>
 * Created by EL on 9.2.2016.
 */
public class Main {

    // numeric attributes are not possible to predict in this version

    // general setting; RULE_EXTRACTOR is needed to be true if you want to run the whole NSL cycle
    public static RuleExtractor RULE_EXTRACTOR = RuleExtractor.TREPAN;
    public static boolean RUN_RULE_EXTRACTION_CORRECTION = false;
    public static double percentualAccuracyOfOriginalDataset = 1.0; // 1.0

    // settings for random generator
    public static final Double SIGMA = 1d; // normally 1


    public static final Double MU = 0.0d;
    private static Integer SEED = 13;
    public static final String CYCLE_TOKEN = "-cycle";


    public static final String RULE_EXTRACTION_CHECKER = "-trepanCheck";
    public static final String SEED_TOKEN = "-seed";
    public static final String PERCENTUAL_TRIM_TOKEN = "-trimAcc";

    public static final String SET_EXTRACTOR_TOKEN = "-extractor";
    public static final String JRIP_EXTRACTOR_RESAMPLING_SIZE_TOKEN = "-JRipResampling";
    public static final String JRIP_EXTRACTOR_PERTURBATION_PSI_TOKEN = "-JRipPerturbation";

    /**
     * params
     * either:
     *      -generator      // to run dataset generation, or
     *      -trepanCheck    // to just check TREPAN behavior, or
     *          //sequence of
     *          [-extractor (JRIP|TREPAN)]
     *          [-JRipPerturbation doublePsi]
     *          [-JRipResampling doublePercentageOfAmountOfTrainSet]
     *          [-seed randomSeedLong]
     *          [-trimAcc double]
     *          [-cycle #ofCyclesInteger]   // multiple cycles if is present, otherwise single cycle
     *          (KBANN|TopGen|REGENT|PYRAMID|CasCor|DNC)
     *          #ofRuns     // # of crossvalidation's folds
     *          datasetFile
     *          backgroundKnowledgeLearnerData
     *          weightLearningSettingFile
     *          structureLearningSettingFile    // if needed
     *          ...     // TBD
     *
     *
     * @param arg
     * @throws FileNotFoundException
     */
    public static void main(String arg[]) throws FileNotFoundException {
        writeInfo(arg);

        if (arg.length > 0 && "-generator".equals(arg[0])) {
            PropositionalFormulaeGenerator.main(arg);
            System.exit(0);
        }

        if (arg.length < 4) {
            System.out.println("Not enough arguments. Right setting in form 'algorithmName numberOfRuns datasetFile weightLearningSettingFile [...]' or '" + CYCLE_TOKEN + " numberOfCycleRuns algorithmName numberOfFolds datasetFile weightLearningSettingFile [...]'. Possible algorithms KBANN, CasCor, DNC, SLSF, TopGen, REGENT; write as first argument to see more. TODO this MSG...");
            System.exit(0);
        }

        if (RULE_EXTRACTION_CHECKER.equals(arg[0])) {
            RUN_RULE_EXTRACTION_CORRECTION = true;
            arg = eraseFirstFromArgs(arg);
        }

        if (SET_EXTRACTOR_TOKEN.equals(arg[0])) {
            RULE_EXTRACTOR = RuleExtractor.create(arg[1]);
            arg = eraseTwoFirstFromArgs(arg);
        }

        if (JRIP_EXTRACTOR_PERTURBATION_PSI_TOKEN.equals(arg[0])) {
            SamplePerturbator.psiOfPerturbationHappening = Tools.parseDouble(arg[1], "The argument (JRIP_EXTRACTOR_PERTURBATION_PSI_TOKEN) must be double.\nArgument input instead '" + arg[1] + "'.");
            arg = eraseTwoFirstFromArgs(arg);
        }

        if (JRIP_EXTRACTOR_RESAMPLING_SIZE_TOKEN.equals(arg[0])) {
            SamplePerturbator.percentageOfResampling = Tools.parseDouble(arg[1], "The argument (JRIP_EXTRACTOR_RESAMPLING_SIZE_TOKEN) must be double.\nArgument input instead '" + arg[1] + "'.");
            arg = eraseTwoFirstFromArgs(arg);
        }

        if (SEED_TOKEN.equals(arg[0])) {
            SEED = Tools.parseInt(arg[1], "The second argument (seed) must be integer.\nArgument input instead '" + arg[1] + "'.");
            arg = eraseTwoFirstFromArgs(arg);
        }

        if (PERCENTUAL_TRIM_TOKEN.equals(arg[0])) {
            percentualAccuracyOfOriginalDataset = Tools.parseDouble(arg[1], "The second argument (percentageOfResampling trim) must be double.\nArgument input instead '" + arg[1] + "'.");
            arg = eraseTwoFirstFromArgs(arg);
        }

        RandomGeneratorImpl randomGenerator = new RandomGeneratorImpl(SIGMA, MU, SEED);

        int numberOfSingleCycles = 0;
        boolean multipleCycles = CYCLE_TOKEN.equals(arg[0]);
        if (multipleCycles) {
            numberOfSingleCycles = Tools.parseInt(arg[1], "The second argument (number of single cycle repeats) must be integer.\nArgument input instead '" + arg[1] + "'.");
            arg = eraseTwoFirstFromArgs(arg);
        }

        int numberOfRepeats = Tools.parseInt(arg[1], "The second/fourth argument (number of repeats) must be integer.\nArgument input instead '" + arg[1] + "'.");
        File datasetFile = Tools.retrieveFile(arg[2], "The third/fifth argument (datasetFile) does not exist.\nArgument input instead '" + arg[2] + "'.");
        File backgroundKnowledgeDatasetFile = Tools.retrieveFile(arg[3], "The fourth/sixth argument (background knowledge datasetFile) does not exist.\nArgument input instead '" + arg[3] + "'.");
        File wlsFile = Tools.retrieveFile(arg[4], "The fifth/seventh argument (weightLearningSettingFile) does not exist.\nArgument input instead '" + arg[4] + "'.");

        // TODO: add parameter for reading normalization
        boolean normalize = true; // not needed since only nominal input are used
        MultiRepresentationDataset multiRepre = DatasetImpl.parseMultiRepresentationDataset(datasetFile, normalize);
        MultiRepresentationDataset backgroundMultiRepre = DatasetImpl.parseMultiRepresentationDataset(backgroundKnowledgeDatasetFile, normalize);
        WeightLearningSetting wls = parseAndAdjustWLS(arg[0], WeightLearningSetting.parse(wlsFile, randomGenerator.getRandom()), multiRepre);

        System.out.println("" + backgroundMultiRepre.getNesislDataset().getClassAttribute());

        /**/// this is right but testing older version
        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(backgroundMultiRepre, randomGenerator, 1);
        Instances backgroundKnowledgeTrainData = crossval.getTrainWekaDataset(backgroundMultiRepre.getNesislDataset());
        RuleSet ruleSet = RuleMiner.mineAndTrimmeRule(backgroundMultiRepre.getNesislDataset(), backgroundKnowledgeTrainData);
        /**/
        // minizmena

        /*// older version - train data for JRip are taken from train folds
        MultiCrossvalidation crossval = MultiCrossvalidation.createStratified(multiRepre, randomGenerator, 1);
        Instances backgroundKnowledgeTrainData = crossval.getTrainWekaDataset(multiRepre.getNesislDataset());
        RuleSet ruleSet = RuleMiner.mineAndTrimmeRule(multiRepre.getNesislDataset(), backgroundKnowledgeTrainData);
        */

        File ruleFile = Tools.storeToTemporaryFile(ruleSet.getTheory());
        /* end of rule mining and trimming */

        System.out.println("theory\n" + ruleSet.getTheory());

        System.out.println("!!!!!!!!!! ZEPTAT SE FRANTY JAK JE TO S ORDERED U SEKVENCI DNA");

        if(RUN_RULE_EXTRACTION_CORRECTION) {
            crossval = MultiCrossvalidation.createStratified(multiRepre, randomGenerator, 1);
            backgroundKnowledgeTrainData = crossval.getTestWekaDataset(multiRepre.getNesislDataset());
            ruleSet = RuleMiner.mineAndTrimmeRule(multiRepre.getNesislDataset(), backgroundKnowledgeTrainData);
            ruleFile = Tools.storeToTemporaryFile(ruleSet.getTheory());
            runRuleExtractionCorrection(arg,  wls, multiRepre, ruleSet, ruleFile);
        }else if (multipleCycles) {
            runMultipleCycles(arg, randomGenerator, numberOfSingleCycles, numberOfRepeats, wls, multiRepre, ruleSet, ruleFile);
        } else {
            runOneTransit(arg, randomGenerator, numberOfRepeats, wls, multiRepre, ruleSet, ruleFile);
        }
    }

    private static void writeInfo(String[] arg) {
        Arrays.stream(arg).forEach(e -> System.out.println(e + "\t"));
        System.out.println();
        System.out.println("inner parameters");
        System.out.println("sigma:\t" + SIGMA);
        System.out.println("mu:\t" + MU);
        System.out.println("seed:\t" + SEED);
        System.out.println("percentualAccuracyOfOriginalDataset:\t" + percentualAccuracyOfOriginalDataset);
        System.out.println("RULE_EXTRACTOR:\t" + RULE_EXTRACTOR);
        System.out.println("end of info");
        System.out.println();
    }

    private static String[] eraseTwoFirstFromArgs(String[] arg) {
        String[] swap = new String[arg.length - 2];
        final String[] finalArg = arg;
        IntStream.rangeClosed(2, arg.length - 1).forEach(idx -> swap[idx - 2] = finalArg[idx]);
        return swap;
    }

    private static String[] eraseFirstFromArgs(String[] arg) {
        String[] swap = new String[arg.length - 1];
        final String[] finalArg = arg;
        IntStream.rangeClosed(1, arg.length - 1).forEach(idx -> swap[idx - 1] = finalArg[idx]);
        return swap;
    }

    private static WeightLearningSetting parseAndAdjustWLS(String anObject, WeightLearningSetting parse, MultiRepresentationDataset multiRepre) {
        // turning off regularization (appendix)
        WeightLearningSetting wls = parse;
        if (!"SLSF".equals(anObject)) {
            wls = WeightLearningSetting.turnOffRegularization(wls);
        }

        if (multiRepre.getNesislDataset().getOutputFactOrder().size() < 2 && wls.isLearningWithCrossEntropy()) {
            wls = WeightLearningSetting.turnOffCrossentropyLearning(wls);
        }
        return wls;
    }

    private static void runOneTransit(String[] arg, RandomGeneratorImpl randomGenerator, int numberOfRepeats, WeightLearningSetting wls, MultiRepresentationDataset multiRepre, RuleSet ruleSet, File ruleFile) throws FileNotFoundException {
        System.out.println("running one iteration only");
        SingleCycle single = new SingleCycle(percentualAccuracyOfOriginalDataset);
        switch (arg[0]) {
            case "CasCor":
                single.runCasCor(arg, numberOfRepeats, multiRepre, wls, randomGenerator);
                break;
            case "DNC":
                single.runDNC(arg, numberOfRepeats, multiRepre, wls, randomGenerator);
                break;
            case "KBANN":
                System.out.println("catch");
                System.out.println(ruleSet);
                System.out.println(ruleSet.getTheory());
                single.runKBANN(arg, numberOfRepeats, multiRepre, wls, randomGenerator, ruleSet, ruleFile);
                break;
            case "TopGen":
                single.runTopGen(arg, numberOfRepeats, multiRepre, wls, randomGenerator, ruleSet, ruleFile);
                break;
            case "REGENT":
                single.runREGENT(arg, numberOfRepeats, multiRepre, wls, randomGenerator, ruleSet, ruleFile);
                break;
            case "PYRAMID":
                single.runPYRAMID(arg, numberOfRepeats, multiRepre, wls, randomGenerator, ruleSet, ruleFile);
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

    private static void runMultipleCycles(String[] arg, RandomGeneratorImpl randomGenerator, int numberOfSingleCycles, int numberOfRepeats, WeightLearningSetting wls, MultiRepresentationDataset multiRepre, RuleSet ruleSet, File ruleFile) throws FileNotFoundException {
        System.out.println("running multiple iterations of the cycle");
        MultipleCycles cycles = new MultipleCycles(numberOfSingleCycles);
        switch (arg[0]) {
            case "KBANN":
                cycles.runKBANN(arg, numberOfRepeats, multiRepre, wls, randomGenerator, ruleSet, ruleFile);
                break;
            case "TopGen":
                cycles.runTopGen(arg, numberOfRepeats, multiRepre, wls, randomGenerator, ruleSet, ruleFile);
                break;
            case "REGENT":
                cycles.runREGENT(arg, numberOfRepeats, multiRepre, wls, randomGenerator, ruleSet, ruleFile);
                break;
            default:
                System.out.println("Unknown algorithm for multiple neural-symbolic learning cycles '" + arg[0] + "'.");
                break;
        }
    }

    private static void runRuleExtractionCorrection(String[] arg,WeightLearningSetting wls, MultiRepresentationDataset multiRepre, RuleSet ruleSet, File ruleFile) {
        System.out.println("running rule extraction correction (now, only KBANN-TREPAN pair is possible)");
        RuleExtractionCheck cycles = new RuleExtractionCheck();
        switch (arg[0]) {
            case "KBANN":
                cycles.runKBANNcheckedByTREPAN(arg, multiRepre, wls, ruleSet, ruleFile);
                break;
            default:
                System.out.println("Unknown algorithm for multiple neural-symbolic learning cycles '" + arg[0] + "'.");
                break;
        }
    }

}
