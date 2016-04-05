package main.java.cz.cvut.ida.nesisl.modules.algorithms.regent;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGen;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Created by EL on 23.3.2016.
 */
public class Regent implements NeuralNetworkOwner {
    private final KBANN kbann;
    private final RandomGeneratorImpl randomGenerator;
    private NeuralNetwork neuralNetwork;
    private NeuralNetwork bestSoFarNetwork;
    private Double bestSoFarScore = Double.MAX_VALUE;

    public Regent(KBANN kbann, RandomGeneratorImpl randomGenerator) {
        this.randomGenerator = randomGenerator;
        this.kbann = kbann;
        this.neuralNetwork = kbann.getNeuralNetwork();
        this.bestSoFarNetwork = kbann.getNeuralNetwork();
    }

    public NeuralNetwork getNeuralNetwork() {
        return bestSoFarNetwork;
    }

    private void updateBestSoFar(NeuralNetwork network, Double error) {
        if (error < bestSoFarScore) {
            bestSoFarScore = error;
            this.bestSoFarNetwork = network;
            this.neuralNetwork = network;
        }
    }

    public static Regent create(File file, List<Pair<Integer, ActivationFunction>> specific, RandomGeneratorImpl randomGenerator, Double omega) {
        KBANN kbann = KBANN.create(file, specific, new KBANNSettings(randomGenerator, omega));
        return new Regent(kbann, randomGenerator);
    }

    public NeuralNetwork learn(Dataset dataset, WeightLearningSetting wls, RegentSetting regentSetting, KBANNSettings kbannSetting) {
        this.kbann.learn(dataset, wls);
        this.neuralNetwork = kbann.getNeuralNetwork();
        this.bestSoFarScore = Tools.computeAverageSquaredTotalError(neuralNetwork, dataset);

        List<NeuralNetwork> children = mutateInitialNetworkToMakeChildrens(this.neuralNetwork, dataset, regentSetting);
        List<Individual> population = computeFitness(children, dataset);

        Comparator<Individual> comparator = (p1, p2) -> {
            if (Tools.isZero(Math.abs(p1.getFitness() - p2.getFitness()))) {
                return Long.compare(p1.getNeuralNetwork().getNumberOfHiddenNodes(), p2.getNeuralNetwork().getNumberOfHiddenNodes());
            }
            return Double.compare(p1.getFitness(), p2.getFitness());
        };

        while (regentSetting.getMaxAllowedFitness() > regentSetting.computedFitness()) {
            System.out.println(population.size());
            List<Pair<NeuralNetwork, NeuralNetwork>> selectedForCrossover = tournamentSelectionForCrossover(population, regentSetting);
            List<NeuralNetwork> crossovered = crossover(selectedForCrossover, dataset, regentSetting);

            List<NeuralNetwork> mutation = new ArrayList<>();
            addToMutationFromCrossovers(mutation, crossovered, regentSetting);
            addToMutationFromPopulation(mutation, population, regentSetting);
            List<NeuralNetwork> mutated = mutateNetwork(mutation, dataset, regentSetting);

            List<Individual> successors = new ArrayList<>();
            addEvaluatedElites(population, successors, regentSetting, dataset, wls);
            addEvaluatedSuccessors(mutated, successors, regentSetting, dataset, wls);
            addEvaluatedSuccessors(crossovered, successors, regentSetting, dataset, wls);

            Collections.sort(population, comparator);
            updateBestSoFar(population.get(0));
        }

        bestSoFarNetwork.setClassifierStateful(ThresholdClassificator.create(bestSoFarNetwork, dataset));
        return bestSoFarNetwork;
    }

    private List<NeuralNetwork> crossover(List<Pair<NeuralNetwork, NeuralNetwork>> pairs, Dataset dataset, RegentSetting regentSetting) {
        return pairs.parallelStream().map(pair -> generatedSuccessor(pair.getLeft(), pair.getRight(), dataset, regentSetting)).flatMap(l -> l.stream()).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<NeuralNetwork> generatedSuccessor(NeuralNetwork first, NeuralNetwork second, Dataset dataset, RegentSetting regentSetting) {
        if (first == second) {
            throw new IllegalStateException();
        }

        Map<Node, NeuralNetwork> nodeOriginalNetwork = new HashMap<>();
        first.getNodes().forEach(n -> nodeOriginalNetwork.put(n, first));
        second.getNodes().forEach(n -> nodeOriginalNetwork.put(n, second));

        Map<Long, List<Node>> setA = new HashMap<>();
        Map<Long, List<Node>> setB = new HashMap<>();
        splitNodes(first, second, setA, setB, nodeOriginalNetwork);

        NeuralNetwork networkA = constructNetwork(first, setA);
        NeuralNetwork networkB = constructNetwork(first, setB);

        networkA = fillForwardEdges(networkA, nodeOriginalNetwork, first, second, dataset);
        networkB = fillForwardEdges(networkB, nodeOriginalNetwork, first, second, dataset);

        List<NeuralNetwork> result = new ArrayList<>();
        result.add(networkA);
        result.add(networkB);

        return result;
    }

    private NeuralNetwork fillForwardEdges(NeuralNetwork network, Map<Node, NeuralNetwork> nodeOriginalNetwork, NeuralNetwork first, NeuralNetwork second, Dataset dataset) {
        Set<Node> firstNetworkNodes = new HashSet<>();
        firstNetworkNodes.addAll(first.getHiddenNodes());

        Set<Node> firstNodes = new HashSet<>();
        Set<Node> secondNodes = new HashSet<>();
        network.getHiddenNodes().forEach(node -> {
            if (firstNetworkNodes.contains(node)) {
                firstNodes.add(node);
            } else {
                secondNodes.add(node);
            }
        });

        network = fillForwardEdges(firstNodes, network, first);
        network = fillForwardEdges(secondNodes, network, second);
        network = fullyConnectAdjacentLayers(network, nodeOriginalNetwork);
        network = fillInputOutputEdges(network, first, second);
        network = correctBias(network, nodeOriginalNetwork, dataset);

        return network;
    }

    private NeuralNetwork fillInputOutputEdges(NeuralNetwork network, NeuralNetwork first, NeuralNetwork second) {
        for (int inputIdx = 0; inputIdx < network.getInputNodes().size(); inputIdx++) {
            Node firstInput = first.getInputNodes().get(inputIdx);
            Node secondInput = second.getInputNodes().get(inputIdx);
            for (int outputIdx = 0; outputIdx < network.getOutputNodes().size(); outputIdx++) {
                Node firstOutput = first.getInputNodes().get(outputIdx);
                Node secondOutput = second.getInputNodes().get(outputIdx);
                Edge firstEdge = new Edge(firstInput, firstOutput, Edge.Type.FORWARD);
                Edge secondEdge = new Edge(secondInput, secondOutput, Edge.Type.FORWARD);

                if (first.getOutgoingForwardEdges(firstInput).contains(firstEdge) || second.getOutgoingForwardEdges(secondInput).contains(secondEdge)) {
                    double weight = 0.0d;
                    if (first.getOutgoingForwardEdges(firstInput).contains(firstEdge)) {
                        weight += first.getWeight(firstEdge) / 2;
                    }
                    if (second.getOutgoingForwardEdges(secondInput).contains(secondEdge)) {
                        weight += second.getWeight(secondEdge) / 2;
                    }
                    network.addEdgeStateful(network.getInputNodes().get(inputIdx), network.getOutputNodes().get(outputIdx), weight, Edge.Type.FORWARD);
                }
            }
        }
        return network;
    }

    private NeuralNetwork correctBias(NeuralNetwork network, Map<Node, NeuralNetwork> nodeOriginalNetwork, Dataset dataset) {
        Map<NeuralNetwork, Map<Sample, Results>> computedValues = new HashMap<>();
        nodeOriginalNetwork.values().forEach(net -> computedValues.put(net, Tools.evaluateOnTestAllAndGetResults(dataset, net)));

        Set<Node> nodes = new HashSet<>();
        nodeOriginalNetwork.values().forEach(net -> {
            nodes.addAll(net.getHiddenNodes());
            nodes.add(net.getBias());
            nodes.addAll(net.getInputNodes());
        });
        Map<Node, Double> averages = computeAverages(nodes, nodeOriginalNetwork, computedValues);


        Set<Node> networkNodes = new HashSet<>(network.getHiddenNodes());

        final NeuralNetwork finalNetwork = network;
        finalNetwork.getHiddenNodes().forEach(node -> adjustBias(node, finalNetwork, nodeOriginalNetwork, networkNodes, averages));
        network = averagesOutputsBiases(network, nodeOriginalNetwork);
        return network;
    }

    private Map<Node, Double> computeAverages(Set<Node> nodes, Map<Node, NeuralNetwork> nodeOriginalNetwork, Map<NeuralNetwork, Map<Sample, Results>> computedValues) {
        //return nodes.parallelStream().collect(Collectors.toMap(node -> node, node -> averageValues(node, computedValues.get(nodeOriginalNetwork.get(node)))));
        Map<Node, Double> map = new HashMap<>();
        nodes.stream().map(node -> new AbstractMap.SimpleEntry<Node, Double>(node, averageValues(node, computedValues.get(nodeOriginalNetwork.get(node))))).forEachOrdered(e -> map.put(e.getKey(), e.getValue()));
        return map;
    }

    private void adjustBias(Node node, NeuralNetwork network, Map<Node, NeuralNetwork> nodeOriginalNetwork, Set<Node> networkNodes, Map<Node, Double> averages) {
        boolean isAndNode = TopGen.isAndNode(node, nodeOriginalNetwork.get(node));
        NeuralNetwork originalNetwork = nodeOriginalNetwork.get(node);

        Predicate<? super Edge> notTransferredEdges = (edge) -> !networkNodes.contains(edge.getSource());
        Predicate<? super Edge> positiveWeightsOnly = (edge) -> originalNetwork.getWeight(edge) > 0;
        Predicate<? super Edge> negativeWeightsOnly = (edge) -> originalNetwork.getWeight(edge) < 0;

        Stream<Edge> sameEdges = originalNetwork.getIncomingForwardEdges(node).stream().filter(notTransferredEdges);
        Stream<Edge> signedEdgesOnly = isAndNode ? sameEdges.filter(positiveWeightsOnly) : sameEdges.filter(negativeWeightsOnly);

        Double deviation = signedEdgesOnly.mapToDouble(edge ->
                        originalNetwork.getWeight(edge) * averages.get(edge.getSource())
        ).sum();

        Node previousBias = originalNetwork.getBias();
        double weight = originalNetwork.getWeight(new Edge(previousBias, node, Edge.Type.FORWARD));
        if (isAndNode) {
            weight = weight - deviation;
        } else {
            weight = weight + deviation;
        }
        network.addEdgeStateful(network.getBias(), node, weight, Edge.Type.FORWARD);
    }

    private Double averageValues(Node node, Map<Sample, Results> map) {
        return map.entrySet().parallelStream().mapToDouble(entry -> entry.getValue().getComputedValues().get(node)).average().orElse(0);
    }

    private NeuralNetwork averagesOutputsBiases(NeuralNetwork network, Map<Node, NeuralNetwork> nodeOriginalNetwork) {
        Iterator<NeuralNetwork> iterator = nodeOriginalNetwork.values().iterator();
        NeuralNetwork first = iterator.next();
        NeuralNetwork second = iterator.next();
        Node bias = network.getBias();
        IntStream.range(0, network.getOutputNodes().size()).forEach(idx -> {
            double average = (first.getWeight(new Edge(first.getBias(), first.getOutputNodes().get(idx), Edge.Type.FORWARD))
                    + second.getWeight(new Edge(second.getBias(), second.getOutputNodes().get(idx), Edge.Type.FORWARD)))
                    / 2;
            network.addEdgeStateful(bias, network.getOutputNodes().get(idx), average, Edge.Type.FORWARD);
        });
        return network;
    }

    private NeuralNetwork fullyConnectAdjacentLayers(NeuralNetwork network, Map<Node, NeuralNetwork> nodeOriginalNetwork) {
        List<Node> previousLayer = network.getInputNodes();
        List<Node> currentLayer = null;
        HashSet<Pair<Node, Node>> edges = network.getWeights().entrySet().stream().map(entry -> entry.getKey().getAsPair()).collect(Collectors.toCollection(HashSet::new));
        for (int layerIndex = 0; layerIndex <= network.getMaximalNumberOfHiddenLayer() + 1; layerIndex++) {
            if (layerIndex > network.getMaximalNumberOfHiddenLayer()) {
                currentLayer = new ArrayList<>(network.getOutputNodes());
            } else if (network.getHiddenNodesInLayer(layerIndex).size() < 1) {
                continue;
            } else {
                currentLayer = new ArrayList<>(network.getHiddenNodesInLayer(layerIndex));
            }

            final List<Node> finalPreviousLayer = previousLayer;
            currentLayer.stream().filter(Node::isModifiable).forEach(currentNode ->
                            finalPreviousLayer.stream().filter(source -> !edges.contains(new Pair<>(source, currentNode)))
                                    .forEach(previousNode ->
                                                    network.addEdgeStateful(previousNode, currentNode, randomGenerator.nextDouble(), Edge.Type.FORWARD)
                                    )
            );

            previousLayer = currentLayer;
        }
        return network;
    }

    private NeuralNetwork fillForwardEdges(Set<Node> nodes, NeuralNetwork network, NeuralNetwork original) {
        List<Node> list = new ArrayList<>(nodes);
        for (int first = 0; first < list.size(); first++) {
            Node from = list.get(first);
            for (int second = first + 1; second < list.size(); second++) {
                Node to = list.get(second);
                Edge forward = new Edge(from, to, Edge.Type.FORWARD);
                Edge back = new Edge(to, from, Edge.Type.FORWARD);
                if (original.getOutgoingForwardEdges(from).contains(forward)) {
                    network.addEdgeStateful(forward, original.getWeight(forward));
                }
                if (original.getOutgoingForwardEdges(to).contains(back)) {
                    network.addEdgeStateful(back, original.getWeight(back));
                }
            }

            for (int inputIdx = 0; inputIdx < original.getInputNodes().size(); inputIdx++) {
                Node input = original.getInputNodes().get(inputIdx);
                Edge edge = new Edge(input, from, Edge.Type.FORWARD);
                if (original.getIncomingForwardEdges(from).contains(edge)) {
                    network.addEdgeStateful(network.getInputNodes().get(inputIdx), from, original.getWeight(edge), Edge.Type.FORWARD);
                }
            }

            for (int outputIdx = 0; outputIdx < original.getOutputNodes().size(); outputIdx++) {
                Node to = original.getOutputNodes().get(outputIdx);
                Edge edge = new Edge(from, to, Edge.Type.FORWARD);
                if (original.getOutgoingForwardEdges(from).contains(edge)) {
                    network.addEdgeStateful(from, network.getOutputNodes().get(outputIdx), original.getWeight(edge), Edge.Type.FORWARD);
                }
            }
        }
        return network;
    }

    private NeuralNetwork constructNetwork(NeuralNetwork network, Map<Long, List<Node>> structure) {
        NeuralNetworkImpl result = new NeuralNetworkImpl(network.getInputNodes(), network.getOutputNodes(), network.getMissingValuesProcessor());
        Long maxLayer = structure.keySet().stream().mapToLong(l -> l).max().orElse(0);
        // cause structure posses reversedLayerIdxs
        structure.entrySet().forEach(entry -> result.addNodesAtLayerStateful(entry.getValue(), maxLayer - entry.getKey()));
        return result;
    }

    private void splitNodes(NeuralNetwork first, NeuralNetwork second, Map<Long, List<Node>> setA, Map<Long, List<Node>> setB, Map<Node, NeuralNetwork> nodeOriginalNetwork) {
        Set<Node> nodeSetA = new HashSet<>();
        Set<Node> nodeSetB = new HashSet<>();
        long firstMax = first.getMaximalNumberOfHiddenLayer();
        long secondMax = second.getMaximalNumberOfHiddenLayer();
        for (long idx = 0; idx <= Math.max(firstMax, secondMax); idx++) {
            splitNodes(first.getHiddenNodesInLayer(firstMax - idx), first, idx, setA, setB, nodeSetA, nodeSetB, nodeOriginalNetwork);
            splitNodes(second.getHiddenNodesInLayer(secondMax - idx), second, idx, setA, setB, nodeSetA, nodeSetB, nodeOriginalNetwork);
        }
    }

    private void splitNodes(List<Node> nodes, NeuralNetwork network, long idx, Map<Long, List<Node>> setA, Map<Long, List<Node>> setB, Set<Node> nodeSetA, Set<Node> nodeSetB, Map<Node, NeuralNetwork> nodeOriginalNetwork) {
        nodes.forEach(node -> {
            nodeOriginalNetwork.put(node, network);
            splitNode(node, idx, setA, setB, nodeSetA, nodeSetB, nodeOriginalNetwork);
        });
    }

    private void splitNode(Node node, long idx, Map<Long, List<Node>> setA, Map<Long, List<Node>> setB, Set<Node> nodeSetA, Set<Node> nodeSetB, Map<Node, NeuralNetwork> nodeOriginalNetwork) {
        double supportA = computeFeedforwardWeightSupport(node, nodeSetA, nodeOriginalNetwork);
        double supportB = computeFeedforwardWeightSupport(node, nodeSetB, nodeOriginalNetwork);

        boolean addToA = randomGenerator.isProbable(0.5);
        if (!Tools.isZero(supportA) && !Tools.isZero(supportB)) {
            double psi = supportA / (supportA + supportB);
            addToA = randomGenerator.isProbable(psi);
            /*if (!addToA) {
                addToA = !randomGenerator.isProbable(1 - psi);
                if (addToA) {
                    // not adding the node anywhere
                    return;
                }
            }*/
        }
        if (addToA) {
            addTo(node, idx, setA, nodeSetA);
        } else {
            addTo(node, idx, setB, nodeSetB);
        }
    }

    private void addTo(Node node, long reversedLayerIdx, Map<Long, List<Node>> set, Set<Node> nodeSet) {
        if (!set.containsKey(reversedLayerIdx)) {
            set.put(reversedLayerIdx, new ArrayList<>());
        }
        set.get(reversedLayerIdx).add(node);
        nodeSet.add(node);
    }

    // only for feedforward networks
    private double computeFeedforwardWeightSupport(Node node, Set<Node> nodeSet, Map<Node, NeuralNetwork> nodeOriginalNetwork) {
        NeuralNetwork network = nodeOriginalNetwork.get(node);
        return nodeSet.stream().filter(current -> nodeOriginalNetwork.get(current) == network).mapToDouble(current -> {
                    Edge edge = new Edge(node, current, Edge.Type.FORWARD);
                    if (network.getOutgoingForwardEdges(node).contains(edge)) {
                        return Math.abs(network.getWeight(edge));
                    }
                    return 0.0;
                }
        ).sum();
    }


    private List<Pair<NeuralNetwork, NeuralNetwork>> tournamentSelectionForCrossover(List<Individual> population, RegentSetting regentSetting) {
        return tournamentSelection(population, regentSetting, regentSetting.getNumberOfCrossoverChildrenPairs());
    }

    private void addEvaluatedSuccessors(List<NeuralNetwork> parents, List<Individual> successors, RegentSetting regentSetting, Dataset dataset, WeightLearningSetting wls) {
        List<Individual> evaluated = parents.parallelStream().map(network -> evaluate(network, regentSetting, dataset, wls)).collect(Collectors.toList());
        successors.addAll(evaluated);
    }

    private void addEvaluatedElites(List<Individual> population, List<Individual> successors, RegentSetting regentSetting, Dataset dataset, WeightLearningSetting wls) {
        List<Individual> selected = population.subList(0, regentSetting.getNumberOfElites());
        List<Individual> evaluated = selected.parallelStream().map(individual -> individual.getNeuralNetwork()).parallel().map(network -> evaluate(network, regentSetting, dataset, wls)).collect(Collectors.toList());
        successors.addAll(evaluated);
    }

    private Individual evaluate(NeuralNetwork network, RegentSetting regentSetting, Dataset dataset, WeightLearningSetting wls) {
        regentSetting.increaseFitnessCountSynchronized();
        Backpropagation.feedforwardBackpropagationStateful(network, dataset, wls);
        double error = Tools.computeAverageSquaredTotalError(network, dataset);
        return new Individual(network, error);
    }

    private void addToMutationFromPopulation(List<NeuralNetwork> mutation, List<Individual> population, RegentSetting regentSetting) {
        for (int iter = 0; iter < regentSetting.getNumberOfMutationOfPopulation(); iter++) {
            Integer which = randomGenerator.nextIntegerTo(population.size());
            NeuralNetwork network = population.get(which).getNeuralNetwork();
            mutation.add(network);
            population.remove(which);
        }
    }

    private void addToMutationFromCrossovers(List<NeuralNetwork> mutation, List<NeuralNetwork> crossOvered, RegentSetting regentSetting) {
        for (int iter = 0; iter < regentSetting.getNumberOfMutationOfCrossovers(); iter++) {
            Integer which = randomGenerator.nextIntegerTo(crossOvered.size());
            NeuralNetwork network = crossOvered.get(which);
            mutation.add(network);
            crossOvered.remove(which);
        }
    }

    private List<NeuralNetwork> mutateNetwork(List<NeuralNetwork> mutation, Dataset dataset, RegentSetting regentSetting) {
        return mutation.parallelStream().map(network -> mutate(network, dataset, regentSetting, false)).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<NeuralNetwork> mutateInitialNetworkToMakeChildrens(NeuralNetwork network, Dataset dataset, RegentSetting regentSetting) {
        return LongStream.range(0, regentSetting.getPopulationSize()).parallel().mapToObj(i -> mutate(network, dataset, regentSetting, true)).collect(Collectors.toCollection(ArrayList::new));
    }

    private NeuralNetwork mutate(NeuralNetwork network, Dataset dataset, RegentSetting regentSetting, boolean canDeleteNode) {
        if (canDelete(canDeleteNode, regentSetting)) {
            return mutationByNodeDeletion(network);
        } else {
            return mutationByAddingNode(network, dataset, canDeleteNode, regentSetting);
        }
    }

    private NeuralNetwork mutationByAddingNode(NeuralNetwork network, Dataset dataset, boolean isPopulationInitialization, RegentSetting regentSetting) {
        int which = 0;
        if (isPopulationInitialization) {
            which = randomGenerator.nextIntegerTo((int) network.getNumberOfHiddenNodes());
        }
        randomGenerator.nextIntegerTo((int) network.getNumberOfHiddenNodes());
        return TopGen.generateSuccessor(network, dataset, which, regentSetting.getKBANNSetting(), randomGenerator);
    }

    private NeuralNetwork mutationByNodeDeletion(NeuralNetwork network) {
        long size = network.getNumberOfHiddenNodes();
        long idx = randomGenerator.nextLongTo(size);
        Node node = network.getHiddenNodes().get((int) idx);
        return network.removeHiddenNode(node);
    }

    private boolean canDelete(boolean canDeleteNode, RegentSetting regentSetting) {
        return canDeleteNode && randomGenerator.isProbable(regentSetting.getProbabilityOfNodeDeletion());
    }

    private List<Pair<NeuralNetwork, NeuralNetwork>> tournamentSelection(List<Individual> population, RegentSetting regentSetting, long howMany) {
        List<Pair<NeuralNetwork, NeuralNetwork>> selected = new ArrayList<>();
        while (selected.size() < howMany) {
            Individual first = tournamentSelection(population, regentSetting, null);
            Individual second = tournamentSelection(population, regentSetting, first);
            selected.add(new Pair<>(first.getNeuralNetwork(), second.getNeuralNetwork()));
        }
        return selected;
    }

    private Individual tournamentSelection(List<Individual> population, RegentSetting regentSetting, Individual alreadySelected) {
        long tournamentSize = Math.min(regentSetting.getTournamentSize(), population.size());
        Individual selected = chooseIndividual(population);
        while(alreadySelected == selected){
            selected = chooseIndividual(population);
        }
        while (tournamentSize - 1 > 0) {
            Individual second = chooseIndividual(population);
            if (second == alreadySelected || selected == alreadySelected) {
                continue;
            }
            selected = (selected.getFitness() < second.getFitness()) ? selected : second;
            tournamentSize--;
        }
        return selected;
    }

    private Individual chooseIndividual(List<Individual> population) {
        Integer start = randomGenerator.nextIntegerTo(population.size() - 1);
        return population.get(start);
    }

    private void updateBestSoFar(Pair<NeuralNetwork, Double> neuralNetworkDoublePair) {
        updateBestSoFar(neuralNetworkDoublePair.getLeft(), neuralNetworkDoublePair.getRight());
    }

    private List<Individual> computeFitness(List<NeuralNetwork> children, Dataset dataset) {
        return children.parallelStream().map(network -> computeFitness(network, dataset)).collect(Collectors.toCollection(ArrayList::new));
    }

    private Individual computeFitness(NeuralNetwork network, Dataset dataset) {
        return new Individual(network, Tools.computeAverageSquaredTotalError(network, dataset));
    }
}
