package main.java.cz.cvut.ida.nesisl.modules.dataset.attributes;


/**
 * Created by EL on 14.8.2016.
 */
public class RealAttribute implements AttributeProprety{

    private Double min = Double.MAX_VALUE;
    private Double max = Double.MIN_VALUE;

    private final Integer order;
    private final Integer orderWithComments;

    public RealAttribute(Integer order, Integer orderWithComments) {
        this.order = order;
        this.orderWithComments = orderWithComments;
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
    public void addValue(String string) {
        Double value = Double.valueOf(string);
        if(value < min){
            min = value;
        }else if(value > max){
            max = value;
        }
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }
}
