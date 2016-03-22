package main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation;

/**
 * Created by EL on 16.3.2016.
 */
public class CasCorSetting {

    private int sizeOfCasCorPool;
    private long maximumNumberOfHiddenNodes;

    public CasCorSetting(int sizeOfCasCorPool, long maximumNumberOfHiddenNodes) {
        this.sizeOfCasCorPool = sizeOfCasCorPool;
        this.maximumNumberOfHiddenNodes = maximumNumberOfHiddenNodes;
    }

    public int getSizeOfCasCorPool() {
        return sizeOfCasCorPool;
    }

    public void setSizeOfCasCorPool(int sizeOfCasCorPool) {
        this.sizeOfCasCorPool = sizeOfCasCorPool;
    }

    public long getMaximumNumberOfHiddenNodes() {
        return maximumNumberOfHiddenNodes;
    }

    public void setMaximumNumberOfHiddenNodes(long maximumNumberOfHiddenNodes) {
        this.maximumNumberOfHiddenNodes = maximumNumberOfHiddenNodes;
    }
}
