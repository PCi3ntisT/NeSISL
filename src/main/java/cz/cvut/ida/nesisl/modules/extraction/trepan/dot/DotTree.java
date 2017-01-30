package main.java.cz.cvut.ida.nesisl.modules.extraction.trepan.dot;


import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by EL on 9.9.2016.
 */
public class DotTree {
    private final DotNode root;
    private final Map<DotNode, String> labels;
    private final  Map<DotNode, List<DotNode>> edges;

    private DotTree(DotNode root, Map<DotNode, String> labels, Map<DotNode, List<DotNode>> edges) {
        this.root = root;
        this.labels = labels;
        this.edges = edges;
    }

    public DotNode getRoot() {
        return root;
    }

    public Map<DotNode, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    public Map<DotNode, List<DotNode>> getEdges() {
        return Collections.unmodifiableMap(edges);
    }

    public static DotTree create(DotNode root, Map<DotNode, String> labels, Map<DotNode, List<DotNode>> edges) {
        return new DotTree(root,labels,edges);
    }
}
