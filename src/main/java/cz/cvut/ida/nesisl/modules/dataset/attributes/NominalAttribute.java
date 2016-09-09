package main.java.cz.cvut.ida.nesisl.modules.dataset.attributes;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by EL on 14.8.2016.
 */
public class NominalAttribute implements AttributeProprety {

    private final Set<String> values;
    private final Integer order;
    private final Integer orderWithComments;

    public NominalAttribute(Integer order, Integer orderWithComments) {
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

    @Override
    public void addValue(String value) {
        if(DatasetImpl.UNKNOWN_VALUE.equals(value)){
            return;
        }
        values.add(value);
    }

    public Set<String> getValues() {
        return new HashSet<>(values);
    }


}
