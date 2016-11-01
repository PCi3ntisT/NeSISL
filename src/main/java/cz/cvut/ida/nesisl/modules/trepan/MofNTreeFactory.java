package main.java.cz.cvut.ida.nesisl.modules.trepan;


import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.RuleFile;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotNode;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTree;
import main.java.cz.cvut.ida.nesisl.modules.trepan.dot.DotTreeTools;

import java.util.*;


/**
 * Created by EL on 1.11.2016.
 */
public class MofNTreeFactory {

    private static final MofNTreeFactory factory = new MofNTreeFactory();

    public static MofNTreeFactory getDefault() {
        return factory;
    }

    public String getTheory(DotTree tree, NeuralNetwork neuralNetwork) {
        Map<String, String> classesMapping = constructVectorClassesMapping(neuralNetwork);
        return processNode(tree.getRoot(), tree, new NodeFactory(), null, true, classesMapping);
    }

    private Map<String, String> constructVectorClassesMapping(NeuralNetwork neuralNetwork) {
        Map<String, String> classesMapping = new HashMap<>();
        String vectorsAndClasses = Trepan.attributesMapping(neuralNetwork, true);
        String[] splitted = vectorsAndClasses.split("\n");
        String[] vectors = splitted[0].split("\t");
        String[] classes = splitted[1].split("\t");

        for (int idx = 2; idx < vectors.length; idx++) { // from 1 since the zeroth element is "class" and the first is number of bits
            classesMapping.put(vectors[idx].replaceAll("\\s+", ""), classes[idx]);
        }
        return classesMapping;
    }

    private String processNode(DotNode node, DotTree tree, NodeFactory nodeFactory, String ancestor, boolean ancestorWasRight, Map<String, String> classesMapping) {
        if (DotTreeTools.getDefault().isTerminal(node, tree)) {
            return processTerminal(node, tree, ancestor, ancestorWasRight, classesMapping);
        }
        return processIntermediate(node, tree, nodeFactory, ancestor, ancestorWasRight, classesMapping);
    }

    private String processTerminal(DotNode node, DotTree tree, String ancestor, boolean ancestorWasRight, Map<String, String> classesMapping) {
        String antecedents = ancestor;
        if (null == ancestor) {
            antecedents = "";
        } else if (!ancestorWasRight) {
            antecedents = RuleFile.NOT_TOKEN + "(" + antecedents + ")";
        }

        String head = (classesMapping.keySet().size() > 2) ? classesMapping.get(tree.getLabels().get(node).trim()) : tree.getLabels().get(node).trim();
        if ("false".equals(head)) {// since this problem is binary and we are only interested in positive results, aren't we?
            return "";
        } else if ("true".equals(head)) { // also, binary problem, but positive class
            head = DatasetImpl.CLASS_TOKEN;
        }

        return head + " " + RuleFile.CHANGABLE_RULE + " " + antecedents + " " + RuleFile.RULE_ENDING_TOKEN;
    }

    private String processIntermediate(DotNode node, DotTree tree, NodeFactory nodeFactory, String ancestor, boolean ancestorWasRight, Map<String, String> classesMapping) {
        StringBuilder sb = new StringBuilder();
        String currentNode = nodeFactory.getNextNode();

        if (!ancestorWasRight && null != ancestor) {
            ancestor = RuleFile.NOT_TOKEN + "(" + ancestor + ")";
        }

        String mOfN = createMofNRule(node, tree, nodeFactory, sb);

        String antecedents;
        if (null != ancestor) {
            String interMezzo = nodeFactory.getNextNode();
            sb.append(interMezzo + " " + RuleFile.CHANGABLE_RULE + " " + mOfN + RuleFile.RULE_ENDING_TOKEN).append("\n");
            antecedents = ancestor + RuleFile.ANTECEDENTS_DELIMITER + interMezzo;
        } else {
            antecedents = mOfN;
        }

        sb.append(currentNode + " " + RuleFile.CHANGABLE_RULE + " " + antecedents + RuleFile.RULE_ENDING_TOKEN).append("\n");

        sb.append(processNode(tree.getEdges().get(node).get(0), tree, nodeFactory, currentNode, true, classesMapping)).append("\n");
        sb.append(processNode(tree.getEdges().get(node).get(1), tree, nodeFactory, currentNode, false, classesMapping)).append("\n");
        return sb.toString();
    }

    /**
     * Awful statefullnes in the string builder in which auxiliar rules are written if needed.
     *
     * @param node
     * @param tree
     * @param nodeFactory
     * @param auxiliarRules
     * @return
     */
    private String createMofNRule(DotNode node, DotTree tree, NodeFactory nodeFactory, StringBuilder auxiliarRules) {
        String label = tree.getLabels().get(node);
        List<String> antecedents = DotTreeTools.getDefault().retrieveAntecedents(label);
        Long m = DotTreeTools.getDefault().getM(label);

        StringBuilder sb = new StringBuilder();
        antecedents.stream()
                .map(antecedent -> antecedent.replaceAll("\\s+", ""))
                .forEach(antecedent -> {
                    if (antecedent.contains(DotTreeTools.IS_TRUE)) {
                        antecedent = antecedent.substring(0, antecedent.length() - DotTreeTools.IS_TRUE.length());
                    } else {
                        String auxiliarNode = nodeFactory.getNextNode();
                        String originalAntecedent = antecedent.substring(0, antecedent.length() - DotTreeTools.IS_FALSE.length());
                        auxiliarRules.append(auxiliarNode + " " + RuleFile.CHANGABLE_RULE + " " + RuleFile.NOT_TOKEN + "(" + originalAntecedent + ")" + RuleFile.RULE_ENDING_TOKEN).append("\n");
                        antecedent = auxiliarNode;
                    }
                    sb.append(antecedent + ", ");
                });

        String formated = sb.toString();
        formated = formated.substring(0, formated.length() - 2);
        return "(" + RuleFile.N_TRUE + " " + m + " (" + formated + ")" + ")";
    }

    private class NodeFactory {

        private final Set<String> bag;

        public NodeFactory() {
            this.bag = new HashSet<>();
        }

        public String getNextNode() {
            synchronized (bag) {
                String node = "interConclusion" + bag.size();
                bag.add(node);
                return node;
            }
        }
    }

}
