package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.weka.WekaJRip;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.AntecedentsTrimmer;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.Relabeling;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.RuleAccuracy;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.RuleTrimmer;
import weka.core.Instances;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

/**
 * Created by EL on 6.9.2016.
 */
public class NeuralSymbolicCycle {


    public static void main(String arg[]) throws FileNotFoundException {
        System.out.println("called with parameters");
        Arrays.stream(arg).forEach(e -> System.out.println(e + "\t"));
        System.out.println();

        if (arg.length < 4) {
            //TODO revise
            System.out.println("Not enough arguments. Right setting in form 'algorithmName numberOfRuns datasetFile weightLearningSettingFile [...]'. Possible algorithms KBANN, CasCor, DNC, SLSF, TopGen, REGENT; write as first argument to see more. TODO this MSG");
            System.exit(0);
        }

        double simga = 1d;
        double mu = 0.0d;
        int seed = 13;
        int numberOfRepeats = 0;
        try {
            numberOfRepeats = Integer.valueOf(arg[1]);
        } catch (Exception ex) {
            System.out.println("The second argument (number of repeats) must be integer.");
            System.out.println("Argument input instead '" + arg[1] + "'.");
            System.exit(0);
        }
        File datasetFile = new File(arg[2]);
        if (!datasetFile.exists()) {
            System.out.println("The third argument (datasetFile) does not exist.");
            System.out.println("Argument input instead '" + arg[2] + "'.");
            System.exit(0);
        }
        File wlsFile = new File(arg[3]);
        if (!wlsFile.exists()) {
            System.out.println("The fourth argument (weightLearningSettingFile) does not exist.");
            System.out.println("Argument input instead '" + arg[3] + "'.");
            System.exit(0);
        }
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);
        if (!"SLSF".equals(arg[0])) {
            wls = WeightLearningSetting.turnOffRegularization(wls);
        }


        // TODO NACITANI NORMALIZACE ZAJISTIT :) & zkontrolovat
        boolean normalize = true;
        RandomGeneratorImpl randomGenerator = new RandomGeneratorImpl(simga, mu, seed);

        Pair<Dataset, Instances> datasetsPair = DatasetImpl.parseAndGetDatasets(datasetFile, normalize);

        Dataset nesislDataset = datasetsPair.getLeft();
        Instances wekaDataset = datasetsPair.getRight();

        RuleSet ruleSet = WekaJRip.create(wekaDataset).getRuleSet();
        ruleSet = RuleTrimmer.create(ruleSet).getRuleSet();
        ruleSet = RuleTrimmer.create(ruleSet).getRuleSet();
        String theory = ruleSet.getTheory();
        System.out.println(theory);

        RuleAccuracy acc = RuleAccuracy.create(ruleSet);
        System.out.println(acc.computeAccuracy(nesislDataset.getRawData(), nesislDataset));
        System.out.println(acc.numberOfConsistentClassifications(nesislDataset.getRawData(), nesislDataset));

        Dataset relabeled = Relabeling.create(nesislDataset, ruleSet).getDataset();
        RuleAccuracy relabeledAcc = RuleAccuracy.create(ruleSet);
        System.out.println(relabeledAcc.computeAccuracy(relabeled.getRawData(), relabeled));
        System.out.println(relabeledAcc.numberOfConsistentClassifications(relabeled.getRawData(), relabeled));

        Main.fakeNumberTheoryComplexityTodo = ruleSet.getComplexity();
        File file = Tools.storeToTemporaryFile(theory);
        Main main = new Main();
        String[] rearanged = {arg[0], // KBANN
                arg[1], // #ofRepeats
                arg[2], // datasetFile
                arg[3], // weightLearningSettingsFile
                file.getAbsolutePath(),//arg[4], // ruleFile
                arg[5], // KBANNsetting
                //arg[6], // [ruleSpecificFile]
                };
        main.runKBANN(rearanged, numberOfRepeats, nesislDataset, wls, randomGenerator);

        //RuleSet a1 = AntecedentsTrimmer.create(ruleSet).getRuleSet();
        //RuleSet r1 = RuleTrimmer.create(ruleSet).getRuleSet();

        // todo
        // PARSERT NA FOLDy regent/topgen a mozna jeste dalsi veci

        // under development

        // run JRip
        // prelabelovat data podle JRipu

        //Main main = new Main();
        /*switch (arg[0]) {
            case "KBANN":
                main.runKBANN(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "backprop":
                main.runBackprop(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "fullyConnected":
                main.runFullyConnected(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "CasCor":
                main.runCasCor(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "DNC":
                main.runDNC(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "SLSF":
                main.runSLSF(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "TopGen":
                main.runTopGen(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "REGENT":
                main.runREGENT(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            default:
                System.out.println("Unknown algorithm '" + arg[0] + "'.");
                break;
        }*/
    }
}
