package main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.application.Main;
import main.java.cz.cvut.ida.nesisl.modules.extraction.RuleExtractor;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.export.neuralNetwork.tex.TikzExporter;
import main.java.cz.cvut.ida.nesisl.modules.export.texFile.TexFile;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.extraction.TrepanResults;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static main.java.cz.cvut.ida.nesisl.modules.extraction.RuleExtractor.JRIP;

/**
 * Created by EL on 15.3.2016.
 */
public class ExperimentResult {
    private final File datasetFile;
    private final int numberOfRepeats;
    private final String learningAlg;
    private final String myAdress;

    private NeuralNetwork initNetwork;
    private NeuralNetwork finalNetwork;
    private Long runningTime;
    private Double averageSquaredTestError;
    private Double RocAuc;
    private Double threshold;
    private Double accuracy;
    private long numberOfHiddenNodes;
    private double averageSquaredTotalTrainError;
    private double trainAccuracy;
    private Long trepanNumberOfInnerNodes;
    private Double trepanTestFidelity;
    private Double trepanTrainFidelity;
    private Double trepanTrainAcc;
    private Double trepanTestAcc;
    private Long mOfNDecisionTreeDescriptionLength;
    private Long ruleSetDescriptionLength;
    private double testRuleSetAccuracy;
    private double trainRuleSetAccuracy;
    private File mofNDecisionTreeFile;


    public ExperimentResult(int numberOfRepeats, String learningAlg, File datasetFile, File structureLearningSetting, WeightLearningSetting wls) {
        this.datasetFile = datasetFile;
        this.numberOfRepeats = numberOfRepeats;
        this.learningAlg = learningAlg;
        this.myAdress = datasetFile.getAbsoluteFile().getParent() + File.separator +
                learningAlg + File.separator +
                Tools.retrieveParentFolderName(structureLearningSetting) + File.separator +
                Tools.retrieveParentFolderName(wls.getFile()) + File.separator +
                learningAlg + "_" + numberOfRepeats;
    }

    public ExperimentResult(int numberOfRepeats, String learningAlg, File datasetFile, File structureLearningSetting, WeightLearningSetting wls, int cycleNumber) {
        this.datasetFile = datasetFile;
        this.numberOfRepeats = numberOfRepeats;
        this.learningAlg = learningAlg;
        this.myAdress = datasetFile.getAbsoluteFile().getParent() + File.separator +
                learningAlg + File.separator +
                Tools.retrieveParentFolderName(structureLearningSetting) + File.separator +
                Tools.retrieveParentFolderName(wls.getFile()) + File.separator +
                cycleNumber + File.separator +
                learningAlg + "_" + numberOfRepeats;
    }

    public File getDatasetFile() {
        return datasetFile;
    }

    public int getNumberOfRepeats() {
        return numberOfRepeats;
    }

    public String getLearningAlg() {
        return learningAlg;
    }

    public NeuralNetwork getInitNetwork() {
        return initNetwork;
    }

    public void setInitNetwork(NeuralNetwork initNetwork) {
        this.initNetwork = initNetwork;
    }

    private void networkToLatexAndBuild(NeuralNetwork network, String name) {
        File file = new File(myAdress);
        if (!file.exists()) {
            file.mkdirs();
        }

        TexFile tex = TikzExporter.export(network);
        File output = tex.saveAs(myAdress + File.separator + name + ".tex");
        //TexFile.build(output);
    }

    public NeuralNetwork getFinalNetwork() {
        return finalNetwork;
    }

    public void setFinalNetwork(NeuralNetwork finalNetwork) {
        this.finalNetwork = finalNetwork;
    }

    public void setRunningTime(long time) {
        this.runningTime = time;
    }

    public Long getRunningTime() {
        return runningTime;
    }

    public void setAverageSquaredTotalTestError(double averageSquaredTestError) {
        this.averageSquaredTestError = averageSquaredTestError;
    }

    public double getAverageSquaredTestError() {
        return averageSquaredTestError;
    }

    public void setRocAuc(Double rocAuc) {
        this.RocAuc = rocAuc;
    }

    public Double getRocAuc() {
        return RocAuc;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setNumberOfHiddenNodes(long numberOfHiddenNodes) {
        this.numberOfHiddenNodes = numberOfHiddenNodes;
    }

    public long getNumberOfHiddenNodes() {
        return numberOfHiddenNodes;
    }

    public void addExperiment(NeuralNetwork network, long start, long end, Dataset dataset, long ruleSetComplexity, double trainRuleSetAccuracy, double testRuleSetAccuracy) {
        this.setRunningTime(end - start);
        this.setFinalNetwork(network.getCopy());
        Map<Sample, Results> evaluation = Tools.evaluateOnTestAllAndGetResults(dataset, network);
        this.setAverageSquaredTotalTestError(Tools.computeTotalError(evaluation));

        Map<Sample, Results> train = Tools.evaluateOnTrainDataAllAndGetResults(dataset, network);
        this.setAverageSquaredTotalTrainError(Tools.computeTotalError(train));
        this.setTrainAccuracy(AccuracyCalculation.create(network, train).getAccuracy());

        //this.setRocAuc(RocAucCalculation.createTest(network, evaluation).computeAUC());
        //this.setThreshold(network.getClassifier().getThreshold());

        this.setAccuracy(AccuracyCalculation.create(network, evaluation).getAccuracy());

        this.setNumberOfHiddenNodes(network.getNumberOfHiddenNodes());
        this.setRuleSetDescriptionLength(ruleSetComplexity);
        this.setTrainRuleSetAccuracy(trainRuleSetAccuracy);
        this.setTestRuleSetAccuracy(testRuleSetAccuracy);
    }

    public void exportSavedNetworksToTex() {
        networkToLatexAndBuild(initNetwork, "origin");
        networkToLatexAndBuild(finalNetwork, "final");
    }


    public static void storeCyclesResult(List<List<ExperimentResult>> results, String learningAlg, File datasetFile, File structureLearningSetting, WeightLearningSetting wls) {
        long min = results.stream()
                .mapToLong(list -> list.size())
                .min().orElse(0);

        for (int elementIdx = 0; elementIdx < min; elementIdx++) {
            final int finalElementIdx = elementIdx;
            List<ExperimentResult> resultsFromGroup = results.stream()
                    .map(list -> list.get(finalElementIdx))
                    .collect(Collectors.toCollection(ArrayList::new));
            storeResults(resultsFromGroup, learningAlg, datasetFile, structureLearningSetting, wls, "results" + elementIdx);
        }
    }

    public static void storeResults(List<ExperimentResult> results, String learningAlg, File datasetFile, File structureLearningSetting, WeightLearningSetting wls) {
        storeResults(results,learningAlg,datasetFile,structureLearningSetting,wls,"results");
    }

    public static void storeResults(List<ExperimentResult> results, String learningAlg, File datasetFile, File structureLearningSetting, WeightLearningSetting wls,String resultsFileName) {
        results.forEach(ExperimentResult::exportSavedNetworksToTex);

        String experimentsFile = datasetFile.getAbsoluteFile().getParent() + File.separator +
                learningAlg + File.separator +
                Tools.retrieveParentFolderName(structureLearningSetting) + File.separator +
                Tools.retrieveParentFolderName(wls.getFile()) + File.separator +
                resultsFileName +".txt";

        File expFile = new File(experimentsFile);
        if (!expFile.getParentFile().exists()) {
            expFile.getParentFile().mkdirs();
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(expFile);
            fillExperimentsContent(writer, learningAlg, results);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }

    private static void fillExperimentsContent(PrintWriter writer, String learningAlg, List<ExperimentResult> results) {
        writeName(learningAlg, writer);

        List<Pair<String, StoreableResults>> process = new ArrayList<>();
        process.add(new Pair<>("error", () -> results.stream().mapToDouble(e -> e.getAverageSquaredTestError())));
        process.add(new Pair<>("trainError", () -> results.stream().mapToDouble(e -> e.getAverageSquaredTotalTrainError())));
        process.add(new Pair<>("accuracy", () -> results.stream().mapToDouble(e -> e.getAccuracy())));
        process.add(new Pair<>("trainAccuracy", () -> results.stream().mapToDouble(e -> e.getTrainAccuracy())));
        //process.add(new Pair<>("RocAuc", () -> results.stream().mapToDouble(e -> e.getRocAuc())));
        process.add(new Pair<>("time", () -> results.stream().mapToDouble(e -> e.getRunningTime())));
        //process.add(new Pair<>("threshold", () -> results.stream().mapToDouble(e -> e.getThreshold())));

        process.add(new Pair<>("ruleSetDescriptionLength", () -> results.stream().mapToDouble(e -> e.getRuleSetDescriptionLength())));
        process.add(new Pair<>("trainRuleSetAccuracy", () -> results.stream().mapToDouble(e -> e.getTrainRuleSetAccuracy())));
        process.add(new Pair<>("testRuleSetAccuracy", () -> results.stream().mapToDouble(e -> e.getTestRuleSetAccuracy())));

        if (!RuleExtractor.NONE.equals(Main.RULE_EXTRACTOR)) {
            process.add(new Pair<>("trepanTrainAcc", () -> results.stream().mapToDouble(e -> e.getTrepanTrainAcc())));
            process.add(new Pair<>("trepanTestAcc", () -> results.stream().mapToDouble(e -> e.getTrepanTestAcc())));
            process.add(new Pair<>("trepanTrainFidelity", () -> results.stream().mapToDouble(e -> e.getTrepanTrainFidelity())));
            process.add(new Pair<>("trepanTestFidelity", () -> results.stream().mapToDouble(e -> e.getTrepanTestFidelity())));
            process.add(new Pair<>("trepanNumberOfInnerNodes", () -> results.stream().mapToDouble(e -> e.getTrepanNumberOfInnerNodes())));
            process.add(new Pair<>("mOfNDecisionTreeDescriptionLength", () -> results.stream().mapToDouble(e -> e.getMofNDecisionTreeDescriptionLength())));
        }

        process.forEach(pair -> appendContent(pair.getLeft(), pair.getRight(), writer));
    }

    private static void writeName(String algName, PrintWriter writer) {
        appendContent(algName, () -> DoubleStream.empty(), writer);
        String extractionAlgName = "";
        switch (Main.RULE_EXTRACTOR){
            case JRIP:
                extractionAlgName = "JRip";
                break;
            case TREPAN:
                extractionAlgName = "TREPAN";
                break;
            case NONE:
                extractionAlgName = "NONE";
                break;
            default:
                extractionAlgName = "unknown";
        }
        appendContent(extractionAlgName, () -> DoubleStream.empty(), writer);
    }

    private static void appendContent(String sectionHeader, StoreableResults storeable, PrintWriter writer) {
        writer.println(sectionHeader);
        if (0 == storeable.getValues().count()) {
            return;
        }

        DoubleSummaryStatistics statistics = storeable.getValues().summaryStatistics();
        Double median = Tools.medianDouble(storeable.getValues().boxed().collect(Collectors.toCollection(ArrayList::new)));

        writer.println(statistics.getMin() + "\t" + statistics.getAverage() + "\t" + statistics.getMax() + "\t" + median);
        storeable.getValues().forEach(value -> writer.print(value + "\t"));
        writer.print("\n");
    }

    public void setAverageSquaredTotalTrainError(double averageSquaredTotalTrainError) {
        this.averageSquaredTotalTrainError = averageSquaredTotalTrainError;
    }

    public double getAverageSquaredTotalTrainError() {
        return averageSquaredTotalTrainError;
    }

    public void setTrainAccuracy(double trainAccuracy) {
        this.trainAccuracy = trainAccuracy;
    }

    public double getTrainAccuracy() {
        return trainAccuracy;
    }

    /*private static void writeTime(List<ExperimentResult> results, PrintWriter writer) {
        DoubleSummaryStatistics statistics = results.stream().mapToDouble(ExperimentResult::getRunningTime).summaryStatistics();
        Long median = Tools.medianLong(results.stream().map(e -> e.getRunningTime()).collect(Collectors.toCollection(ArrayList::new)));

        writer.println("times");
        writer.println(statistics.getMin() + "\t" + statistics.getAverage() + "\t" + statistics.getMax() + "\t" + median);
        results.forEach(r -> writer.print(r.getRunningTime() + "\t"));
        writer.print("\n");
    }

    private static void writeError(List<ExperimentResult> results, PrintWriter writer) {
        DoubleSummaryStatistics statistics = results.stream().mapToDouble(ExperimentResult::getAverageSquaredTestError).summaryStatistics();
        Double median = Tools.medianDouble(results.stream().map(e -> e.getAverageSquaredTestError()).collect(Collectors.toCollection(ArrayList::new)));

        writer.println("errors");
        writer.println(statistics.getMin() + "\t" + statistics.getAverage() + "\t" + statistics.getMax() + "\t" + median);
        results.forEach(r -> writer.print(r.getAverageSquaredTestError() + "\t"));
        writer.print("\n");
    }*/

    public String getMyAdress() {
        return myAdress;
    }

    public void addExperiment(NeuralNetwork learnedNetwork, long start, long end, Dataset dataset, TrepanResults trepan, long ruleSetDescriptionLength, double trainRuleSetAccuracy, double testRuleSetAccuracy) {
        //addExperiment(learnedNetwork, start, end, dataset);
        addExperiment(learnedNetwork, start, end, dataset, ruleSetDescriptionLength, trainRuleSetAccuracy, testRuleSetAccuracy);

        this.setTrainAccuracy(trepan.getNetworkTrainAccuracy());
        this.setAccuracy(trepan.getNetworkTestAccuracy());

        this.setTrepanTrainAcc(trepan.getTrepanTrainAccuracy());
        this.setTrepanTestAcc(trepan.getTrepanTestAccuracy());
        this.setTrepanTrainFidelity(trepan.getTrainFidelity());
        this.setTrepanTestFidelity(trepan.getTestFidelity());

        this.setTrepanNumberOfInnerNodes(trepan.getNumberOfInnerNodes());
        this.setMofNDecisionTreeDescriptionLength(trepan.getMofNDecisionTreeDescriptionLength());
        this.setMofNDecisionTreeFile(trepan.getTreeFile());
    }

    public void setTrepanNumberOfInnerNodes(Long trepanNumberOfInnerNodes) {
        this.trepanNumberOfInnerNodes = trepanNumberOfInnerNodes;
    }

    public Long getTrepanNumberOfInnerNodes() {
        return trepanNumberOfInnerNodes;
    }

    public void setTrepanTestFidelity(Double trepanTestFidelity) {
        this.trepanTestFidelity = trepanTestFidelity;
    }

    public Double getTrepanTestFidelity() {
        return trepanTestFidelity;
    }

    public void setTrepanTrainFidelity(Double trepanTrainFidelity) {
        this.trepanTrainFidelity = trepanTrainFidelity;
    }

    public Double getTrepanTrainFidelity() {
        return trepanTrainFidelity;
    }

    public void setTrepanTrainAcc(Double trepanTrainAcc) {
        this.trepanTrainAcc = trepanTrainAcc;
    }

    public Double getTrepanTrainAcc() {
        return trepanTrainAcc;
    }

    public void setTrepanTestAcc(Double trepanTestAcc) {
        this.trepanTestAcc = trepanTestAcc;
    }

    public Double getTrepanTestAcc() {
        return trepanTestAcc;
    }

    public void setMofNDecisionTreeDescriptionLength(Long mOfNDecisionTreeDescriptionLength) {
        this.mOfNDecisionTreeDescriptionLength = mOfNDecisionTreeDescriptionLength;
    }

    public Long getMofNDecisionTreeDescriptionLength() {
        return mOfNDecisionTreeDescriptionLength;
    }

    public Long getRuleSetDescriptionLength() {
        return ruleSetDescriptionLength;
    }

    public void setRuleSetDescriptionLength(Long ruleSetDescriptionLength) {
        this.ruleSetDescriptionLength = ruleSetDescriptionLength;
    }

    public void setTestRuleSetAccuracy(double testRuleSetAccuracy) {
        this.testRuleSetAccuracy = testRuleSetAccuracy;
    }

    public double getTestRuleSetAccuracy() {
        return testRuleSetAccuracy;
    }

    public void setTrainRuleSetAccuracy(double trainRuleSetAccuracy) {
        this.trainRuleSetAccuracy = trainRuleSetAccuracy;
    }

    public double getTrainRuleSetAccuracy() {
        return trainRuleSetAccuracy;
    }

    public void setMofNDecisionTreeFile(File mofNDecisionTreeFile) {
        this.mofNDecisionTreeFile = mofNDecisionTreeFile;
    }

    public File getMofNDecisionTreeFile() {
        return mofNDecisionTreeFile;
    }
}
