package main.java.cz.cvut.ida.nesisl.modules.trepan;

import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotNode;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTree;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 * Created by EL on 9.9.2016.
 */
public class MofNTreeRuleSetComplexity {

    public static final CharSequence DECISION_MARKER = "shape=box";
    public static final String EXPRESSION_LABEL_DELIMITER = ",";
    private static final String OF_DELIMITER = "of";

    public static MofNTreeRuleSetComplexity getDefault() {
        return new MofNTreeRuleSetComplexity();
    }

    public long compute(DotTree tree) {
        return getScore(tree.getRoot(), tree);
    }

    private long getScore(DotNode node, DotTree tree) {
        //System.out.println("copmuting score for\t" + node.getName());
        if (isTerminal(node, tree)) {
            return 1;
        }
        List<DotNode> successors = tree.getEdges().get(node);

        // m-of-n
        long m = getM(tree.getLabels().get(node));
        long n = getN(tree.getLabels().get(node));

        if (1 != m) {
            System.err.println("The part of computation complexity from m-of-n rules, m != 1, not implemented. (It is a combinatorial number that is supposed to be implemented.)");
            throw new NotImplementedException();
        }

        // suppose only 1-of-n
        long left = getScore(successors.get(0), tree) * n;
        long right = getScore(successors.get(1), tree);
        return left + right;
    }

    private boolean isTerminal(DotNode node, DotTree tree) {
        return !tree.getEdges().containsKey(node);
        // this is not working since label contains only label value, not the whole description
        // return !tree.getLabels().get(node).contains(DECISION_MARKER);
    }

    private long getN(String label) {
        if(!label.contains(OF_DELIMITER)){
            return 1;
        }
        int startIdx = label.indexOf("{");
        int endIndex = label.indexOf("}");
        return label.substring(startIdx, endIndex).split(EXPRESSION_LABEL_DELIMITER).length;
    }

    private long getM(String label) {
        if(!label.contains(OF_DELIMITER)){
            return 1;
        }
        int startIdx = 0;
        int endIndex = label.indexOf(OF_DELIMITER);
        return Integer.valueOf(label.substring(startIdx, endIndex).trim());
    }
}
