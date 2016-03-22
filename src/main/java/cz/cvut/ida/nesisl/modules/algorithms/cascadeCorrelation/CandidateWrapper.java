package main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation;

import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.util.Set;

/**
 * Created by EL on 9.3.2016.
 */
public class CandidateWrapper {
    private final Double correlation;
    private final Set<Pair<Edge,Double>> edges;
    private final Node node;

    public CandidateWrapper(Double correlation, Set<Pair<Edge,Double>> edges, Node node) {
        this.correlation = correlation;
        this.edges = edges;
        this.node = node;
    }

    public Double getCorrelation() {
        return correlation;
    }

    public Set<Pair<Edge,Double>> getEdgeWeightPairs() {
        return edges;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "CandidateWrapper{" +
                "correlation=" + correlation +
                ", edges=" + edges +
                ", node=" + node +
                '}';
    }

    public static int compare(CandidateWrapper candidateWrapper1, CandidateWrapper candidateWrapper2) {
        return  candidateWrapper1.getCorrelation().compareTo(candidateWrapper2.getCorrelation());
    }
}
