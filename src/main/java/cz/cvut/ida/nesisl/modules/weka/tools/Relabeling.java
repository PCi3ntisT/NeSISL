package main.java.cz.cvut.ida.nesisl.modules.weka.tools;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.ClassAttribute;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Implication;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Rule;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;

import javax.xml.crypto.Data;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Created by EL on 8.9.2016.
 */
public class Relabeling {


    private final Dataset dataset;

    private Relabeling(Dataset dataset) {
        this.dataset = dataset;
    }

    /**
     * Relabels only train data.
     *
     * @param dataset
     * @param ruleSet
     * @return
     */
    public static Relabeling create(Dataset dataset, RuleSet ruleSet) {
        RuleAccuracy accuracy = RuleAccuracy.create(ruleSet);
        List<Map<Fact, Value>> samples = dataset.getRawData()
                .stream()
                .map(sample -> relabel(sample, ruleSet, accuracy, dataset.getClassAttribute()))
                .collect(Collectors.toCollection(ArrayList::new));
        return new Relabeling(new DatasetImpl(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), samples, dataset.getOriginalFile(), dataset.getClassAttribute()));
    }

    // little bit awful, since there are used two representations at once
    private static Map<Fact, Value> relabel(Map<Fact, Value> sample, RuleSet ruleSet, RuleAccuracy accuracy, ClassAttribute classAttribute) {
        HashMap<Fact, Value> relabeled = new HashMap<>(sample);
        for (Rule rule : ruleSet.getRules()) {
            for (Implication implication : rule.getImplications()) {
                if (accuracy.isConsistent(sample, implication)) {
                    String targetName = DatasetImpl.CLASS_TOKEN + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + rule.getHead();
                    double val = 1.0;

                    if (classAttribute.isBinary()) {
                        targetName = DatasetImpl.CLASS_TOKEN;
                        if (!rule.getHead().equals(classAttribute.getPositiveClass())) {
                            val = 0.0;
                        }
                    }

                    Value value = new Value(val);
                    Fact target = new Fact(targetName);
                    relabeled.put(target, value);
                    return relabeled;
                }
            }
        }
        if (classAttribute.isBinary()) {
            double val = 1.0;
            String targetName = DatasetImpl.CLASS_TOKEN;
            if (!ruleSet.getRules().get(0).getHead().equals(classAttribute.getPositiveClass())) {
                val = 0.0;
            }

            Value value = new Value(val);
            Fact target = new Fact(targetName);
            relabeled.put(target, value);
            return relabeled;
        }
        throw new IllegalStateException("Produced rule set which does not contain a rule without any antecedent.");
    }

    public Dataset getDataset() {
        return dataset;
    }
}
