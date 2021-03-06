package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.kbann;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.SoftMax;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by EL on 1.3.2016.
 */
public class KBANN implements NeuralNetworkOwner {

    private NeuralNetwork network;
    private final KBANNSettings settings;

    public static KBANN create(File rawRuleFile, Dataset dataset, List<Pair<Integer, ActivationFunction>> specific, KBANNSettings kbannSettings, boolean softmaxOutputs) {
        return create(rawRuleFile, dataset, specific, kbannSettings, softmaxOutputs, true);
    }


    public static KBANN create(File rawRuleFile, Dataset dataset, List<Pair<Integer, ActivationFunction>> specific, KBANNSettings kbannSettings, boolean softmaxOutputs, boolean pertrubateNetwork) {
        return new KBANN(RuleFile.create(rawRuleFile, dataset), specific, kbannSettings, softmaxOutputs, pertrubateNetwork, dataset);
    }

    public KBANN(RuleFile ruleFile, List<Pair<Integer, ActivationFunction>> specific, KBANNSettings settings, boolean areSoftmaxOutputs, boolean pertrubateNetwork, Dataset dataset) {
        this.settings = settings;
        NeuralNetwork networkConstruction = constructNetwork(ruleFile, areSoftmaxOutputs, dataset);
        networkConstruction = addSpecificNodes(specific, networkConstruction);
        networkConstruction = addFullyConnectionToAdjacentLayers(networkConstruction);
        if (settings.isEdgesBetweenAdjacentLayersOnly()) {
            networkConstruction = removeEdgesBetweenNonAdjacentLayers(networkConstruction);
        }
        networkConstruction = fillUnbiased(networkConstruction, (pertrubateNetwork) ? settings.getRandomGenerator() : null);
        if (pertrubateNetwork) {
            networkConstruction = perturbeNetworkConnection(networkConstruction, settings);
        }
        this.network = networkConstruction;
    }

    private static NeuralNetwork fillUnbiased(NeuralNetwork network, RandomGenerator randomGenerator) {
        List<Node> nodes = new ArrayList<>(network.getHiddenNodes());
        nodes.addAll(network.getOutputNodes());

        nodes.stream()
                .filter(n -> !network.getIncomingForwardEdges(n).contains(new Edge(network.getBias(), n, Edge.Type.FORWARD)))
                .forEach(n -> network.addEdgeStateful(new Edge(network.getBias(), n, Edge.Type.FORWARD), (null == randomGenerator) ? 0 : randomGenerator.nextDouble()));

        return network;
    }

    private NeuralNetwork removeEdgesBetweenNonAdjacentLayers(NeuralNetwork network) {
        Set<Node> previousLayer = new HashSet<>(network.getInputNodes());
        Set<Node> currentLayer = null;
        Node bias = network.getBias();
        for (int idx = 0; idx <= network.getMaximalNumberOfHiddenLayer() + 1; idx++) {
            if (network.getMaximalNumberOfHiddenLayer() < idx) {
                currentLayer = new HashSet<>(network.getOutputNodes());
            } else {
                currentLayer = new HashSet<>(network.getHiddenNodesInLayer(idx));
            }
            if (currentLayer.isEmpty()) {
                continue;
            }

            for (Node node : currentLayer) {
                Set<Edge> toDelete = new HashSet<>();
                for (Edge edge : network.getIncomingForwardEdges(node)) {
                    if (!previousLayer.contains(edge.getSource()) && !bias.equals(edge.getSource())) {
                        toDelete.add(edge);
                    }
                }
                network.removeEdgesStateful(toDelete);
            }

            previousLayer = currentLayer;
        }
        return network;
    }


    public static NeuralNetwork perturbeNetworkConnection(NeuralNetwork network, KBANNSettings settings) {
        network.getWeights().entrySet().forEach(entry -> {
            if (settings.isBackpropOnly()) {
                double newValue = settings.getRandomGenerator().nextDouble() * settings.getPerturbationMagnitude();
                network.setEdgeWeight(entry.getKey(), newValue);
            } else if (entry.getKey().isModifiable()) {
                double newValue = entry.getValue() + settings.getRandomGenerator().nextDouble() * settings.getPerturbationMagnitude();
                network.setEdgeWeight(entry.getKey(), newValue);
            }
        });
        return network;
    }

    private NeuralNetwork constructNetwork(RuleFile ruleFile, boolean areSoftmaxOutputs, Dataset dataset) {
        Map<Fact, Node> map = new HashMap<>();
        Rules rules = ruleFile.preprocessRules();

        List<Node> inputNodes = createInputNodes(rules, map);
        List<Node> outputNodes = createOutputNodes(rules, map, areSoftmaxOutputs);

        NeuralNetwork network = new NeuralNetworkImpl(inputNodes,
                outputNodes,
                new MissingValueKBANN(),
                areSoftmaxOutputs
        );

        network = createNodeStructure(map, rules, network);
        network = createEdgeStructure(map, rules, network, dataset);

        return network;
    }


    private NeuralNetwork createEdgeStructure(Map<Fact, Node> map, Rules rules, NeuralNetwork network, Dataset dataset) {
        rules.getFinalRules().forEach(rule -> {
            //System.out.println("implying\t" + rule.getHead());
            //System.out.println(rule.toString());
            switch (rule.getType()) {
                case CONJUNCTION:
                    //System.out.println("con");
                    addConjunctionRuleToNetwork(rule, map, network, dataset);
                    break;
                case DISJUNCTION:
                    //System.out.println("dis");
                    addDisjunctionRuleToNetwork(rule, map, network, dataset);
                    break;
                case N_TRUE:
                    //System.out.println("n");
                    addNTrueRuleToNetwork(rule, map, network, dataset);
                    break;
                default:
                    throw new IllegalStateException();
            }
        });
        return network;
    }

    private void addNTrueRuleToNetwork(KBANNRule rule, Map<Fact, Node> map, NeuralNetwork network, Dataset dataset) {
        Node target = map.get(rule.getHead());
        target.setModifiability(rule.isModifiable());
        rule.getBody().forEach(literal -> {
            Fact fact = literal.getFact();
            Node source = map.get(fact);
            Double weight = ((literal.isPositive()) ? 1.0 : -1.0) * settings.getOmega();
            network.addEdgeStateful(new Edge(source, target, Edge.Type.FORWARD, rule.isModifiable()), weight);
        });
        Node bias = network.getBias();
        // -1*bias - because KBANN activation is s = (netInput_i - bias) and bias has value of 1 here
        network.addEdgeStateful(new Edge(bias, target, Edge.Type.FORWARD, rule.isModifiable()), -1 * (rule.getNTrue() * settings.getOmega() - 1) / 2.0);
        //this is right
        //network.addEdgeStateful(new Edge(bias, target, Edge.Type.FORWARD, rule.isModifiable()), -1 * (rule.getNTrue() * settings.getOmega() - 1) / 2.0);
    }

    private void addDisjunctionRuleToNetwork(KBANNRule rule, Map<Fact, Node> map, NeuralNetwork network, Dataset dataset) {
        Node target = map.get(rule.getHead());
        target.setModifiability(rule.isModifiable());
        rule.getBody().forEach(literal -> {
            Fact fact = literal.getFact();
            Node source = map.get(fact);
            Double weight = ((literal.isPositive()) ? 1.0 : -1.0) * settings.getOmega();
            network.addEdgeStateful(new Edge(source, target, Edge.Type.FORWARD, rule.isModifiable()), weight);
        });
        Node bias = network.getBias();
        // -1*bias - because KBANN activation is s = (netInput_i - bias) and bias has value of 1 here
        network.addEdgeStateful(new Edge(bias, target, Edge.Type.FORWARD, rule.isModifiable()), -1 * settings.getOmega() / 2);
    }

    private void addConjunctionRuleToNetwork(KBANNRule rule, Map<Fact, Node> map, NeuralNetwork network, Dataset dataset) {
        Node target = map.get(rule.getHead());
        target.setModifiability(rule.isModifiable());
        rule.getBody().forEach(literal -> {
            Fact fact = literal.getFact();
            Node source = map.get(fact);
            Double weight = ((literal.isPositive()) ? 1.0 : -1.0) * settings.getOmega();
            network.addEdgeStateful(new Edge(source, target, Edge.Type.FORWARD, rule.isModifiable()), weight);
        });
        Node bias = network.getBias();
        // -1*bias - because KBANN activation is s = (netInput_i - bias) and bias has value of 1 here
        network.addEdgeStateful(new Edge(bias, target, Edge.Type.FORWARD, rule.isModifiable()),
                -1 *
                        (rule.getBody().stream().filter(l -> l.isPositive()).count() - 1 / 2.0) *
                        settings.getOmega());
        // this is right
        //                        (rule.getBody().stream().filter(l -> l.isPositive()).count() - 1 / 2.0) *
    }

    private NeuralNetwork createNodeStructure(Map<Fact, Node> map, Rules rules, NeuralNetwork network) {
        rules.getHierarchy().forEach(pair -> {
            Node node = NodeFactory.create(Sigmoid.getFunction(), pair.getLeft().getFact());
            map.put(pair.getLeft(), node);
            network.addNodeAtLayerStateful(node, pair.getRight());
        });
        return network;
    }

    private List<Node> createOutputNodes(Rules rules, Map<Fact, Node> map, boolean areSoftmaxOutputs) {
        List<Node> list = new ArrayList<>();
        ActivationFunction fce = (areSoftmaxOutputs) ? SoftMax.getFunction() : Sigmoid.getFunction();
        rules.getConclusionFacts().forEach(fact -> {
            Node node = NodeFactory.create(fce, fact.getFact());
            list.add(node);
            map.put(fact, node);
        });
        return list;
    }

    private List<Node> createInputNodes(Rules rules, Map<Fact, Node> map) {
        List<Node> list = new ArrayList<>();
        rules.getInputFacts().forEach(fact -> {
            Node node = NodeFactory.create(Identity.getFunction(), fact.getFact());
            list.add(node);
            map.put(fact, node);
        });
        return list;
    }

    /**
     * Returns neural network; (statefull).
     *
     * @return
     */
    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    /**
     * Adds new nodes with given activation function to specific layer; given by pair of layerNumber and activation function. Also connects bias with this node.
     *
     * @param list
     * @param network
     */
    public static NeuralNetwork addSpecificNodes(List<Pair<Integer, ActivationFunction>> list, NeuralNetwork network) {
        list.forEach(pair -> {
            Node node = NodeFactory.create(pair.getRight());
            network.addNodeAtLayerStateful(node, pair.getLeft());
            network.addEdgeStateful(network.getBias(), node, 0.0d, Edge.Type.FORWARD);
        });
        return network;
    }

    /**
     * Adds edges between nodes from adjacent layers, so nodes from two adjacent layers are fully connected. Weight of these links are zero.
     * If there is a node, representing head of a unmodifiable rule, than no edge to this node is added.
     *
     * @param network
     */
    public static NeuralNetwork addFullyConnectionToAdjacentLayers(NeuralNetwork network) {
        return addFullyConnectionToAdjacentLayers(0.0d, network);
    }

    /**
     * Same functionality as addFullyConnectionToAdjacentLayers(), only init value of added edge has weight of edgeInitValue.
     *
     * @param edgeInitValue
     * @param network
     */
    private static NeuralNetwork addFullyConnectionToAdjacentLayers(double edgeInitValue, NeuralNetwork network) {
        List<Node> previousLayer = network.getInputNodes();
        List<Node> currentLayer = null;
        HashSet<Pair<Node, Node>> edges = network.getWeights().entrySet().stream()
                .map(entry -> entry.getKey().getAsPair())
                .collect(Collectors.toCollection(HashSet::new));
        for (int layerIndex = 0; layerIndex <= network.getMaximalNumberOfHiddenLayer() + 1; layerIndex++) {
            if (layerIndex > network.getMaximalNumberOfHiddenLayer()) {
                currentLayer = new ArrayList<>(network.getOutputNodes());
            } else if (network.getHiddenNodesInLayer(layerIndex).size() < 1) {
                continue;
            } else {
                currentLayer = new ArrayList<>(network.getHiddenNodesInLayer(layerIndex));
            }

            final List<Node> finalPreviousLayer = previousLayer;
            currentLayer.stream()
                    .filter(Node::isModifiable)
                    .forEach(currentNode ->
                                    finalPreviousLayer.stream()
                                            .filter(source -> !edges.contains(new Pair<>(source, currentNode)))
                                            .forEach(previousNode ->
                                                            network.addEdgeStateful(previousNode, currentNode, edgeInitValue, Edge.Type.FORWARD)
                                            )
                    );

            previousLayer = currentLayer;
            previousLayer.add(network.getBias());
        }

        return network;
    }

    /**
     * Learns KBANN's weights.
     *
     * @param dataset
     */
    public NeuralNetwork learn(Dataset dataset, WeightLearningSetting weightLearningSetting) {
        this.network = Backpropagation.feedforwardBackpropagation(this.network, dataset, weightLearningSetting);
        this.network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        return this.network;
    }
}
