package main.java.cz.cvut.ida.nesisl.modules.tool;

/**
 * Created by EL on 22.3.2016.
 */
public class Triple<K, T, W> {
    private final K k;
    private final T t;
    private final W w;

    public Triple(K k, T t, W w) {
        this.k = k;
        this.t = t;
        this.w = w;
    }

    public K getK() {
        return k;
    }

    public T getT() {
        return t;
    }

    public W getW() {
        return w;
    }

    @Override
    public String toString() {
        return "Triple{" +
                "k=" + k +
                ", t=" + t +
                ", w=" + w +
                '}';
    }
}
