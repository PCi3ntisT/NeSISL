package main.java.cz.cvut.ida.nesisl.modules.experiments;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.modules.export.neuralNetwork.tex.TikzExporter;
import main.java.cz.cvut.ida.nesisl.modules.export.texFile.TexFile;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

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
    private long runningtime;
    private double averageSquaredError;


    public ExperimentResult(int numberOfRepeats, String learningAlg, File datasetFile) {
        this.datasetFile = datasetFile;
        this.numberOfRepeats = numberOfRepeats;
        this.learningAlg = learningAlg;
        this.myAdress = datasetFile.getAbsoluteFile().getParent() + "" + File.separator + "" + learningAlg + File.separator + learningAlg + "_" + numberOfRepeats;
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
        this.runningtime = time;
    }

    public Long getRunningtime() {
        return runningtime;
    }

    public void setAverageSquaredError(double averageSquaredError) {
        this.averageSquaredError = averageSquaredError;
    }

    public double getAverageSquaredError() {
        return averageSquaredError;
    }


    public static void printResults(List<ExperimentResult> results, String learningAlg, File datasetFile) throws FileNotFoundException {
        String experimentsFile = datasetFile.getAbsoluteFile().getParent() + File.separator + learningAlg + File.separator + "results.txt";
        File expFile = new File(experimentsFile);
        PrintWriter writer = new PrintWriter(expFile);

        writer.println(learningAlg);
        writeError(results, writer);
        writeTime(results, writer);

        writer.close();
    }

    private static void writeTime(List<ExperimentResult> results, PrintWriter writer) {
        DoubleSummaryStatistics statistics = results.stream().mapToDouble(ExperimentResult::getRunningtime).summaryStatistics();
        Long median = Tools.medianLong(results.stream().map(e -> e.getRunningtime()).collect(Collectors.toCollection(ArrayList::new)));

        writer.println("times");
        writer.println(statistics.getMin() + "\t" + statistics.getAverage() + "\t" + statistics.getMax()  + "\t" + median);
        results.forEach(r -> writer.print(r.getRunningtime() + "\t"));
        writer.print("\n");
    }

    private static void writeError(List<ExperimentResult> results, PrintWriter writer) {
        DoubleSummaryStatistics statistics = results.stream().mapToDouble(ExperimentResult::getAverageSquaredError).summaryStatistics();
        Double median = Tools.medianDouble(results.stream().map(e -> e.getAverageSquaredError()).collect(Collectors.toCollection(ArrayList::new)));

        writer.println("errors");
        writer.println(statistics.getMin() + "\t" + statistics.getAverage() + "\t" + statistics.getMax() + "\t" + median);
        results.forEach(r -> writer.print(r.getAverageSquaredError() + "\t"));
        writer.print("\n");
    }
}
