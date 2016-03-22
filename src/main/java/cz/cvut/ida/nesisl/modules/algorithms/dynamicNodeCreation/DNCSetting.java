package main.java.cz.cvut.ida.nesisl.modules.algorithms.dynamicNodeCreation;

/**
 * Created by EL on 14.3.2016.
 */
public class DNCSetting {
    private final double deltaT;
    private final long timeWindow;
    private final double cm;
    private final double ca;
    private final long hiddenNodesLimit;

    public DNCSetting(double deltaT, long timeWindow, double cm, double ca, long hiddenNodesLimit) {
        this.deltaT = deltaT;
        this.timeWindow = timeWindow;
        this.cm = cm;
        this.ca = ca;
        this.hiddenNodesLimit = hiddenNodesLimit;
    }

    public long getTimeWindow() {
        return timeWindow;
    }

    public double getDeltaT() {
        return deltaT;
    }

    public double getCm() {
        return cm;
    }

    public double getCa() {
        return ca;
    }

    public long getHiddenNodesLimit() {
        return hiddenNodesLimit;
    }
}
