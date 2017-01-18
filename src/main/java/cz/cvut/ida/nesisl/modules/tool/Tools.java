package main.java.cz.cvut.ida.nesisl.modules.tool;

import com.sun.org.apache.xpath.internal.SourceTree;
import main.java.cz.cvut.ida.nesisl.api.classifiers.Classifier;
import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.logic.LiteralFactory;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.tresholdClassificator.SoftMaxClassifier;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Identity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by EL on 10.2.2016.
 */
public class Tools {

    public static List<Node> generateIdentityNodes(int numberOfNodes) {
        return generateNodes(numberOfNodes, Identity.getFunction());
    }

    public static List<Node> generateNodes(int numberOfNodes, ActivationFunction function) {
        return Collections.nCopies(numberOfNodes, null).stream()
                .map(o -> NodeFactory.create(function)).collect(Collectors.toCollection(ArrayList::new));
    }

    public static Pair<List<Node>, Map<Node, Node>> copyNodes(List<Node> nodes) {
        Map<Node, Node> mapping = new HashMap<>();
        List<Node> list = new LinkedList<>();
        nodes.forEach(node -> {
            Node copy = NodeFactory.create(node);
            mapping.put(node, copy);
            list.add(copy);
        });
        return new Pair<>(list, mapping);
    }

    public static List<Fact> nodeListToFactList(List<Node> inputNodes) {
        List<Fact> result = new ArrayList<>();
        LiteralFactory factory = new LiteralFactory();
        inputNodes.forEach(node -> result.add(factory.getLiteral(node.getName()).getFact()));
        return result;
    }


    // zparametrizovat
    public static double computeSquaredTrainTotalError(NeuralNetwork network, Dataset dataset) {
        return processTrainErrorStream(network, dataset).mapToDouble(Tools::squaredError).sum() * 1 / 2.0;
    }

    private static Stream<List<Double>> processTrainErrorStream(NeuralNetwork network, Dataset dataset) {
        return processErrorStream(dataset.getTrainData(network), network);
    }

    private static Stream<List<Double>> processErrorStream(List<Sample> data, NeuralNetwork network) {
        return data.stream().map(sample -> computeError(network, sample.getInput(), sample.getOutput()));
    }

    // computeAverageSquaredTotalError
    public static double computeTotalError(Map<Sample, Results> evaluation) {
        return computeAverageSquaredTotalError(evaluation.entrySet().stream().map(entry -> computeDiff(entry.getValue(), entry.getKey().getOutput())));
    }

    // tady napsat metody genericke a na jakych mnozine dat se to trenuje
    // computeAverageSquaredTrainTotalError
    public static double computeAverageSquaredTrainTotalError(NeuralNetwork network, Dataset dataset) {
        return computeAverageSquaredTotalError(processTrainErrorStream(network, dataset));
    }

    //computeAverageSquaredTotalError
    private static double computeAverageSquaredTotalError(Stream<List<Double>> diffs) {
        return diffs.mapToDouble(Tools::squaredError).average().orElse(0);
    }

    public static double maxSquaredError(NeuralNetwork network, Dataset dataset) {
        return processTrainErrorStream(network, dataset).mapToDouble(Tools::squaredError).max().orElse(0);
    }

    public static double squaredError(List<Double> doubles) {
        return doubles.stream().mapToDouble(n -> n * n).sum();
    }

    public static List<Double> computeError(NeuralNetwork network, List<Value> input, List<Value> output) {
        Pair<List<Double>, Results> results = computeErrorResults(network, input, output);
        return results.getLeft();
    }

    // list<Double> of diferences (computedOutputValues - labels)
    public static Pair<List<Double>, Results> computeErrorResults(NeuralNetwork network, List<Value> input, List<Value> output) {
        Results result = network.evaluateAndGetResults(input);
        if (network.areSoftmaxOutputs()) {
            // softmax case
            // would be nicer to make modularized/parametrized instead of this hard coding
            return computeCrossEntropyDiff(result, output);
        } else {
            return computeDiffs(result, output);
        }
        //return computeDiffs(result, output);
    }

    private static Pair<List<Double>, Results> computeCrossEntropyDiff(Results result, List<Value> output) {
        List<Double> list = new ArrayList<>();
        IntStream.range(0, output.size())
                .forEach(idx -> list.add(
                                //result.getComputedOutputs().get(idx) - output.get(idx).getValue())
                                output.get(idx).getValue() - result.getComputedOutputs().get(idx)
                ));
        // in fact returns x_i - t_i
        return new Pair<>(list, result);
    }

    private static Pair<List<Double>, Results> computeCrossEntropy(Results result, List<Value> output) {
        List<Double> list = new ArrayList<>();
        IntStream.range(0, output.size())
                .forEach(idx -> {
                    /*System.out.println("--");
                    System.out.println(output.get(idx).getValue() + "\t" + result.getComputedOutputs().get(idx));
                    System.out.println(Math.log(result.getComputedOutputs().get(idx)));
                    */
                    list.add(
                            output.get(idx).getValue()
                                    * Math.log(result.getComputedOutputs().get(idx)));

                });
        return new Pair<>(list, result);
    }

    private static Pair<List<Double>, Results> computeDiffs(Results result, List<Value> labeledOutput) {
        List<Double> diff = computeDiff(result, labeledOutput);
        return new Pair<>(diff, result);
    }

    private static List<Double> computeDiff(Results result, List<Value> labeledOutput) {
        return IntStream.range(0, result.getComputedOutputs().size()).mapToObj(idx ->
                        labeledOutput.get(idx).getValue() - result.getComputedOutputs().get(idx)
        ).collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<Double> computeAverages(List<List<Value>> list) {
        if (list.size() < 1) {
            return new ArrayList<>();
        }
        int size = list.get(0).size();
        Map<Integer, Double> cache = new HashMap<>();
        IntStream.range(0, size).forEach(idx -> cache.put(idx, 0.0d));

        list.forEach(sample ->
                        IntStream.range(0, size)
                                //.parallel()
                                .forEach(idx -> {
                                    Double newValue = cache.get(idx) + sample.get(idx).getValue();
                                    cache.put(idx, newValue);
                                })
        );

        return IntStream.range(0, size).mapToObj(idx -> cache.get(idx) / size).collect(Collectors.toCollection(ArrayList::new));
    }

    public static void addEdgesWeightsToNetwork(NeuralNetwork network, Map<Edge, Double> weights) {
        weights.entrySet().forEach(entry -> network.setEdgeWeight(entry.getKey(), entry.getValue()));
    }

    public static Map<Sample, Results> evaluateOnTestAllAndGetResults(Dataset dataset, NeuralNetwork network) {
        //return dataset.getTrainData(network).parallelStream().collect(Collectors.toMap(sample -> sample, sample -> network.evaluateAndGetResults(sample.getInput())));
        return evaluateAllAndGetResults(dataset.getTestData(network), network);
    }

    public static Map<Sample, Results> evaluateAllAndGetResults(List<Sample> data, NeuralNetwork network) {
        //return .parallelStream().collect(Collectors.toMap(sample -> sample, sample -> network.evaluateAndGetResults(sample.getInput())));
        Map<Sample, Results> map = new HashMap<>();
        data.forEach(sample -> {
            Results result = network.evaluateAndGetResults(sample.getInput());
            map.put(sample, result);
        });
        return map;
    }

    public static Map<Sample, Results> evaluateOnTrainDataAllAndGetResults(Dataset dataset, NeuralNetwork network) {
        return evaluateAllAndGetResults(dataset.getTrainData(network), network);
    }

    public static boolean isZero(Double value) {
        double threshold = 0.00000001;
        return value >= -threshold && value <= threshold;
    }


    public static void makeFullInterLayerForwardConnections(Collection<Node> fromLayer, Collection<Node> toLayer, NeuralNetwork network, RandomGenerator randomGenerator) {
        fromLayer.forEach(source ->
                        toLayer.forEach(target -> network.addEdgeStateful(source, target, randomGenerator.nextDouble(), Edge.Type.FORWARD)
                        )
        );
    }

    public static void makeFullInterLayerForwardConnections(Collection<Node> fromLayer, Node toNode, NeuralNetwork network, RandomGenerator randomGenerator) {
        List<Node> toLayer = new ArrayList<>();
        toLayer.add(toNode);
        makeFullInterLayerForwardConnections(fromLayer, toLayer, network, randomGenerator);
    }

    public static double computePenalty(NeuralNetwork network, double penaltyEpsilon, double treshold) {
        return penaltyEpsilon * network.getNodes().stream()
                .mapToDouble(node ->
                                network.getIncomingForwardEdges(node).stream()
                                        .filter(edge ->
                                                        edge.isModifiable() && Math.abs(network.getWeight(edge)) < treshold
                                        ).mapToDouble(edge -> Math.abs(network.getWeight(edge))).sum()
                ).sum();
    }

    public static void printEvaluation(NeuralNetwork network, Dataset dataset) {
        dataset.getTrainData(network).forEach(sample -> {
            List<Double> output = network.evaluate(sample.getInput());

            sample.getInput().forEach(val -> System.out.print(val.getValue() + "\t"));
            System.out.print("|");
            IntStream.range(0, output.size()).forEach(idx -> {
                Double val = output.get(idx);
                String classified = (Tools.isZero(Math.abs(sample.getOutput().get(idx).getValue() - network.getClassifier().classifyToDouble(val)))) ? "T" : "F";
                System.out.print("\t" + val + " (" + classified + ")");
            });
            System.out.println();
        });
    }

    public static Map<Sample, Boolean> classify(Classifier classifier, Map<Sample, Results> results) {
        /*return results.entrySet().parallelStream().map(entry -> {
                    Boolean classicifaction = isClassifiedCorrectly(entry.getKey(),results.get(entry.getKey()));
                    return new HashMap.SimpleEntry<>(entry.getKey(), classicifaction);
                }
        ).collect(Collectors.toCollection(HashMap::new));*/
        HashMap<Sample, Boolean> map = new HashMap<>();
        results.entrySet().forEach(entry -> {
                    Boolean classification = isClassifiedCorrectly(classifier, entry.getKey(), results.get(entry.getKey()));
                    map.put(entry.getKey(), classification);
                }
        );
        return map;
    }

    // also awful
    private static Boolean isClassifiedCorrectly(Classifier classifier, Sample sample, Results result) {
        if (classifier instanceof SoftMaxClassifier) {
            return ((SoftMaxClassifier) classifier).isCorrectlyClassified(sample.getOutput(), result.getComputedOutputs());
        }
        for (int idx = 0; idx < result.getComputedOutputs().size(); idx++) {
            if (!isZero(Math.abs(sample.getOutput().get(idx).getValue() - classifier.classifyToDouble(result.getComputedOutputs().get(idx))))) {
                return false;
            }
        }
        return true;
    }

    public static Double medianDouble(List<Double> list) {
        if (1 == list.size()) {
            return list.get(0);
        }

        Collections.sort(list);
        if (0 == list.size() % 2) {
            return list.get(list.size() / 2 - 1);
        }
        return (list.get(list.size() / 2 - 1) + list.get(list.size() / 2)) / 2;
    }

    public static Long medianLong(List<Long> list) {
        if (1 == list.size()) {
            return list.get(0);
        }

        Collections.sort(list);
        if (0 == list.size() % 2) {
            return list.get(list.size() / 2 - 1);
        }
        return (list.get(list.size() / 2 - 1) + list.get(list.size() / 2)) / 2;
    }

    public static boolean hasConverged(List<Double> errors, Integer longTimeWindow, Integer shortTimeWindow, Double epsilonDifference) {
        if (longTimeWindow > errors.size()) {
            return false;
        }
        return Math.abs(average(errors, longTimeWindow) - average(errors, shortTimeWindow)) < epsilonDifference;
    }

    public static double average(List<Double> list, Integer timeWindow) {
        if (timeWindow > list.size()) {
            timeWindow = list.size();
        }
        return list.subList(list.size() - timeWindow, list.size()).stream().mapToDouble(d -> d).average().orElse(0);
    }

    public static double computeAverageSquaredTrainTotalErrorPlusEdgePenalty(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        //System.out.println("\t" + computeAverageSquaredTrainTotalError(network, dataset));
        return computeAverageSquaredTrainTotalError(network, dataset) + computePenalty(network, wls.getPenaltyEpsilon(), wls.getSLFThreshold());
    }

    public static double computeCrossEntropyTrainTotalError(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        return dataset.getTrainData(network).stream()
                .mapToDouble(sample -> {
                    Results results = network.evaluateAndGetResults(sample.getInput());
                    Pair<List<Double>, Results> pair = computeCrossEntropy(results, sample.getOutput());
                    /*System.out.println("");
                    pair.getLeft().stream()
                            .forEach(s -> System.out.println("\t" + s));
                    */
                    return -pair.getLeft().stream().mapToDouble(d -> d).sum();
                })
                .sum();
    }

    public static double computeSquaredTrainTotalErrorPlusEdgePenalty(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        return computeSquaredTrainTotalError(network, dataset) + computePenalty(network, wls.getPenaltyEpsilon(), wls.getSLFThreshold());
    }

    public static Double computeAverageSquaredTestTotalErrorPlusEdgePenalty(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        return computeAverageSquaredTrainTotalError(network, dataset) + computePenalty(network, wls.getPenaltyEpsilon(), wls.getSLFThreshold());
    }

    public static Map<Sample, Results> evaluateOnAndGetResults(List<Sample> evaluationSamples, NeuralNetwork network) {
        return evaluateAllAndGetResults(evaluationSamples, network);
    }

    public static String retrieveParentFolderName(File file) {
        return file.getParentFile().getName();
    }

    public static Double convergedError() {
        return 0.001;
    }

    public static File storeToTemporaryFile(String content) {
        try {
            File temp = File.createTempFile("tempfile", ".tmp");
            BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
            bw.write(content);
            bw.close();
            return temp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void storeToFile(String content, String fileName) {
        File file = new File(fileName);
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

    public static int parseInt(String input, String errorMsg) {
        try {
            return Integer.valueOf(input);
        } catch (Exception ex) {
            System.out.println(errorMsg);
            System.exit(-1);
        }
        return -1;
    }

    public static File retrieveFile(String input, String errorMsg) {
        File file = new File(input);
        if (!file.exists()) {
            System.out.println(errorMsg);
            System.exit(-1);
        }
        return file;
    }

    public static double parseDouble(String input,String  errorMsg) {
        try {
            return Double.valueOf(input);
        } catch (Exception ex) {
            System.out.println(errorMsg);
            System.exit(-1);
        }
        return -1;
    }
}
