package main.java.cz.cvut.ida.nesisl.modules.extraction.jripExtractor;

import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.MultiRepresentationDataset;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.*;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by EL on 27.1.2017.
 */
public class SamplePerturbator {

    private final RandomGenerator randomGenerator;
    private final NeuralNetwork learnedNetwork;
    private final MultiRepresentationDataset originalDataset;
    private final int originalFold;
    public static double percentageOfResampling = 1.0d; // 1.0 2.0
    public static double psiOfPerturbationHappening = 0.25;
    private final Map<Pair<AttributeProprety, String>, Fact> factory = new HashMap<>();

    public SamplePerturbator(NeuralNetwork learnedNetwork, MultiRepresentationDataset dataset, int originalFold, RandomGenerator randomGenerator) {
        this.learnedNetwork = learnedNetwork;
        this.originalDataset = dataset;
        this.originalFold = originalFold;
        this.randomGenerator = randomGenerator;
    }

    /**
     * Creates new sample perturbator which, given parameters, generates new samples, simlar to those in the train dataset already presented, and returns such multi dataset; thus both Weka and NESISL representation contain those new samples.
     * real attributes are not taken into account !
     *
     * @param learnedNetwork
     * @param nesislDataset
     * @param originalFold
     * @return
     */
    public static SamplePerturbator create(NeuralNetwork learnedNetwork, MultiRepresentationDataset nesislDataset, int originalFold, RandomGenerator randomGenerator) {
        return new SamplePerturbator(learnedNetwork, nesislDataset, originalFold, randomGenerator);
    }

    /**
     * real attributes are not taken into account
     *
     * @return
     */
    public MultiRepresentationDataset run() {

        List<Map<Fact, Value>> samples = originalDataset.getNesislDataset().getTrainRawData();

        MultiRepresentationDataset extendedDataset = originalDataset.getShallowCopyWithoutSamples();
        IntStream.range(0, (int) ((percentageOfResampling / 100)* originalDataset.getNesislDataset().getNumberOfTrainData()))
                .forEach(idx -> {
                    //pick randomly
                    int randomIdx = randomGenerator.getRandom().nextInt(samples.size());
                    Map<Fact, Value> sample = samples.get(randomIdx);
                    List<String> sampleRepresentation = originalDataset.getMappingExampleToString().get(sample);
                    //System.out.println(sampleRepresentation);

                    //perturbateAndLabel
                    Pair<List<String>, Map<Fact, Value>> perturbated = perturbateAndLabel(sampleRepresentation, sample, true);
                    //System.out.println("ex\t" + extendedDataset);
                    //System.out.println("ex\t" + perturbated);

                    // insert into extended
                    extendedDataset.addTrainSample(perturbated.getLeft(), perturbated.getRight());
                });

        samples.forEach(sample -> {
            List<String> sampleRepresentation = originalDataset.getMappingExampleToString().get(sample);
            // insert also original sample, just relabeled according to the network
            Pair<List<String>, Map<Fact, Value>> relabeled = relabel(sampleRepresentation, sample);
            extendedDataset.addTrainSample(relabeled.getLeft(), relabeled.getRight());

        });

        /*System.out.println("original");
        Instances original = MultiCrossvalidation.getWekaDataset(samples, originalDataset.getNesislDataset(), originalDataset);
        System.out.println(original);

        System.out.println("extended");
        Instances extended = MultiCrossvalidation.getWekaDataset(extendedDataset.getNesislDataset().getTrainRawData(), extendedDataset.getNesislDataset(), extendedDataset);
        System.out.println(extended);

        System.exit(-111);*/
        return extendedDataset;
    }

    private Pair<List<String>, Map<Fact, Value>> relabel(List<String> sampleRepresentation, Map<Fact, Value> sample) {
      return perturbateAndLabel(new ArrayList<>(sampleRepresentation),new HashMap(sample),false);
    }

    private Pair<List<String>, Map<Fact, Value>> perturbateAndLabel(List<String> sampleRepresentation, Map<Fact, Value> sample, boolean perturbate) {
        /*System.out.println(sample);
        System.out.println(sampleRepresentation);*/

        Map<Integer, String> list = new HashMap<>(sampleRepresentation.size());
        Map<Fact, Value> map = new HashMap<>();

        originalDataset.getAttributes()
                .forEach(attribute -> {
                    if (attribute instanceof ClassAttribute) {
                        // do nothing now
                    } else if (attribute instanceof NominalAttribute) {
                        perturbateNominalAttributeStateful(list, map, attribute, sampleRepresentation, sample, perturbate);
                    } else if (attribute instanceof BinaryAttribute) {
                        perturbateBinaryAttributeStateful(sample, list, map, attribute, perturbate);
                    } else if (attribute instanceof RealAttribute) {
                        System.out.println("real attributes are not fully supported");
                        throw new NotImplementedException();
                    } else if (attribute instanceof CommentAttribute) {
                        list.put(attribute.getOrderWithComments(), "comment+");
                    }
                });

        // return 0/1 in case of binary, otherwise class value
        String classValue = learnedNetwork.predict(map, originalDataset.getNesislDataset().getClassAttribute());
        if (originalDataset.getNesislDataset().getClassAttribute().isBinary()) {
            Fact positiveClass = getFact(originalDataset.getNesislDataset().getClassAttribute(), null);
            map.put(positiveClass, Value.create(classValue));
            String finalValue = "1".equals(classValue) ? originalDataset.getNesislDataset().getClassAttribute().getPositiveClass() : originalDataset.getNesislDataset().getClassAttribute().getNegativeClass();
            list.put(originalDataset.getNesislDataset().getClassAttribute().getOrderWithComments(), finalValue);
        } else {
            originalDataset.getNesislDataset().getClassAttribute().getValues()
                    .forEach(value -> {
                        Fact currentClass = getFact(originalDataset.getNesislDataset().getClassAttribute(), value);
                        String finalValue = ((DatasetImpl.CLASS_TOKEN + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + value).equals(classValue)) ? "1" : "0";
                        map.put(currentClass, Value.create(finalValue));
                        if ("1".equals(finalValue)) {
                            list.put(originalDataset.getNesislDataset().getClassAttribute().getOrderWithComments(), value);
                        }
                    });
        }

        List<String> finalList = new ArrayList<>();
        IntStream.range(0, sampleRepresentation.size())
                .forEach(idx -> {
                    //System.out.println(idx);
                    //System.out.println("\t" + list.get(idx));
                    finalList.add(list.get(idx));
                });
        /*System.out.println("perturbated");
        System.out.println(map);
        System.out.println(finalList);
        System.exit(-112);*/

        return new Pair<>(finalList, map);
    }

    private void perturbateNominalAttributeStateful(Map<Integer, String> list, Map<Fact, Value> map, AttributeProprety attribute, List<String> sampleRepresentation, Map<Fact, Value> sample,boolean perturbate) {
        NominalAttribute nominal = (NominalAttribute) attribute;
        Fact trueOne = null;
        String trueValue = null;
        List<Fact> facts = new ArrayList<>();
        for (String value : nominal.getValues()) {
            Fact fact = getFact(nominal, value);
            facts.add(fact);
            if (sample.get(fact).isOne()) {
                Fact trueFact = fact;
                trueValue = value;
            }
        }
        List<String> minorOrder = new ArrayList<>(nominal.getValues());
        perturbate = perturbate && perturbate();
        if (null == trueOne && perturbate) {
            trueValue = minorOrder.get(randomGenerator.getRandom().nextInt(minorOrder.size()));
        } else if (nominal.isOrdered() && perturbate) {
            List<String> order = ((NominalAttribute) attribute).getDefaultOrder();
            int currentPosition = order.indexOf(trueValue);
            if (0 == currentPosition) {
                trueValue = order.get(currentPosition + 1);
            } else if (order.size() - 1 == currentPosition) {
                trueValue = order.get(currentPosition - 1);
            } else if (randomGenerator.getRandom().nextDouble() < 0.5) {
                trueValue = order.get(currentPosition - 1);
            } else {
                trueValue = order.get(currentPosition + 1);
            }
        } else if (perturbate) {
            trueValue = minorOrder.get(randomGenerator.getRandom().nextInt(minorOrder.size()));
        }
        trueOne = getFact(attribute, trueValue);
        for (Fact fact : facts) {
            if (fact.equals(trueOne)) {
                map.put(fact, Value.create("1"));
                list.put(attribute.getOrderWithComments(), trueValue);
            } else {
                map.put(fact, Value.create("0"));
            }
        }
    }

    private void perturbateBinaryAttributeStateful(Map<Fact, Value> sample, Map<Integer, String> list, Map<Fact, Value> map, AttributeProprety attribute, boolean perturbate) {
        BinaryAttribute binary = (BinaryAttribute) attribute;
        Fact fact = getFact(binary, null);
        fact.setBoolean(true);
        String value = binary.isTrue(sample.get(fact).getValue() + "") ? "1" : "0";
        if (perturbate && perturbate()) {
            value = "1".equals(value) ? "0" : "1";
        }
        map.put(fact, Value.create(value));
        list.put(attribute.getOrderWithComments(), value);
    }

    private Fact getFact(AttributeProprety attribute, String value) {
        Pair<AttributeProprety, String> pair = new Pair<>(attribute, value);
        if (!factory.containsKey(pair)) {
            if (attribute instanceof ClassAttribute) {
                ClassAttribute classAttribute = (ClassAttribute) attribute;
                if (classAttribute.isBinary()) {
                    factory.put(pair, new Fact(DatasetImpl.CLASS_TOKEN + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + classAttribute.getPositiveClass()));
                } else {
                    factory.put(pair, new Fact(DatasetImpl.CLASS_TOKEN + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + value));
                }
            } else if (null == value) {
                factory.put(pair, new Fact(attribute.getOrder() + ""));
            } else {
                factory.put(pair, new Fact(attribute.getOrder() + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + value));
            }
        }
        return factory.get(pair);
    }

    private boolean perturbate() {
        return randomGenerator.getRandom().nextDouble() < psiOfPerturbationHappening;
    }

}
