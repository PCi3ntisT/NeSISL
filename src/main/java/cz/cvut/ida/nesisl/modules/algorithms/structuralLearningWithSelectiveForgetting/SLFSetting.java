package main.java.cz.cvut.ida.nesisl.modules.algorithms.structuralLearningWithSelectiveForgetting;

/**
 * Created by EL on 14.3.2016.
 */
public class SLFSetting {

    private final double penaltyEpsilon;
    private final double treshold;

    public SLFSetting(double penaltyEpsilon, double treshold) {
        this.penaltyEpsilon = penaltyEpsilon;
        this.treshold = treshold;
    }

    public double getPenaltyEpsilon() {
        return penaltyEpsilon;
    }

    public double getTreshold() {
        return treshold;
    }
}
