package main.java.cz.cvut.ida.nesisl.modules.dataset.attributes;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;

/**
 * Created by EL on 14.8.2016.
 */
public class CommentAttribute implements AttributeProprety {

    private final Integer order;
    private final Integer orderWithComments;

    public CommentAttribute(Integer order, Integer orderWithComments) {
        this.order = order;
        this.orderWithComments = orderWithComments;
    }

    @Override
    public void addValue(String value) {

    }

    @Override
    public Integer getOrderWithComments() {
        return orderWithComments;
    }

    @Override
    public Integer getOrder() {
        return order;
    }


}
