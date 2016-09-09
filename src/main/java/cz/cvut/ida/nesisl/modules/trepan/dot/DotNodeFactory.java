package main.java.cz.cvut.ida.nesisl.modules.trepan.dot;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by EL on 9.9.2016.
 */
public class DotNodeFactory {

    private final Map<String,DotNode> nodes;

    public DotNodeFactory() {
        this.nodes = new HashMap<>();
    }

    public DotNode getNode(String name){
        synchronized (nodes){
            if(!nodes.containsKey(name)){
                nodes.put(name,DotNode.create(name));
            }
            return nodes.get(name);
        }
    }

}
