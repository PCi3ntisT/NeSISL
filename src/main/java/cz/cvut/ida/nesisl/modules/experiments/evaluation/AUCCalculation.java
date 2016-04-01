package main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * Created by EL on 31.3.2016.
 */
public class AUCCalculation {

    private static final String AUC_VALUE_LINE_START = "Area Under the Curve for ROC is";

    private final NeuralNetwork network;
    private final HashMap<Sample,Results> evaluation;
    private Double aucValue;

    public AUCCalculation(NeuralNetwork network, Map<Sample,Results> evaluation) {
        this.network = network;
        this.evaluation = new HashMap(evaluation);
    }

    public static AUCCalculation create(NeuralNetwork network, Dataset dataset) {
        return AUCCalculation.create(network, Tools.evaluateOnTestAllAndGetResults(dataset, network));
    }

    public static AUCCalculation create(NeuralNetwork network, Map<Sample,Results> evaluation) {
        return new AUCCalculation(network, evaluation);
    }

    public Double computeAUC() {
        if (null == aucValue) {
            computeAndStoreAUC();
        }
        if (null == aucValue) {
            throw new IllegalStateException("There has been some error during computing AUC value.");
        }
        return this.aucValue;
    }

    private void computeAndStoreAUC() {
        String data = evaluation.entrySet().parallelStream()
                .map(this::resultsToString)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString().trim();
        try {
            File temporalFile = createTemporalFile();
            writeData(temporalFile, data);
            this.aucValue = runAndRetrieveAUC(temporalFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Double runAndRetrieveAUC(File file) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("java","-jar", "." + File.separator + "auc.jar", file.getAbsolutePath(), "list");
        Process process = null;
        try {
            process = builder.start();
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String line = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (null != (line = reader.readLine())) {
            if (line.startsWith(AUC_VALUE_LINE_START)) {
                String val = line.substring(AUC_VALUE_LINE_START.length() + 1);
                return Double.valueOf(val);
            }
        }
        throw new IllegalStateException("Error during computing AUC - AUC value not found in computed output.");
    }

    private void writeData(File file, String data) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write(data);
        bw.close();
    }

    private File createTemporalFile() throws IOException {
        long timeStamp = System.nanoTime();
        String threadName = Thread.currentThread().getName();
        return File.createTempFile("tempfile" + threadName + "_" + timeStamp, ".tmp");
    }

    private String resultsToString(Map.Entry<Sample, Results> sampleResultsEntry) {
        List<Value> labels = sampleResultsEntry.getKey().getOutput();
        List<Double> output = sampleResultsEntry.getValue().getComputedOutputs();
        IntFunction<String> mapping = (idx) -> output.get(idx) + "\t" + labels.get(idx).getValue().intValue() + "\n";
        return IntStream.range(0, labels.size()).mapToObj(mapping).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
    }

}
