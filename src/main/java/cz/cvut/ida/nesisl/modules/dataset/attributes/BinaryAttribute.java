package main.java.cz.cvut.ida.nesisl.modules.dataset.attributes;

import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;

import java.util.HashSet;
import java.util.Set;

/**
 * Does not solve values at all. Just returns true if 'T', 't' or '1' is in the position.
 *
 * Created by EL on 23.1.2017.
 */
public class BinaryAttribute implements AttributeProprety {

    private final Set<String> values;
    private final Integer order;
    private final Integer orderWithComments;

    public BinaryAttribute(Integer order, Integer orderWithComments) {
        this.order = order;
        this.orderWithComments = orderWithComments;
        this.values = new HashSet<>();
    }


    @Override
    public Integer getOrderWithComments() {
        return orderWithComments;
    }

    @Override
    public Integer getOrder() {
        return order;
    }

    /**
     * Does not solve values at all. Just returns true if 'T', 't', 'Y', 'y' or '1' is in the position.
     * @param value
     */
    @Override
    public void addValue(String value) {
        if (DatasetImpl.UNKNOWN_VALUE.equals(value)) {
            return;
        }
        values.add(value);
    }

    /**
     * Does not solve values at all. Just returns true if 'T', 't', 'Y', 'y' or '1' is in the position.
     * @return
     */
    public Set<String> getValues() {
        return new HashSet<>(values);
    }

    /**
     * Does not solve values at all. Just returns true if 'T', 't', 'Y', 'y' or '1' is in the position.
     * @param value
     * @return
     */
    public boolean isTrue(String value) {
        return isValueTrue(value);
    }

    @Override
    public String toString() {
        return "BinaryAttribute{" +
                "values=" + values +
                ", order=" + order +
                ", orderWithComments=" + orderWithComments +
                '}';
    }

    public static boolean isValueTrue(String value) {
        return "T".equals(value)
                || "t".equals(value)
                || "Y".equals(value)
                || "y".equals(value)
                || "1".equals(value);
    }
}
