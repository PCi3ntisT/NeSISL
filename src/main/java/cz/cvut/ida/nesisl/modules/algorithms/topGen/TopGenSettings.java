package main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen;

/**
 * Created by EL on 21.3.2016.
 */
public class TopGenSettings {

    private final Double threshold;
    private final Long numberOfSuccessors;
    private final Long lengthOfOpenList;

    public TopGenSettings(Double threshold, Long numberOfSuccessors, Long lengthOfOpenList) {
        this.threshold = threshold;
        this.numberOfSuccessors = numberOfSuccessors;
        this.lengthOfOpenList = lengthOfOpenList;
    }

    public Double getThreshold() {
        return threshold;
    }

    public Long getNumberOfSuccessors() {
        return numberOfSuccessors;
    }

    public Long getLengthOfOpenList() {
        return lengthOfOpenList;
    }
}
