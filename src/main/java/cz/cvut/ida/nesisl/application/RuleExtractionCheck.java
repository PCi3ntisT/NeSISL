package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiRepresentationDataset;
import main.java.cz.cvut.ida.nesisl.modules.export.neuralNetwork.tex.TikzExporter;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.trepan.MofNTreeFactory;
import main.java.cz.cvut.ida.nesisl.modules.trepan.Trepan;
import main.java.cz.cvut.ida.nesisl.modules.trepan.TrepanResults;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTree;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTreeReader;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;
import main.java.cz.cvut.ida.nesisl.modules.weka.tools.RuleAccuracy;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by EL on 20.1.2017.
 */
public class RuleExtractionCheck {
    /*create KBANN
    without pertrubation, run
    TREPAN on
    that and
    check accuracy*/


    // another awful copy & paste of several others, but it is just for prototyping
    public void runKBANNcheckedByTREPAN(String[] arg, MultiRepresentationDataset dataset, WeightLearningSetting wls, RuleSet ruleSet, File ruleFile) {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run KBANN use 'KBANN   #ofRepeats  datasetFile backgroundData weightLearningSettingsFile  KBANNsetting    [ruleSpecificFile]'");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        RandomGeneratorImpl dummyGenerator = new RandomGeneratorImpl(0, 0, 0);
        String algName = "KBANN";
        File settingFile = new File(arg[5]);
        KBANNSettings kbannSettings = KBANNSettings.create(dummyGenerator, settingFile);

        List<Pair<Integer, ActivationFunction>> specificRules = new ArrayList<>();

        System.out.println("ruleFile");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(ruleFile.getAbsoluteFile()));
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("end of file");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("");

        KBANN kbann = KBANN.create(ruleFile, dataset.getNesislDataset(), specificRules, kbannSettings, wls.isLearningWithCrossEntropy());
        //kbann.learn(dataset.getNesislDataset(),wls);

        double trainInitialBackgrounKnowledgeAccuracy = RuleAccuracy.create(ruleSet).computeTestAccuracy(dataset.getNesislDataset());
        double testInitialBackgrounKnowledgeAccuracy = RuleAccuracy.create(ruleSet).computeTrainAccuracy(dataset.getNesislDataset());

        String myAddress = dataset.getDatasetFile().getAbsoluteFile().getParent() + File.separator +
                algName + File.separator +
                Tools.retrieveParentFolderName(settingFile) + File.separator +
                Tools.retrieveParentFolderName(wls.getFile()) + File.separator +
                algName + "_extractionCheck";
        TrepanResults trepan = Trepan.create(kbann.getNeuralNetwork(), dataset.getNesislDataset(), algName, 0, myAddress).run();

        kbann.getNeuralNetwork().setClassifierStateful(new ThresholdClassificator(0.5));
        long correct = dataset.getNesislDataset().getTestData(kbann.getNeuralNetwork())
                .stream()
                .filter(sample -> {
                    List<Double> computedOutputs = kbann.getNeuralNetwork().evaluate(sample.getInput());
                    return kbann.getNeuralNetwork().getClassifier().isCorrectlyClassified(sample.getOutput(), computedOutputs);
                }).count();

        double myNeuralAcc = correct / (1.0 * dataset.getNesislDataset().getTestData(kbann.getNeuralNetwork()).size());
        System.out.println("my test correctness:\t" + myNeuralAcc);

        double testNeuralAccuracy = trepan.getNetworkTestAccuracy();
        double trainNeuralAccuracy = trepan.getNetworkTrainAccuracy();

        double testTrepanAccuracy = trepan.getTrepanTestAccuracy();
        double trainTrepanAccuracy = trepan.getTrepanTrainAccuracy();

        System.out.println("initial");
        System.out.println("\t" + trainInitialBackgrounKnowledgeAccuracy);
        System.out.println("\t" + testInitialBackgrounKnowledgeAccuracy);

        System.out.println("neural");
        System.out.println("\t" + trainNeuralAccuracy);
        System.out.println("\t" + testNeuralAccuracy);

        System.out.println("extracted");
        System.out.println("\t" + trainTrepanAccuracy);
        System.out.println("\t" + testTrepanAccuracy);

        System.out.println("TODO vyblejt to do souboru");

        //System.out.println("extracted theory");
        //DotTree tree = DotTreeReader.getDefault().create(trepan.getTreeFile());
        //ruleFile = Tools.storeToTemporaryFile(MofNTreeFactory.getDefault().getTheory(tree, kbann.getNeuralNetwork()));
        //System.out.println(MofNTreeFactory.getDefault().getTheory(tree, kbann.getNeuralNetwork()));

        String tex = TikzExporter.exportToString(kbann.getNeuralNetwork());
        //System.out.println(tex);

        //currentResult.addExperiment(learnedNetwork, start, end, nesislDataset, trepan, ruleSetDescriptionLength, trainRuleAcc, testRuleAcc);
        //ExperimentResult.storeResults(algName, dataset.getDatasetFile(), settingFile, wls,initialBackgrounKnowledgeAccuracy, neuralAccuarcy,extractedAccuracy);
    }

}
