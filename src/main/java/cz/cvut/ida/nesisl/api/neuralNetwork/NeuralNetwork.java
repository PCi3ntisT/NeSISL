package main.java.cz.cvut.ida.nesisl.api.neuralNetwork;

import main.java.cz.cvut.ida.nesisl.api.classifiers.Classifier;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.dataset.Value;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by EL on 9.2.2016.
 */
public interface NeuralNetwork {

    // mozna vyclenit shallow veci do jineho interface

    public long getNumberOfInputNodes();
    public long getNumberOfOutputNodes();
    public long getNumberOfHiddenNodes();
    public long getNumberOfNodes();
    public long getMaximalNumberOfHiddenLayer();
    public long getNumberOfLayers();
    public long getNumberOfNodesInLayer(long layerNumber);

    public List<Node> getInputNodes();
    public List<Node> getOutputNodes();
    public List<Node> getHiddenNodes();
    public List<Node> getHiddenNodesInLayer(long layerNumber);
    public Set<Node> getNodes();

    public Set<Edge> getIncomingEdges(Node node);
    public Set<Edge> getIncomingForwardEdges(Node node);
    public Set<Edge> getIncomingBackwardEdges(Node node);
    public Set<Edge> getOutgoingEdges(Node node);
    public Set<Edge> getOutgoingForwardEdges(Node node);
    public Set<Edge> getOutgoingBackwardEdges(Node node);

    public Long getLayerNumber(Node node);
    public Map<Edge,Double> getWeights();
    public Double getWeight(Edge edge);

    public void addNodeAtLayerStateful(Node node, long layerNumber);
    public void addNodesAtLayerStateful(List<Node> nodes, long layerNumber);
    public NeuralNetwork getCopy();
    public Pair<NeuralNetwork,Map<Node,Node>> getCopyWithMapping();
    public NeuralNetwork getShallowCopy();

    public void removeHiddenNodeStateful(Node node);
    public NeuralNetwork removeHiddenNode(Node node);
    public NeuralNetwork removeHiddenNodeShallow(Node node);
    public void removeHiddenLayerStateful(long layerNumber);
    public NeuralNetwork removeHiddenLayer(long layerNumber);
    public NeuralNetwork removeHiddenLayerShallow(long layerNumber);


    public void addEdgeStateful(Edge edge, Double weight);
    public void addEdgeStateful(Node source, Node target, Double weight, Edge.Type type);
    public NeuralNetwork addEdge(Node source, Node target, Double weight, Edge.Type type);
    public NeuralNetwork addEdgeShallow(Node source, Node target, Double weight, Edge.Type type);
    public void setEdgeWeight(Edge edge,Double weight);

    public void removeEdgesStateful(Collection<Edge> edges);
    public void removeEdgeStateful(Edge edge);

    public void setParametersStateful(Node node, Parameters parameter);
    public NeuralNetwork setParameters(Node node, Parameters parameter);
    public NeuralNetwork setParameters(List<Pair<Node,Parameters>> tuples);

    public List<Double> evaluate(List<Value> input);
    public Results evaluateAndGetResults(List<Value> input);

    public MissingValues getMissingValuesProcessor();

    public Node getBias();

    public List<Fact> getInputFactOrder();
    public List<Fact> getOutputFactOrder();

    public void insertIntermezzoNodeStateful(Node currentParent, Node newOr);

    public NeuralNetwork setClassifier(Classifier classifier);
    public void setClassifierStateful(Classifier classifier);

    public Classifier getClassifier();

}
