package main.java.cz.cvut.ida.nesisl.api.neuralNetwork;

import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

/**
 * Created by EL on 9.2.2016.
 */
public class Edge {

    public enum Type {
        FORWARD, BACKWARD
    }

    private final Node source;
    private final Node target;
    private final Type type;
    private boolean modifiable;

    public Edge(Node source, Node target, Type type, boolean modifidable) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.modifiable = modifidable;
    }

    /**
     * Creates modifiable edge.
     * @param source
     * @param target
     * @param type
     */
    public Edge(Node source, Node target, Type type) {
        this(source,target,type,true);
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    public Pair<Node,Node> getAsPair(){
        return new Pair<Node,Node>(source,target);
    }

    public Pair<Node,Node> getAsOppositePair(){
        return new Pair<Node,Node>(target,source);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;

        Edge edge = (Edge) o;

        if (!source.equals(edge.source)) return false;
        if (!target.equals(edge.target)) return false;
        if (type != edge.type) return false;

        return true;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + target.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "source=" + source +
                ", target=" + target +
                ", type=" + type +
                ", modifiable=" + modifiable +
                '}';
    }
}
