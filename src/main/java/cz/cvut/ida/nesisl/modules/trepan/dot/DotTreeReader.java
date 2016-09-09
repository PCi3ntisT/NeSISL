package main.java.cz.cvut.ida.nesisl.modules.trepan.dot;

import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.trepan.MofNTreeRuleSetComplexity;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by EL on 9.9.2016.
 */
public class DotTreeReader {

    public static void main(String[] args) {
        System.out.println("a little test");
        File file = new File("./experiments/test/treeRuleSetComplexity1");
        long r = MofNTreeRuleSetComplexity.getDefault().compute(getDefault().create(file));
        System.out.println(r);

    }

    public static DotTreeReader getDefault() {
        return new DotTreeReader();
    }

    public DotTree create(File tree) {
        DotNodeFactory factory = new DotNodeFactory();

        DotNode root = null;

        Map<DotNode, List<DotNode>> edges = new HashMap<>();
        Map<DotNode, String> labels = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(tree))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (containsEdge(line)) {
                    Pair<DotNode, DotNode> sourceToTarget = parseEdgeNodes(line, factory);
                    DotNode source = sourceToTarget.getLeft();
                    DotNode target = sourceToTarget.getRight();
                    if (!edges.containsKey(source)) {
                        edges.put(source, new ArrayList<>());
                    }
                    edges.get(source).add(target);
                } else if (containsInnerNode(line)) {
                    Pair<DotNode, String> pair = parseNodeGetNodeAndLabel(line, factory);
                    DotNode node = pair.getLeft();
                    String label = pair.getRight();
                    labels.put(node, label);
                    if (null == root) {
                        root = node;
                    }
                } else if (containsLeaf(line)) {
                    Pair<DotNode, String> pair = parseNodeGetNodeAndLabel(line, factory);
                    DotNode node = pair.getLeft();
                    String label = pair.getRight();
                    labels.put(node, label);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DotTree.create(root, labels, edges);
    }

    private Pair<DotNode, String> parseNodeGetNodeAndLabel(String line, DotNodeFactory factory) {
        int end = line.indexOf("[");
        String name = line.substring(0, end).trim();
        DotNode node = factory.getNode(name);
        int endIdx = line.lastIndexOf("\"");
        String startToken = "label=\"";
        int startIdx = line.indexOf(startToken) + startToken.length();
        String label = line.substring(startIdx, endIdx);
        return new Pair<>(node, label);
    }

    private boolean containsLeaf(String line) {
        return line.contains("label")
                && !line.contains(MofNTreeRuleSetComplexity.DECISION_MARKER);
    }

    private boolean containsInnerNode(String line) {
        return line.contains("label")
                && line.contains(MofNTreeRuleSetComplexity.DECISION_MARKER);
    }

    private Pair<DotNode, DotNode> parseEdgeNodes(String line, DotNodeFactory factory) {
        line = line.replace(";", "");
        String[] splitted = line.split("->");
        DotNode source = factory.getNode(splitted[0].trim());
        DotNode target = factory.getNode(splitted[1].trim());
        return new Pair<>(source, target);
    }

    private boolean containsEdge(String line) {
        return line.contains("->")
                && line.contains(";")
                && !line.contains("[");
    }
}
