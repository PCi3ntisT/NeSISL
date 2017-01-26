package main.java.cz.cvut.ida.nesisl.modules.weka.tools;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.BinaryAttribute;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.ClassAttribute;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Antecedent;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Implication;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.Rule;
import main.java.cz.cvut.ida.nesisl.modules.weka.rules.RuleSet;

import java.util.List;
import java.util.Map;

/**
 * Created by EL on 8.9.2016.
 */
public class RuleAccuracy {

    private final RuleSet ruleSet;

    public RuleAccuracy(RuleSet ruleSet) {
        this.ruleSet = ruleSet.getCopy();
    }

    public long numberOfConsistentClassifications(List<Map<Fact, Value>> data, Dataset nesislDataset) {
        return data.stream()
                .filter(sample -> isConsistent(sample, nesislDataset, ruleSet))
                .count();
    }

    public double computeTrainAccuracy(Dataset dataset) {
        return computeAccuracy(dataset.getTrainRawData(), dataset);
    }

    public double computeTestAccuracy(Dataset dataset) {
        return computeAccuracy(dataset.getRawTestData(), dataset);
    }

    public double computeAccuracy(List<Map<Fact, Value>> data, Dataset nesislDataset) {
        // awful
        if (null == ruleSet) {
            return 0.0;
        }
        return (1.0 * numberOfConsistentClassifications(data, nesislDataset)) / data.size();
    }

    public boolean isConsistent(Map<Fact, Value> sample, Dataset nesislDataset, RuleSet ruleSet) {
        for (Rule rule : ruleSet.getRules()) {
            for (Implication implication : rule.getImplications()) {
                if (isImplicationConsistent(sample, implication)) {
                    /*System.out.println(rule.toString());
                    //System.out.println(sample);
                    System.out.println(sample.get(new Fact("17==g")));
                    System.out.println("class\t" + sample.get(new Fact("class==-")) + "\t" + sample.get(new Fact("class==+")));
                    System.out.println("tady ta metoda areSameClasses je spatne, dava ocividne furt true");
                    */

                    if (areSameClasses(sample, rule.getHead(), nesislDataset)) {
                        //System.out.println("\ttrue");
                        return true;
                    } else {
                        //System.out.println("\tfalse");
                        /*List<Fact> list = sample.keySet().stream().collect(Collectors.toList());
                        list.sort((f1, f2) -> f1.getFact().compareTo(f2.getFact()));
                        System.out.println("fail");
                        //list.forEach(f -> System.out.println(f.getFact() + "==" + sample.get(f).getValue()));
                        implication.getBody().forEach(ante -> System.out.println(sample.get(new Fact(ante.getAttribute()+"=="+ante.getValue()))));
                        System.out.println("==>\t" + sample.get(new Fact("class")) + "\t\t\t" + nesislDataset.getClassAttribute().getPositiveClass());
                        System.out.println();
                        implication.getBody().forEach(ante -> System.out.println(ante.getAttribute()+"=="+ante.getValue()));
                        System.out.println("==>\t" + rule.getHead());
                        */

                        System.out.println(sample);
                        System.out.println(rule);

                        return false;
                    }
                }
            }
        }

        // implicit behavior for binary problems
        System.out.println("Should not get so far! Missing last rule without no antecedent.");
        return false;
    }

    private boolean areSameClasses(Map<Fact, Value> sample, String head, Dataset nesislDataset) {
        ClassAttribute target = nesislDataset.getClassAttribute();
        if (target.isBinary()) {
            Fact outputFact = new Fact(DatasetImpl.CLASS_TOKEN + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + target.getPositiveClass());

            if (target.getPositiveClass().equals(head)) {
                return sample.get(outputFact).isOne();
            } else {
                return sample.get(outputFact).isZero();
            }
            /* old version
            if (target.getPositiveClass().equals(head)) {
                return sample.get(new Fact(DatasetImpl.CLASS_TOKEN)).isOne();
            } else {
                return sample.get(new Fact(DatasetImpl.CLASS_TOKEN)).isZero();
            }*/
        } else {
            return sample.get(new Fact(DatasetImpl.CLASS_TOKEN + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + head)).isOne();
        }
    }

    public boolean isImplicationConsistent(Map<Fact, Value> sample, Implication implication) {
        for (Antecedent antecedent : implication.getBody()) {
            Fact fact = new Fact(antecedent.getAttribute() + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + antecedent.getValue());
            if(!sample.containsKey(fact)){
                // awfuly added feature because of binary classes
                fact = new Fact(antecedent.getAttribute());
                if (sample.get(fact).isOne() && !BinaryAttribute.isValueTrue(antecedent.getValue())){
                    return false;
                }else if(sample.get(fact).isZero() && BinaryAttribute.isValueTrue(antecedent.getValue())){
                    return false;
                }
            }else if(!sample.get(fact).isOne()) {
                return false;
            }
        }
        return true;
    }

    public static RuleAccuracy create(RuleSet ruleSet) {
        return new RuleAccuracy(ruleSet);
    }
}
