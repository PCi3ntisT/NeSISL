package main.java.cz.cvut.ida.nesisl.modules.dataset.attributes;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by EL on 14.8.2016.
 */
public class ClassAttribute implements AttributeProprety {

    private final Integer order;
    private final Integer orderWithComments;
    private final List<String> values;
    private final boolean binary;
    private final String positiveClass;

    public ClassAttribute(Integer order, Integer orderWithComments, List<String> values) {
        this.order = order;
        this.orderWithComments = orderWithComments;
        this.values = values;
        this.binary = false;
        this.positiveClass = null;
    }

    public ClassAttribute(Integer order, Integer orderWithComments, String positiveClass, String negativeClass) {
        this.order = order;
        this.orderWithComments = orderWithComments;
        this.values = new ArrayList<>();
        values.add(positiveClass);
        values.add(negativeClass);
        this.positiveClass = positiveClass;
        this.binary = true;
    }

    @Override
    public void addValue(String value) {
        if (!values.contains(value)) {
            throw new IllegalStateException("Undeclared value of class in data: '" + value + "'.");
        }
    }

    @Override
    public Integer getOrderWithComments() {
        return orderWithComments;
    }

    @Override
    public Integer getOrder() {
        return order;
    }

    public boolean isBinary() {
        return binary;
    }

    public List<String> getValues() {
        return values;
    }

    public String getPositiveClass() {
        return positiveClass;
    }
}
