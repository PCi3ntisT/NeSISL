package main.java.cz.cvut.ida.nesisl.modules.trepan.dot;

/**
 * Created by EL on 9.9.2016.
 */
public class DotNode {
    private final String name;

    private DotNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DotNode create(String name) {
        return new DotNode(name);
    }

    @Override
    public String toString() {
        return "DotNode{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DotNode dotNode = (DotNode) o;

        if (!name.equals(dotNode.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
