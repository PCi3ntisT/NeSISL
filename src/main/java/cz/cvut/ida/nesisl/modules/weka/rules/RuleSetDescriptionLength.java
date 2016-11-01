package main.java.cz.cvut.ida.nesisl.modules.weka.rules;

/**
 * Created by EL on 31.10.2016.
 */
public class RuleSetDescriptionLength {

    private final long descriptionLength;

    public RuleSetDescriptionLength(long descriptionLength) {
        this.descriptionLength = descriptionLength;
    }

    public long getDescriptionLength() {
        return descriptionLength;
    }

    public RuleSetDescriptionLength create(RuleSet ruleset){
        RuleSetDescriptionLengthFactor factory = RuleSetDescriptionLengthFactor.getDefault();
        return new RuleSetDescriptionLength(factory.computeDescriptionLength(ruleset));
    }
}
