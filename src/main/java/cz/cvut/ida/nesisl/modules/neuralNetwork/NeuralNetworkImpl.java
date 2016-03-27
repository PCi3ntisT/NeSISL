package main.java.cz.cvut.ida.nesisl.modules.neuralNetwork;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge.Type;
import main.java.cz.cvut.ida.nesisl.modules.dataset.Value;
import main.java.cz.cvut.ida.nesisl.modules.export.neuralNetwork.tex.TikzExporter;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.ConstantOne;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Created by EL on 9.2.2016.
 */
public class NeuralNetworkImpl implements NeuralNetwork {

    private final MissingValues missingValuesProcessor;

    private long numberOfHiddenLayers = 0;
    private long numberOfHiddenNodes = 0;

    private List<Node> inputNodes = new LinkedList<Node>();
    private List<Node> outputNodes = new LinkedList<Node>();

    private Map<Long, List<Node>> network = new HashMap<Long, List<Node>>();

    private Map<Node, Set<Edge>> forwardIncomingEdges = new HashMap<>();
    private Map<Node, Set<Edge>> forwardOutgoingEdges = new HashMap<>();
    private Map<Node, Set<Edge>> backwardIncomingEdges = new HashMap<>();
    private Map<Node, Set<Edge>> backwardOutgoingEdges = new HashMap<>();

    private Map<Node, Long> hiddenNodeLayer = new HashMap<>();
    private Map<Edge, Double> weights = new HashMap<>();
    private Node bias;
    private final List<Fact> outputFactOrder;
    private final List<Fact> inputFactOrder;

    public NeuralNetworkImpl(int numberOfInputNodes, int numberOfOutputNodes, MissingValues missingValuesProcessor) {
        this(Tools.generateIdentityNodes(numberOfInputNodes), Tools.generateIdentityNodes(numberOfOutputNodes), missingValuesProcessor);
    }

    public NeuralNetworkImpl(List<Node> inputNodes, List<Node> outputNodes, MissingValues missingValuesProcessor) {
        this(inputNodes, outputNodes, new HashMap<>(), new HashMap<>(),
                new HashMap<>(), new HashMap<>(),
                new HashMap<>(), new HashMap<>(), missingValuesProcessor);
    }

    public NeuralNetworkImpl(List<Node> inputNodes, List<Node> outputNodes,
                             Map<Long, List<Node>> network, Map<Edge, Double> weights,
                             Map<Node, Set<Edge>> forwardIncomingEdges,
                             Map<Node, Set<Edge>> forwardOutgoingEdges,
                             Map<Node, Set<Edge>> backwardIncomingEdges,
                             Map<Node, Set<Edge>> backwardOutgoingEdges, MissingValues missingValuesProcessor) {
        this.inputNodes = new ArrayList<>(inputNodes);
        this.outputNodes = new ArrayList<>(outputNodes);
        this.network = network;
        this.weights = weights;
        this.forwardIncomingEdges = forwardIncomingEdges;
        this.forwardOutgoingEdges = forwardOutgoingEdges;
        this.backwardIncomingEdges = backwardIncomingEdges;
        this.backwardOutgoingEdges = backwardOutgoingEdges;
        numberOfHiddenNodes = network.values().stream().flatMap(List::stream).count();
        numberOfHiddenLayers = network.keySet().stream().reduce(0l, (a, b) -> Long.max(a, b));
        network.entrySet().forEach(entry -> {
            List<Node> list = entry.getValue();
            if (null != list) {
                list.forEach(node -> this.updateHiddenNodeLayerIndex(node, entry.getKey()));
            }
        });
        this.missingValuesProcessor = missingValuesProcessor;

        this.inputFactOrder = Collections.unmodifiableList(Tools.nodeListToFactList(inputNodes));
        this.outputFactOrder = Collections.unmodifiableList(Tools.nodeListToFactList(outputNodes));

    }


    @Override
    public long getNumberOfInputNodes() {
        return inputNodes.size();
    }

    @Override
    public long getNumberOfOutputNodes() {
        return outputNodes.size();
    }

    @Override
    public long getNumberOfHiddenNodes() {
        return numberOfHiddenNodes;
    }

    @Override
    public long getNumberOfNodes() {
        return getNumberOfHiddenNodes() + getNumberOfInputNodes() + getNumberOfOutputNodes();
    }

    @Override
    public long getMaximalNumberOfHiddenLayer() {
        return numberOfHiddenLayers;
    }

    @Override
    public long getNumberOfLayers() {
        return getMaximalNumberOfHiddenLayer() + 2;
    }

    @Override
    public long getNumberOfNodesInLayer(long layerNumber) {
        if (network.containsKey(layerNumber) && null != network.get(layerNumber)) {
            return network.get(layerNumber).size();
        }
        return 0;
    }

    @Override
    public List<Node> getInputNodes() {
        return Collections.unmodifiableList(inputNodes);
    }

    @Override
    public List<Node> getOutputNodes() {
        return Collections.unmodifiableList(outputNodes);
    }

    @Override
    public List<Node> getHiddenNodes() {
        return network.values().stream().filter(list -> null != list).flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    public List<Node> getHiddenNodesInLayer(long layerNumber) {
        if (network.containsKey(layerNumber) && null != network.get(layerNumber)) {
            return Collections.unmodifiableList(network.get(layerNumber));
        }
        return new LinkedList<>();
    }

    @Override
    public Set<Node> getNodes() {
        Set<Node> set = new HashSet<>(inputNodes);
        set.addAll(new HashSet<>(outputNodes));
        set.addAll(new HashSet<>(getHiddenNodes()));
        set.add(getBias());
        return set;
    }

    @Override
    public Set<Edge> getIncomingForwardEdges(Node node) {
        return selectEdges(node, forwardIncomingEdges);
    }

    @Override
    public Set<Edge> getIncomingBackwardEdges(Node node) {
        return selectEdges(node, backwardIncomingEdges);
    }

    @Override
    public Set<Edge> getIncomingEdges(Node node) {
        return joinEdges(getIncomingForwardEdges(node), getIncomingBackwardEdges(node));
    }

    @Override
    public Set<Edge> getOutgoingForwardEdges(Node node) {
        return selectEdges(node, forwardOutgoingEdges);
    }

    @Override
    public Set<Edge> getOutgoingBackwardEdges(Node node) {
        return selectEdges(node, backwardOutgoingEdges);
    }

    @Override
    public Long getLayerNumber(Node node) {
        return hiddenNodeLayer.get(node);
    }


    @Override
    public Set<Edge> getOutgoingEdges(Node node) {
        return joinEdges(getOutgoingForwardEdges(node), getOutgoingBackwardEdges(node));
    }

    @Override
    public Map<Edge, Double> getWeights() {
        return new HashMap<>(weights);
    }

    @Override
    public Double getWeight(Edge edge) {
        if (weights.containsKey(edge) && null != weights.get(edge)) {
            return weights.get(edge);
        }
        try {
            throw new Exception("Uninitialized or null weight for edge " + edge + ".");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void addNodeAtLayerStateful(Node node, long layerNumber) {
        assert !hiddenNodeLayer.containsKey(node);
        if (layerNumber >= 0) {
            if (!network.containsKey(layerNumber) || null == network.get(layerNumber)) {
                network.put(layerNumber, new LinkedList<>());
            }
            network.get(layerNumber).add(node);
            updateHiddenNodeLayerIndex(node, layerNumber);
        } else { // layerNumber < 0
            for (long idx = getMaximalNumberOfHiddenLayer(); idx >= 0; idx--) {
                network.put(idx - layerNumber, network.get(idx));
            }
            ArrayList<Node> list = new ArrayList<>();
            list.add(node);
            network.put(0l, list);
            actualizeNodeLayerIndexes();
            numberOfHiddenLayers += -layerNumber;
        }
        numberOfHiddenNodes++;
    }

    private void actualizeNodeLayerIndexes() {
        hiddenNodeLayer.clear();
        LongStream.range(0, getMaximalNumberOfHiddenLayer() + 1).forEach(idx ->
                        getHiddenNodesInLayer(idx).forEach(node -> hiddenNodeLayer.put(node, idx))
        );
    }

    @Override
    public void addNodesAtLayerStateful(List<Node> nodes, long layerNumber) {
        nodes.forEach(node -> this.addNodeAtLayerStateful(node, layerNumber));
    }


    @Override
    public NeuralNetwork getCopy() {
        return getCopyWithMapping().getLeft();
    }

    @Override
    public Pair<NeuralNetwork, Map<Node, Node>> getCopyWithMapping() {
        Map<Node, Node> mapping = new HashMap<>();
        Pair<List<Node>, Map<Node, Node>> newInputs = Tools.copyNodes(inputNodes);
        Pair<List<Node>, Map<Node, Node>> newOutputs = Tools.copyNodes(outputNodes);
        NeuralNetwork copy = new NeuralNetworkImpl(newInputs.getLeft(), newOutputs.getLeft(), this.missingValuesProcessor);
        mapping.putAll(newInputs.getRight());
        mapping.putAll(newOutputs.getRight());

        network.entrySet().forEach(entry -> {
            if (null != entry.getValue()) {
                Long layerNumber = entry.getKey();
                Pair<List<Node>, Map<Node, Node>> copies = Tools.copyNodes(entry.getValue());
                mapping.putAll(copies.getRight());
                copy.addNodesAtLayerStateful(copies.getLeft(), layerNumber);
            }
        });
        mapping.put(getBias(), copy.getBias());

        weights.entrySet().forEach(entry -> {
            Edge oldEdge = entry.getKey();

            // TODO just for debug, shouldn't be at the final version
            if (mapping.get(oldEdge.getSource()) == mapping.get(oldEdge.getTarget())) {
                System.out.println("ccc" +
                                "\n\t" + oldEdge +
                                "\n\t" + oldEdge.getSource() +
                                "\n\t" + oldEdge.getTarget() +
                                "\n\t" + mapping.get(oldEdge.getSource()) +
                                "\n\t" + mapping.get(oldEdge.getTarget()) +
                                "\n\t" + (mapping.get(oldEdge.getTarget()) == mapping.get(oldEdge.getSource()))
                );
                throw new IllegalStateException("cannot do right now");
            }

            copy.addEdgeStateful(mapping.get(oldEdge.getSource()), mapping.get(oldEdge.getTarget()), this.getWeight(oldEdge), oldEdge.getType());
        });

        return new Pair<>(copy, mapping);
    }

    /**
     * Shallow copy - nodes are left modifiable and from origin source; nodes' parameters are shared with origin
     *
     * @return
     */
    @Override
    public NeuralNetwork getShallowCopy() {

        Map<Long, List<Node>> newNetwork = new HashMap<>();
        network.entrySet().forEach(entry -> {
            if (null != entry.getValue()) {
                newNetwork.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        });

        HashMap<Node, Set<Edge>> newForwardIncomingEdges = new HashMap<>(forwardIncomingEdges);
        HashMap<Node, Set<Edge>> newForwardOutgoingEdges = new HashMap<>(forwardOutgoingEdges);
        HashMap<Node, Set<Edge>> newBackwardIncomingEdges = new HashMap<>(backwardIncomingEdges);
        HashMap<Node, Set<Edge>> newBackwardOutgoingEdges = new HashMap<>(backwardOutgoingEdges);

        HashMap<Edge, Double> newWeights = new HashMap<>(weights);

        NeuralNetwork copy = new NeuralNetworkImpl(new ArrayList<>(inputNodes),
                new ArrayList<>(outputNodes),
                newNetwork, newWeights,
                newForwardIncomingEdges,
                newForwardOutgoingEdges,
                newBackwardIncomingEdges,
                newBackwardOutgoingEdges, this.missingValuesProcessor);
        return copy;
    }

    @Override
    public void removeHiddenNodeStateful(Node node) {
        Long layerNumber = hiddenNodeLayer.get(node);
        if (null == layerNumber) {
            throw new IllegalStateException("This node probably does not belong to this network.");
        }
        hiddenNodeLayer.remove(node);
        removeEdgesStateful(forwardIncomingEdges.get(node));
        removeEdgesStateful(forwardOutgoingEdges.get(node));
        removeEdgesStateful(backwardIncomingEdges.get(node));
        removeEdgesStateful(backwardOutgoingEdges.get(node));
        numberOfHiddenNodes--;
    }

    @Override
    public NeuralNetwork removeHiddenNode(Node node) {
        NeuralNetwork network = this.getCopy();
        network.removeHiddenNodeStateful(node);
        return network;
    }

    @Override
    public NeuralNetwork removeHiddenNodeShallow(Node node) {
        NeuralNetwork network = this.getShallowCopy();
        network.removeHiddenNodeStateful(node);
        return network;
    }

    @Override
    public void removeHiddenLayerStateful(long layerNumber) {
        if (network.containsKey(layerNumber) && null != network.get(layerNumber)) {
            network.get(layerNumber).forEach(this::removeHiddenNodeStateful);
            network.remove(layerNumber);
        }
    }

    @Override
    public NeuralNetwork removeHiddenLayer(long layerNumber) {
        NeuralNetwork network = this.getCopy();
        network.removeHiddenLayer(layerNumber);
        return network;
    }

    @Override
    public NeuralNetwork removeHiddenLayerShallow(long layerNumber) {
        NeuralNetwork network = this.getShallowCopy();
        network.removeHiddenLayer(layerNumber);
        return network;
    }

    @Override
    public void addEdgeStateful(Edge edge, Double weight) {
        setEdgeWeight(edge, weight);
        switch (edge.getType()) {
            case FORWARD:
                addEdge(edge, edge.getTarget(), forwardIncomingEdges);
                addEdge(edge, edge.getSource(), forwardOutgoingEdges);
                break;
            case BACKWARD:
                addEdge(edge, edge.getTarget(), backwardIncomingEdges);
                addEdge(edge, edge.getSource(), backwardOutgoingEdges);
                break;
            default:
                break;
        }
    }

    @Override
    public void addEdgeStateful(Node source, Node target, Double weight, Type type) {
        addEdgeStateful(new Edge(source, target, type), weight);
    }

    private void addEdge(Edge edge, Node node, Map<Node, Set<Edge>> edges) {
        if (!edges.containsKey(node) || null == edges.get(node)) {
            edges.put(node, new HashSet<>());
        }
        edges.get(node).add(edge);
    }

    @Override
    public NeuralNetwork addEdge(Node source, Node target, Double weight, Type type) {
        NeuralNetwork network = this.getCopy();
        network.addEdgeStateful(source, target, weight, type);
        return network;
    }

    @Override
    public NeuralNetwork addEdgeShallow(Node source, Node target, Double weight, Type type) {
        NeuralNetwork network = this.getShallowCopy();
        network.addEdgeStateful(source, target, weight, type);
        return network;
    }

    @Override
    public void setEdgeWeight(Edge edge, Double weight) {
        if (null == weight) {
            System.out.println(edge);
            throw new IllegalStateException("trying to add edge with weight 'null'");
        }
        weights.put(edge, weight);
    }

    @Override
    public void removeEdgesStateful(Collection<Edge> edges) {
        edges.forEach(this::removeEdgeStateful);
    }

    @Override
    public void removeEdgeStateful(Edge edge) {
        Map<Node, Set<Edge>> incoming = null;
        Map<Node, Set<Edge>> outgoing = null;
        switch (edge.getType()) {
            case FORWARD:
                incoming = forwardIncomingEdges;
                outgoing = forwardOutgoingEdges;
                break;
            case BACKWARD:
                incoming = backwardIncomingEdges;
                outgoing = backwardOutgoingEdges;
                break;
            default:
                break;
        }
        assert null != incoming;
        assert null != outgoing;


        removeEdgeFromSet(edge, incoming.get(edge.getTarget()));
        removeEdgeFromSet(edge, outgoing.get(edge.getSource()));

        /*removeEdgeFromSet(edge, forwardOutgoingEdges.get(edge.getSource()));
        removeEdgeFromSet(edge, forwardIncomingEdges.get(edge.getSource()));
        removeEdgeFromSet(edge, backwardOutgoingEdges.get(edge.getSource()));
        removeEdgeFromSet(edge, backwardIncomingEdges.get(edge.getSource()));

        removeEdgeFromSet(edge, forwardOutgoingEdges.get(edge.getTarget()));
        removeEdgeFromSet(edge, forwardIncomingEdges.get(edge.getTarget()));
        removeEdgeFromSet(edge, backwardOutgoingEdges.get(edge.getTarget()));
        removeEdgeFromSet(edge, backwardIncomingEdges.get(edge.getTarget()));
        */

        weights.remove(edge);

        // just for debug
        if (weights.containsKey(edge)) {
            throw new IllegalStateException();
        }
    }

    private static void removeEdgeFromSet(Edge edge, Set<Edge> set) {
        if (null != set && set.contains(edge)) {
            set.remove(edge);
        }
    }

    @Override
    public void setParametersStateful(Node node, Parameters parameter) {
        node.setParameters(parameter);
    }

    @Override
    public NeuralNetwork setParameters(Node node, Parameters parameter) {
        NeuralNetwork network = this.getCopy();
        network.setParametersStateful(node, parameter);
        return network;
    }

    @Override
    public NeuralNetwork setParameters(List<Pair<Node, Parameters>> tuples) {
        NeuralNetwork network = this.getCopy();
        tuples.forEach(pair -> network.setParametersStateful(pair.getLeft(), pair.getRight()));
        return network;
    }

    @Override
    public List<Double> evaluate(List<Value> input) {
        Results results = this.evaluateAndGetResults(input);
        return results.getComputedOutputs();
    }

    public Results evaluateAndGetResults(List<Value> input) {
        assert input.size() == inputNodes.size();

        Map<Node, Double> outputValues = new HashMap<>();

        IntStream.range(0, input.size()).forEach(idx -> {
            Node node = inputNodes.get(idx);
            outputValues.put(node, node.getValue(input.get(idx)));
        });
        outputValues.put(getBias(), getBias().getValue(0.0));

        LongStream.rangeClosed(0, getMaximalNumberOfHiddenLayer()).forEach(layer -> {
            if (network.containsKey(layer) && null != network.get(layer)) {
                evaluateFeedforwardLayer(network.get(layer), outputValues);
            }
        });

        evaluateFeedforwardLayer(outputNodes, outputValues);

        /*System.out.println("kontrolni vypis");
        outputValues.entrySet().forEach(entry -> System.out.println(entry.getKey() + "\t" + entry.getValue()));
        List<Double> q = outputNodes.stream().map(node -> outputValues.get(node)).collect(Collectors.toList());
        System.out.println("outputList");
        q.forEach(System.out::println);
        System.exit(-2);*/

        List<Double> outputNodesValues = outputNodes.stream().map(node -> outputValues.get(node)).collect(Collectors.toList());
        /*System.out.println("lefts");
        outputNodesValues.forEach(System.out::println);
        System.out.println("konecLeft");*/
        return new Results(outputNodesValues, outputValues);
    }

    @Override
    public MissingValues getMissingValuesProcessor() {
        return missingValuesProcessor;
    }

    @Override
    public Node getBias() {
        if (null == bias) {
            bias = NodeFactory.create(ConstantOne.getFunction());
        }
        return bias;
    }

    @Override
    public List<Fact> getInputFactOrder() {
        return inputFactOrder;
    }

    @Override
    public List<Fact> getOutputFactOrder() {
        return outputFactOrder;
    }

    /**
     * Given node is split to two - the first on possess only incoming connections to the node; the other (newOr) possess only outgoing connections and is one layer further from input layer. Note that no connection between these two nodes is added.
     *
     * @param node
     * @param newOr
     */
    @Override
    public void insertIntermezzoNodeStateful(Node node, Node newOr) {
        if (!getHiddenNodes().contains(node)) {
            throw new IllegalStateException("Cannot split one node to two when the original is not presented in the network.");
        }

        Long layerNumber = getLayerNumber(node) + 1;
        List<Node> parentLayer = getHiddenNodesInLayer(layerNumber);
        if (!parentLayer.isEmpty()) {
            for (long idx = getMaximalNumberOfHiddenLayer(); idx >= layerNumber; idx--) {
                network.put(idx + 1, network.get(idx));
            }
            actualizeNodeLayerIndexes();
            network.put(layerNumber, new ArrayList<>());
        }
        addNodeAtLayerStateful(newOr, layerNumber);

        // do the same thing as the later code
        /*Set<Edge> edges = new HashSet(getOutgoingForwardEdges(node));
        edges.stream().forEach(edge -> {
            Node successiveNode = edge.getTarget();
            addEdgeStateful(newOr, successiveNode, getWeight(edge), Type.FORWARD);
            removeEdgeStateful(edge);
        });*/

        getOutgoingForwardEdges(node).stream().forEach(edge -> {
            Node successiveNode = edge.getTarget();
            addEdgeStateful(newOr, successiveNode, getWeight(edge), edge.getType());
        });

        Set<Edge> edges = new HashSet<>(getOutgoingEdges(node));
        removeEdgesStateful(edges);

    }

    private void evaluateFeedforwardNode(Node node, Map<Node, Double> outputValues) {
        Double sum = 0.0d;
        if (forwardIncomingEdges.containsKey(node) && null != forwardIncomingEdges.get(node)) {
            sum = forwardIncomingEdges.get(node).stream().filter(edge -> edge.getSource() != edge.getTarget())
                    .mapToDouble(edge -> {
                                if (null == outputValues.get(edge.getSource())) {
                                    evaluateFeedforwardNode(edge.getSource(), outputValues);
                                }
                                return outputValues.get(edge.getSource()) * this.getWeight(edge);
                            }
                    ).sum();
        }
        outputValues.put(node, node.getValue(sum));
    }

    private void evaluateFeedforwardLayer(List<Node> nodes, Map<Node, Double> outputValues) {
        nodes.forEach(node -> evaluateFeedforwardNode(node, outputValues));
    }

    private static Set<Edge> selectEdges(Node node, Map<Node, Set<Edge>> map) {
        if (map.containsKey(node) && null != map.get(node)) {
            return Collections.unmodifiableSet(map.get(node));
        }
        return new HashSet<>();
    }

    private static Set<Edge> joinEdges(Set<Edge> forwardIncoming, Set<Edge> backwardIncoming) {
        Set<Edge> set = new HashSet<>();
        set.addAll(forwardIncoming);
        set.addAll(backwardIncoming);
        return set;
    }

    private void updateHiddenNodeLayerIndex(Node node, Long layerNumber) {
        hiddenNodeLayer.put(node, layerNumber);
        this.numberOfHiddenLayers = Math.max(layerNumber, this.numberOfHiddenLayers);
    }
}
