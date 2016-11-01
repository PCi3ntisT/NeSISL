package main.java.cz.cvut.ida.nesisl.modules.trepan;

import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotNode;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTree;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTreeTools;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 * Created by EL on 9.9.2016.
 */
public class MofNDecisionTreeDescriptionLength {

    public static MofNDecisionTreeDescriptionLength getDefault() {
        return new MofNDecisionTreeDescriptionLength();
    }

    public long computeDescriptionLength(DotTree tree) {
        return computeDescriptionLength(tree.getRoot(),tree);
    }

    private long computeDescriptionLength(DotNode node, DotTree tree) {
        if (isTerminal(node, tree)) {
            return 1;
        }
        List<DotNode> successors = tree.getEdges().get(node);

        // m-of-n
        //long m = getM(tree.getLabels().get(node));
        long n = DotTreeTools.getDefault().getN(tree.getLabels().get(node));

        return 1 // for m symbol
               + 2 * n // for antecedents; 2* for whitespaces
                + 2 // for delimiters of true branch
                + computeDescriptionLength(successors.get(0), tree)
                + computeDescriptionLength(successors.get(1), tree);
    }


    private boolean isTerminal(DotNode node, DotTree tree) {
        return !tree.getEdges().containsKey(node);
        // this is not working since label contains only label value, not the whole description
        // return !tree.getLabels().get(node).contains(DECISION_MARKER);
    }


}
