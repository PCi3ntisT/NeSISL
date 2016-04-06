package main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.export.neuralNetwork.tex.TikzExporter;
import main.java.cz.cvut.ida.nesisl.modules.export.texFile.TexFile;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

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
    private Double averageSquaredError;
    private Double RocAuc;
    private Double threshold;
    private Double accuracy;


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
        networkToLatexAndBuild(initNetwork, "origin");
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
        networkToLatexAndBuild(finalNetwork, "final");
    }

    public void setRunningTime(long time) {
        this.runningTime = time;
    }

    public Long getRunningTime() {
        return runningTime;
    }

    public void setAverageSquaredTotalError(double averageSquaredError) {
        this.averageSquaredError = averageSquaredError;
    }

    public double getAverageSquaredError() {
        return averageSquaredError;
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

    public void addExperiment(NeuralNetwork network, long start, long end, Dataset dataset) {
        this.setRunningTime(end - start);
        this.setFinalNetwork(network.getCopy());
        Map<Sample, Results> evaluation = Tools.evaluateOnTestAllAndGetResults(dataset, network);
        this.setAverageSquaredTotalError(Tools.computeAverageSquaredTotalError(evaluation));
        this.setRocAuc(AUCCalculation.create(network, evaluation).computeAUC());
        this.setThreshold(network.getClassifier().getTreshold());
        this.setAccuracy(AccuracyCalculation.create(network, evaluation).getAccuracy());
    }

    public static void storeResults(List<ExperimentResult> results, String learningAlg, File datasetFile, File structureLearningSetting, WeightLearningSetting wls) {
        String experimentsFile = datasetFile.getAbsoluteFile().getParent() + File.separator +
                learningAlg + File.separator +
                Tools.retrieveParentFolderName(structureLearningSetting) + File.separator +
                Tools.retrieveParentFolderName(wls.getFile()) + File.separator +
                "results.txt";
        File expFile = new File(experimentsFile);
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
        process.add(new Pair<>("error", () -> results.stream().mapToDouble(e -> e.getAverageSquaredError())));
        process.add(new Pair<>("accuracy", () -> results.stream().mapToDouble(e -> e.getAccuracy())));
        process.add(new Pair<>("RocAuc", () -> results.stream().mapToDouble(e -> e.getRocAuc())));
        process.add(new Pair<>("time", () -> results.stream().mapToDouble(e -> e.getRunningTime())));
        process.add(new Pair<>("threshold", () -> results.stream().mapToDouble(e -> e.getThreshold())));

        process.forEach(pair -> appendContent(pair.getLeft(), pair.getRight(), writer));
    }

    private static void writeName(String algName, PrintWriter writer) {
        appendContent(algName, () -> DoubleStream.empty(), writer);
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

    /*private static void writeTime(List<ExperimentResult> results, PrintWriter writer) {
        DoubleSummaryStatistics statistics = results.stream().mapToDouble(ExperimentResult::getRunningTime).summaryStatistics();
        Long median = Tools.medianLong(results.stream().map(e -> e.getRunningTime()).collect(Collectors.toCollection(ArrayList::new)));

        writer.println("times");
        writer.println(statistics.getMin() + "\t" + statistics.getAverage() + "\t" + statistics.getMax() + "\t" + median);
        results.forEach(r -> writer.print(r.getRunningTime() + "\t"));
        writer.print("\n");
    }

    private static void writeError(List<ExperimentResult> results, PrintWriter writer) {
        DoubleSummaryStatistics statistics = results.stream().mapToDouble(ExperimentResult::getAverageSquaredError).summaryStatistics();
        Double median = Tools.medianDouble(results.stream().map(e -> e.getAverageSquaredError()).collect(Collectors.toCollection(ArrayList::new)));

        writer.println("errors");
        writer.println(statistics.getMin() + "\t" + statistics.getAverage() + "\t" + statistics.getMax() + "\t" + median);
        results.forEach(r -> writer.print(r.getAverageSquaredError() + "\t"));
        writer.print("\n");
    }*/
}
