package main.java.cz.cvut.ida.nesisl.modules.dataset.attributes;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * Created by EL on 14.8.2016.
 */
public class NominalAttribute implements AttributeProprety {

    private final Set<String> values;
    private final Integer order;
    private final Integer orderWithComments;
    private List<String> defaultValues;

    public NominalAttribute(Integer order, Integer orderWithComments, List<String> defaultValues) {
        this.order = order;
        this.orderWithComments = orderWithComments;
        this.values = new HashSet<>();
        this.defaultValues = defaultValues;
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
        if (DatasetImpl.UNKNOWN_VALUE.equals(value)) {
            return;
        }
        values.add(value);
    }

    public Set<String> getValues() {
        return new HashSet<>(values);
    }


    public boolean isOrdered() {
        if (null != defaultValues) {
            runOrderSelfCheck();
        }
        return null != defaultValues;
    }

    private void runOrderSelfCheck() {
        if ((values.size() == 3
                && values.contains("low")
                && values.contains("middle")
                && values.contains("high"))
                || (values.size() == 1 &&
                (values.contains("low")
                        || values.contains("middle")
                        || values.contains("high")))
                || (values.size() == 2 &&
                ((values.contains("low") && values.contains("middle"))
                        || (values.contains("low") && values.contains("high"))
                        || (values.contains("middle") && values.contains("high"))
                ))) {
            defaultValues = new ArrayList<>();
            defaultValues.add("low");
            defaultValues.add("middle");
            defaultValues.add("high");
        }
    }

    public List<String> getDefaultOrder(){
        return Collections.unmodifiableList(defaultValues);
    }
}
