package main.java.cz.cvut.ida.nesisl.modules.dataset.attributes;

import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by EL on 14.8.2016.
 */
public class AttributePropertyFactory {

    private int order;
    private int orderWithComments;

    public AttributeProprety create(String line) {
        if (line.trim().length() < 1) {
            return null;
        }
        String[] splitted = line.trim().split(DatasetImpl.ATTRIBUTE_DELIMITER);
        if (splitted.length < 2) {
            return null;
        }
        if (!DatasetImpl.ATTRIBUTE_TOKEN.equals(splitted[0].toUpperCase())) {
            return null;
        }
        AttributeProprety attribute = null;

        switch (splitted[1].trim().toLowerCase()) {
            case DatasetImpl.CLASS_TOKEN:
                int start = line.indexOf("{");
                int end = line.indexOf("}");

                String values = line.substring(start + 1, end);
                String[] valuesSplitted = values.split(DatasetImpl.CLASS_VALUES_DELIMITER);
                if (2 == valuesSplitted.length){
                    attribute = new ClassAttribute(order,orderWithComments,valuesSplitted[0].trim(),valuesSplitted[1].trim());
                    //attribute = new ClassAttribute(order,orderWithComments,DatasetImpl.CLASS_TOKEN,valuesSplitted[1].trim());
                }else{
                    List<String> classes = Arrays.stream(valuesSplitted).map(value -> value.trim()).collect(Collectors.toList());
                    attribute = new ClassAttribute(order,orderWithComments,classes);
                }
                break;
            case DatasetImpl.COMMENT_ATTRIBUTE_TOKEN:
                attribute = new CommentAttribute(order,orderWithComments);
                break;
            case DatasetImpl.REAL_ATTRIBUTE_TOKEN:
                attribute = new RealAttribute(order,orderWithComments);
                break;
            case DatasetImpl.NOMINAL_ATTRIBUTE_TOKEN:
                start = line.indexOf("{");
                end = line.indexOf("}");
                List<String> defaultValuesOrder = null;
                if( start > 0 || end > 0){
                    values = line.substring(start + 1, end);
                    defaultValuesOrder = Arrays.asList(values.split(DatasetImpl.CLASS_VALUES_DELIMITER));
                }
                attribute = new NominalAttribute(order,orderWithComments,defaultValuesOrder);
                break;
            case DatasetImpl.BINARY_ATTRIBUTE_TOKEN:
                attribute = new BinaryAttribute(order,orderWithComments);
                break;
            default:
                break;
        }

        if(null != attribute && !(attribute instanceof CommentAttribute)){
            order++;
        }
        orderWithComments++;

        return attribute;
    }
}
