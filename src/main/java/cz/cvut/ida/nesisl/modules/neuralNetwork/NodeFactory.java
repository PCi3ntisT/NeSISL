package main.java.cz.cvut.ida.nesisl.modules.neuralNetwork;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Parameters;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * Created by EL on 11.2.2016.
 */
public class NodeFactory {

    private static final AtomicLong index = new AtomicLong();

    /**
     * Returns copy of given node.
     *
     * @param node
     * @return
     */
    public static Node create(Node node) {
        return create(node.getActivationFunction(), node.getParameters(), node.getName());
    }

    public static Node create(ActivationFunction activationFunction, Parameters parameters) {
        return create(activationFunction, parameters, "");
    }

    public static Node create(ActivationFunction function) {
        return create(function, new Parameters());
    }

    public static Node create(ActivationFunction function, String nodeName) {
        return create(function, new Parameters(), nodeName);
    }

    private static Node create(ActivationFunction function, Parameters parameters, String nodeName) {
        Node node;
        long count = index.incrementAndGet();
        node = new NodeImpl(function, new Parameters(parameters), count);
        node.setName(nodeName);

        /*if("" == nodeName){
            System.out.println("odstranit ;)");
            throw new IllegalStateException("proc bezejmena?");
        }*/
        return node;
    }

    public static Node create(ActivationFunction function, Fact fact) {
        return create(function, fact.getFact());
    }


    public static List<Node> generateNodes(List<Fact> facts, ActivationFunction activationFunction) {
        return facts.stream().map(fact -> create(activationFunction, fact)).collect(Collectors.toCollection(ArrayList::new));
    }
}
