package main.java.cz.cvut.ida.nesisl.modules.tool;

import com.sun.org.apache.xpath.internal.operations.Bool;
import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.logic.LiteralFactory;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.dataset.Value;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeImpl;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Sigmoid;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by EL on 10.2.2016.
 */
public class Tools {

    public static List<Node> generateIdentityNodes(int numberOfNodes) {
        return generateNodes(numberOfNodes, Identity.getFunction());
    }

    public static List<Node> generateNodes(int numberOfNodes,ActivationFunction function) {
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
    public static double computeSuqaredTotalError(NeuralNetwork network, Dataset dataset, WeightLearningSetting wls) {
        return processErrorStream(network, dataset).mapToDouble(Tools::squaredError).sum() * 1 / 2.0;
    }

    private static Stream<List<Double>> processErrorStream(NeuralNetwork network, Dataset dataset) {
        return dataset.getTrainData(network).stream().map(sample -> computeError(network, sample.getInput(), sample.getOutput()));
    }

    public static double computeAverageSuqaredTotalError(NeuralNetwork network, Dataset dataset) {
        return processErrorStream(network, dataset).mapToDouble(Tools::squaredError).average().orElse(0);
    }

    public static double maxSquaredError(NeuralNetwork network, Dataset dataset) {
        return processErrorStream(network, dataset).mapToDouble(Tools::squaredError).max().orElse(0);
    }

    public static double squaredError(List<Double> doubles) {
        return doubles.stream().mapToDouble(n -> n * n).sum();
    }

    public static List<Double> computeError(NeuralNetwork network, List<Value> input, List<Value> output) {
        Pair<List<Double>, Results> results = computeErrorResults(network, input, output);

        /*String s = "";
        for (Value value : input) {
            s += value.getValue() + " ";
        }
        s += "\n\t";
        for (Value value : output) {
            s += value.getValue() + " ";
        }
        s += "\n\t";
        for (Double value : results.getLeft()) {
            s += value + " ";
        }
        System.out.println(s);*/

        /*System.out.println("vysledky");
        results.getLeft().forEach(System.out::println);

        System.exit(-1);*/
        return results.getLeft();
    }

    // list<Double> of diferences (computedOutputValues - labels)
    public static Pair<List<Double>, Results> computeErrorResults(NeuralNetwork network, List<Value> input, List<Value> output) {
        //System.out.println("computeErrorResults");
        Results result = network.evaluateAndGetResults(input);
        List<Double> diff = IntStream.range(0, result.getComputedOutputs().size()).mapToObj(idx ->
                        output.get(idx).getValue() - result.getComputedOutputs().get(idx)
        ).collect(Collectors.toCollection(ArrayList::new));
        return new Pair<>(diff, result);
    }

    public static List<Double> computeAverages(List<List<Value>> list) {
        if (list.size() < 1) {
            return new ArrayList<>();
        }
        int size = list.get(0).size();
        Map<Integer, Double> cache = new HashMap<>();
        IntStream.range(0, size).forEach(idx -> cache.put(idx, 0.0d));

        list.forEach(sample ->
                        IntStream.range(0, size).parallel().forEach(idx -> {
                            Double newValue = cache.get(idx) + sample.get(idx).getValue();
                            cache.put(idx, newValue);
                        })
        );

        return IntStream.range(0, size).mapToObj(idx -> cache.get(idx) / size).collect(Collectors.toCollection(ArrayList::new));
    }

    public static void addEdgesWeightsToNetwork(NeuralNetwork network, Map<Edge, Double> weights) {
        weights.entrySet().forEach(entry -> network.setEdgeWeight(entry.getKey(), entry.getValue()));
    }

    public static Map<Sample, Results> evaluateAllAndGetResults(Dataset dataset, NeuralNetwork network) {
        Map<Sample, Results> map = new HashMap<>();
        dataset.getTrainData(network).forEach(sample -> {
            Results result = network.evaluateAndGetResults(sample.getInput());
            map.put(sample, result);
        });
        return map;
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
        return penaltyEpsilon * network.getNodes().stream().mapToDouble(node ->
                        network.getIncomingForwardEdges(node).stream().filter(edge ->
                                        edge.isModifiable() && Math.abs(network.getWeight(edge)) < treshold
                        ).mapToDouble(edge -> network.getWeight(edge)).sum()
        ).sum();
    }

    public static void printEvaluation(NeuralNetwork network, Dataset dataset) {
        dataset.getTrainData(network).forEach(sample ->{
            List<Double> output = network.evaluate(sample.getInput());

            sample.getInput().forEach(val -> System.out.print(val.getValue() + "\t"));
            System.out.print("|");
            output.forEach(val -> System.out.print("\t" + val));
            System.out.println();
        });
    }

    public static Map<Sample, Boolean> classify(Map<Sample, Results> results) {
        /*return results.entrySet().parallelStream().map(entry -> {
                    Boolean classicifaction = isClassifiedCorrectly(entry.getKey(),results.get(entry.getKey()));
                    return new HashMap.SimpleEntry<>(entry.getKey(), classicifaction);
                }
        ).collect(Collectors.toCollection(HashMap::new));*/
        HashMap<Sample, Boolean> map = new HashMap<>();
        results.entrySet().forEach(entry -> {
                    Boolean classicifaction = isClassifiedCorrectly(entry.getKey(), results.get(entry.getKey()));
                    map.put(entry.getKey(), classicifaction);
                }
        );
        return map;
    }

    private static Boolean isClassifiedCorrectly(Sample sample, Results result) {
        DoubleStream diff = IntStream.range(0, result.getComputedOutputs().size()).mapToDouble(idx ->
                        sample.getOutput().get(idx).getValue() - result.getComputedOutputs().get(idx)
        );
        //return diff.sum() < 0.1;
        return diff.max().orElse(0) < 0.25;
    }

    public static Double medianDouble(List<Double> list) {
        Collections.sort(list);
        if(0 == list.size() % 2){
            return list.get(list.size() / 2);
        }
        return (list.get(list.size() / 2) + list.get(1 + list.size() / 2)) / 2;
    }

    public static Long medianLong(List<Long> list) {
        Collections.sort(list);
        if(0 == list.size() % 2){
            return list.get(list.size() / 2);
        }
        return (list.get(list.size() / 2) + list.get(1 + list.size() / 2)) / 2;
    }
}
