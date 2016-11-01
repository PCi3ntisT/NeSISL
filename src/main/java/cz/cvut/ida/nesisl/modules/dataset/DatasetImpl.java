package main.java.cz.cvut.ida.nesisl.modules.dataset;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.logic.Literal;
import main.java.cz.cvut.ida.nesisl.api.logic.LiteralFactory;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.MissingValues;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.*;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.tool.Triple;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by EL on 13.2.2016.
 */
public class DatasetImpl implements Dataset {


    private final File originalFile;
    private final List<Map<Fact, Value>> trainSamples;
    private final List<Map<Fact, Value>> nodeTrainSamples;
    private final List<Fact> inputFacts;
    private final ClassAttribute classAttribute;

    private final List<Fact> outputFacts;
    private List<Fact> cachedInputOrder;
    private List<Fact> cachedOutputOrder;
    private List<Sample> cachedSamples;

    private List<Double> cachedOutputAverage;
    private List<Fact> cachedTrainNodeInputOrder;
    private List<Fact> cachedTrainNodeOutputOrder;
    private List<Sample> cachedTrainNode;

    private List<Double> cachedTrainNodeOutputAverage;

    public DatasetImpl(List<Fact> inputFacts, List<Fact> outputFacts, List<Map<Fact, Value>> trainSamples, File originalFile,ClassAttribute classAttribute) {
        this.inputFacts = new ArrayList<>(inputFacts);
        this.outputFacts = new ArrayList<>(outputFacts);
        this.trainSamples = new ArrayList<>(trainSamples);
        this.originalFile = originalFile;
        this.nodeTrainSamples = new ArrayList<>(trainSamples);
        this.classAttribute = classAttribute;
    }

    public DatasetImpl(List<Fact> inputFactOrder, List<Fact> outputFactOrder, List<Map<Fact, Value>> trainData, List<Map<Fact, Value>> nodeTrainData, File originalFile,ClassAttribute classAttribute) {
        this.inputFacts = new ArrayList<>(inputFactOrder);
        this.outputFacts = new ArrayList<>(outputFactOrder);
        this.trainSamples = new ArrayList<>(trainData);
        this.originalFile = originalFile;
        this.nodeTrainSamples = new ArrayList<>(nodeTrainData);
        this.classAttribute = classAttribute;
    }

    @Override
    public List<Sample> getTrainData(NeuralNetwork network) {
        return getTrainData(network.getInputFactOrder(), network.getOutputFactOrder(), network);
    }

    @Override
    public List<Sample> getNodeTrainData(NeuralNetwork network) {
        return getNodeTrainData(network.getInputFactOrder(), network.getOutputFactOrder(), network);
    }

    @Override
    public List<Fact> getInputFactOrder() {
        return Collections.unmodifiableList(inputFacts);
    }

    @Override
    public List<Fact> getOutputFactOrder() {
        return Collections.unmodifiableList(outputFacts);
    }

    @Override
    public List<Double> getAverageOutputs(NeuralNetwork network) {
        synchronized (this) {
            cachedOutputAverage = getAverageOutputs(network, cachedOutputAverage, getTrainData(network), cachedInputOrder, cachedOutputOrder);
            return cachedOutputAverage;
        }
    }

    @Override
    public List<Double> getTrainNodeAverageOutputs(NeuralNetwork network) {
        synchronized (this) {
            cachedTrainNodeOutputAverage = getAverageOutputs(network, cachedTrainNodeOutputAverage, getNodeTrainData(network), cachedTrainNodeInputOrder, cachedTrainNodeOutputOrder);
            return cachedTrainNodeOutputAverage;
        }
    }

    public List<Double> getAverageOutputs(NeuralNetwork network, List<Double> cachedAverage, List<Sample> data, List<Fact> cachedInput, List<Fact> cachedOutput) {
        synchronized (this) {
            List<Double> result = cachedAverage;
            if (null == cachedOutput || !areDataCached(network, cachedInput, cachedOutput)) {
                List<List<Value>> outputs = data
                        //.parallelStream()
                        .stream()
                        .map(sample -> sample.getOutput())
                        .collect(Collectors.toCollection(ArrayList::new));
                result = Tools.computeAverages(outputs);
            }
            return result;
        }
    }

    @Override
    public File getOriginalFile() {
        return this.originalFile;
    }

    @Override
    public List<Map<Fact, Value>> getTrainRawData() {
        return new ArrayList<>(trainSamples);
    }

    @Override
    public List<Map<Fact, Value>> getRawTestData() {
        return new ArrayList<>(nodeTrainSamples);
    }

    @Override
    public String cannonicalOutput(Map<Fact, Value> sample) {
        synchronized (outputFacts) {
            return IntStream.range(0, outputFacts.size())
                    .mapToObj(idx -> sample.get(outputFacts.get(idx)) + ",") // delimiter for cannonic form
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();
        }
    }

    @Override
    public List<Sample> getTestData(NeuralNetwork network) {
        return getNodeTrainData(network.getInputFactOrder(), network.getOutputFactOrder(), network);
    }

    private boolean areDataCached(NeuralNetwork network, List<Fact> cachedInputFactOrder, List<Fact> cachedOutputFactOrder) {
        return areDataCached(network.getInputFactOrder(), getInputFactOrder(), cachedInputFactOrder, cachedOutputFactOrder);
    }

    private List<Sample> getTrainData(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, NeuralNetwork network) {
        synchronized (this) {
            if (areDataCached(inputFactPermutation, outputFactPermutation, cachedInputOrder, cachedOutputOrder)) {
                return this.cachedSamples;
            }
            Triple<List<Sample>, List<Fact>, List<Fact>> permuted = permuteSamples(inputFactPermutation, outputFactPermutation, trainSamples, network);
            this.cachedSamples = permuted.getK();
            this.cachedInputOrder = permuted.getT();
            this.cachedOutputOrder = permuted.getW();
            return this.cachedSamples;
        }
    }

    private List<Sample> getNodeTrainData(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, NeuralNetwork network) {
        synchronized (this) {
            if (areDataCached(inputFactPermutation, outputFactPermutation, cachedTrainNodeInputOrder, cachedTrainNodeOutputOrder)) {
                return this.cachedTrainNode;
            }
            Triple<List<Sample>, List<Fact>, List<Fact>> permuted = permuteSamples(inputFactPermutation, outputFactPermutation, nodeTrainSamples, network);
            this.cachedTrainNode = permuted.getK();
            this.cachedTrainNodeInputOrder = permuted.getT();
            this.cachedTrainNodeOutputOrder = permuted.getW();
            return this.cachedTrainNode;
        }
    }

    private Triple<List<Sample>, List<Fact>, List<Fact>> permuteSamples(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, List<Map<Fact, Value>> cachedSamples, NeuralNetwork network) {
        List<Sample> samples = cachedSamples
                //.parallelStream()
                .stream()
                .map(map -> permuteSample(map, inputFactPermutation, outputFactPermutation, network.getMissingValuesProcessor())).collect(Collectors.toCollection(ArrayList::new));
        return new Triple<>(samples, Collections.unmodifiableList(inputFactPermutation), Collections.unmodifiableList(outputFactPermutation));
    }

    private static Sample permuteSample(Map<Fact, Value> sample, List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, MissingValues missingValuesProcessor) {
        List<Value> inputList = permuteSamplePart(sample, inputFactPermutation, missingValuesProcessor);
        List<Value> outputList = permuteSamplePart(sample, outputFactPermutation, missingValuesProcessor);
        return new SampleImpl(inputList, outputList);
    }

    private static List<Value> permuteSamplePart(Map<Fact, Value> sample, List<Fact> inputFactPermutation, MissingValues missingValuesProcessor) {
        return inputFactPermutation.stream().map(fact -> missingValuesProcessor.processMissingValueToValue(sample.get(fact))).collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean areDataCached(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, List<Fact> cachedInputOrder, List<Fact> cachedOutputOrder) {
        synchronized (this) {
            if (null == cachedInputOrder || null == cachedOutputOrder || !inputFactPermutation.equals(cachedInputOrder) || !outputFactPermutation.equals(cachedOutputOrder)) {
                return false;
            }
            return true;
        }
    }

    public static Dataset stratifiedSplit(Dataset dataset, RandomGenerator randomGenerator, int numberOfFolds) {
        synchronized (dataset) {
            if(1 == numberOfFolds){
                return new DatasetImpl(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), dataset.getTrainRawData(), dataset.getTrainRawData(), dataset.getOriginalFile(),dataset.getClassAttribute());
            }

            List<Map<Fact, Value>> trainData = new ArrayList<>();
            List<Map<Fact, Value>> nodeTrainData = new ArrayList<>();
            List<List<Map<Fact, Value>>> splitted = splitAccordingToResults(dataset);
            splitted.forEach(group -> {
                if (!group.isEmpty()) {
                    Collections.shuffle(group, randomGenerator.getRandom());
                    int border = group.size() / numberOfFolds;
                    if (2 * border != group.size()
                            && group.size() > 2
                            && randomGenerator.nextDouble() < 0.5) {
                        border++;
                    }
                    border = Math.min(border, group.size());
                    border = Math.max(border, 0);
                    trainData.addAll(group.subList(0, border));
                    nodeTrainData.addAll(group.subList(border, group.size()));
                }
            });
            return new DatasetImpl(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), trainData, nodeTrainData, dataset.getOriginalFile(),dataset.getClassAttribute());
        }
    }

    public ClassAttribute getClassAttribute() {
        return classAttribute;
    }

    public static Dataset stratifiedSplitHalfToHalf(Dataset dataset, RandomGenerator randomGenerator) {
        return stratifiedSplit(dataset, randomGenerator, 2);
    }

    private static List<List<Map<Fact, Value>>> splitAccordingToResults(Dataset dataset) {
        synchronized (dataset) {
            return dataset
                    .getTrainRawData()
                    .stream()
                    .collect(Collectors.groupingBy(e -> dataset.cannonicalOutput(e), Collectors.toCollection(ArrayList::new)))
                    .values()
                    .stream()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public static final String FACT_DELIMITER = "\\s+";
    public static final String INPUT_OUTPUT_DELIMITER = "|";
    public static final String TRAIN_TOKEN = "TRAIN SET";

    public static final String ATTRIBUTE_TOKEN = "@ATTRIBUTE";
    public static final String DATA_TOKEN = "@DATA";
    public static final String AMBIGUOUS_TOKEN = "@AMBIGUOUS";
    public static final char COMMENTED_LINE_START = '%';
    public static final String ATTRIBUTE_DELIMITER = "\\s+";
    public static final String AMBIGUOUS_DELIMITER = "\\s+";
    public static final String DATA_DELIMITER = ",";
    public static final String CLASS_VALUES_DELIMITER = ",";
    public static final String ATTRIBUTE_VALUE_DELIMITER = "==";
    public static final String UNKNOWN_VALUE = "?";


    public static final String REAL_ATTRIBUTE_TOKEN = "real";
    public static final String NOMINAL_ATTRIBUTE_TOKEN = "nominal";
    public static final String CLASS_TOKEN = "class";
    public static final String COMMENT_ATTRIBUTE_TOKEN = "comment";

    enum State {
        TRAIN_SET,
        FACT_LINE,
        EXAMPLES,
        ATTRIBUTES,
        DATA,
        AMBIGUOUS
    }

    enum Attribute {
        REAL,
        NOMINAL,
        CLASS,
        COMMENT
    }

    public static Dataset parseDataset(String pathToFile, boolean normalize) {
        return parseAndGetDataset(new File(pathToFile), normalize);
    }

    private static Pair<Dataset, Instances> parseAndGetDatasets(String pathToFile, boolean normalize) {
        return parseAndGetDatasets(new File(pathToFile), normalize);
    }

    /**
     * Parses and returns datasets - first for NESISL, second for WEKA.
     *
     * @param file
     * @param normalize
     * @return
     */
    public static Pair<Dataset, Instances> parseAndGetDatasets(File file, boolean normalize) {
        Triple<List<AttributeProprety>, List<List<String>>, List<Pair<String, Set<String>>>> triple = parseDataset(file, normalize);
        List<Pair<String, Set<String>>> ambigious = triple.getW();
        List<AttributeProprety> attributes = triple.getK();
        List<List<String>> examples = triple.getT();

        Dataset nesislDataset = createNesislDataset(ambigious, attributes, examples, file, normalize);
        Instances wekaDataset = createWekaDataset(ambigious, attributes, examples, file, normalize);

        return new Pair<>(nesislDataset, wekaDataset);
    }

    /**
     * Parses and returns datasets which contains NESISL representation as well as structures for generating WEKA datasets.
     *
     * @param file
     * @param normalize
     * @return
     */
    public static MultiRepresentationDataset parseMultiRepresentationDataset(File file, boolean normalize) {
        Triple<List<AttributeProprety>, List<List<String>>, List<Pair<String, Set<String>>>> triple = parseDataset(file, normalize);
        List<Pair<String, Set<String>>> ambigious = triple.getW();
        List<AttributeProprety> attributes = triple.getK();
        List<List<String>> examples = triple.getT();

        Map<Map<Fact, Value>,String> map = new HashMap<>();

        //Dataset nesislDataset = createNesislDataset(ambigious, attributes, examples, file, normalize);
        List<Fact> inputFacts = generateInputFacts(ambigious, attributes);
        List<Fact> outputFacts = generateOutputFacts(attributes); // for one class only (predicting values of one attribute only)
        Pair<List<Map<Fact, Value>>,Map<Map<Fact, Value>,List<String>>> pair = reformateDataWithMapping(inputFacts, outputFacts, examples, attributes, ambigious, normalize);
        ClassAttribute classAttribute = (ClassAttribute) attributes.stream()
                .filter(attribute -> attribute instanceof ClassAttribute).toArray()[0];
        List<Map<Fact, Value>> reformatedExamples = pair.getLeft();
        Map<Map<Fact, Value>, List<String>> mappingExampleToString = pair.getRight();
        Dataset nesislDataset = new DatasetImpl(Collections.unmodifiableList(inputFacts), Collections.unmodifiableList(outputFacts), reformatedExamples, file, classAttribute);

        //Instances wekaDataset = createWekaDataset(ambigious, attributes, examples, file, normalize);

        return MultiRepresentationDataset.create(attributes,examples,file,normalize,nesislDataset,mappingExampleToString,ambigious);
    }

    public static Instances createWekaDataset(List<Pair<String, Set<String>>> ambigious, List<AttributeProprety> attributes, List<List<String>> examples, File file, boolean normalize) {
        System.out.println("TODO - resolve ambigious ");
        String stringForm = datasetToString(attributes, examples, normalize);

        InputStream stream = new ByteArrayInputStream(stringForm.getBytes(StandardCharsets.UTF_8));
        Instances data = null;
        try {
            data = ConverterUtils.DataSource.read(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    private static String generateWekaHead(List<AttributeProprety> attributes){
        StringBuilder sb = new StringBuilder();
        sb.append(Instances.ARFF_RELATION + "\tdata\n");
        sb.append(attributesToString(attributes));
        return sb.toString();
    }

    private static String generateWekaData(List<AttributeProprety> attributes, List<List<String>> examples, boolean normalize){
        StringBuilder sb = new StringBuilder();
        sb.append(Instances.ARFF_DATA + "\n");
        sb.append(examplesToString(attributes, examples, normalize));
        return sb.toString();
    }

    private static String datasetToString(List<AttributeProprety> attributes, List<List<String>> examples, boolean normalize) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateWekaHead(attributes));
        sb.append(generateWekaData(attributes, examples, normalize));
        return sb.toString();
    }

    private static String examplesToString(List<AttributeProprety> attributes, List<List<String>> examples, boolean normalize) {
        StringBuilder sb = new StringBuilder();
        Set<Integer> comments = new HashSet<>();
        Set<Integer> reals = new HashSet<>();
        int classIdx = 0;
        for (int idx = 0; idx < attributes.size(); idx++) {
            if (attributes.get(idx) instanceof ClassAttribute) {
                classIdx = idx;
                comments.add(idx);
            } else if (attributes.get(idx) instanceof CommentAttribute) {
                comments.add(idx);
            } else if (attributes.get(idx) instanceof RealAttribute) {
                reals.add(idx);
            }
        }

        final int finalClassIdx = classIdx;
        examples.forEach(example -> {
            // otherwise we do not know what to predict ;)
            if (!example.get(finalClassIdx).equals(UNKNOWN_VALUE)) {
                StringBuilder sample = new StringBuilder();
                IntStream
                        .range(0, example.size())
                        .filter(idx -> !comments.contains(idx))
                        .forEach(idx -> {
                            if (reals.contains(idx)) {
                                sample.append(formateNumber(example.get(idx), normalize, (RealAttribute) attributes.get(idx)) + ",");
                            } else {
                                sample.append(example.get(idx) + ",");
                            }
                        });
                sample.append(example.get(finalClassIdx));
                sb.append(sample.toString()+ "\n");
            }
        });

        return sb.toString();
    }

    private static Double formateNumber(String exampleValue, boolean normalize, RealAttribute real) {
        if (UNKNOWN_VALUE.equals(exampleValue)) {
            // should not be hardcoded, but based on MissingValueProcesor, and so on
            exampleValue = "0.5";//MissingValueKBANN.
        }
        Double value = Double.valueOf(exampleValue);
        if (normalize) {
            value = (value - real.getMin()) / (real.getMax() - real.getMin());
        }
        return value;
    }

    private static String attributesToString(List<AttributeProprety> attributes) {
        StringBuilder sb = new StringBuilder();
        attributes.forEach(attribute -> {
            if (attribute instanceof NominalAttribute) {
                sb.append(ATTRIBUTE_TOKEN + "\t" + attribute.getOrder() + "\t{" + valuesToString(attribute) + "}" + "\n");
            } else if (attribute instanceof NominalAttribute) {
                sb.append(ATTRIBUTE_TOKEN + "\t" + attribute.getOrder() + "\t" + REAL_ATTRIBUTE_TOKEN);
            }
        });
        attributes.forEach(attribute -> {
            if (attribute instanceof ClassAttribute) {
                sb.append(ATTRIBUTE_TOKEN + "\t" + attribute.getOrder() + "\t{" + valuesToString(attribute) + "}" + "\n");
            }
        });
        return sb.toString();
    }

    private static String valuesToString(AttributeProprety attribute) {
        System.out.println(attribute);
        // leaving out unkwnown value
        StringBuilder sb = new StringBuilder();
        Stream<String> stream = Stream.empty();
        if (attribute instanceof NominalAttribute) {
            stream = ((NominalAttribute) attribute).getValues().stream();
        } else if (attribute instanceof ClassAttribute) {
            stream = ((ClassAttribute) attribute).getValues().stream();
        }
        stream.filter(value -> !UNKNOWN_VALUE.equals(value))
                .forEach(value -> sb.append(value + ","));

        System.out.println(sb.toString());

        return sb.toString().substring(0, sb.length() - 1);
    }

    private static Dataset createNesislDataset(List<Pair<String, Set<String>>> ambigious, List<AttributeProprety> attributes, List<List<String>> examples, File file, boolean normalize) {

        System.out.println("Ouch, probably, while creating the weka-like dataset, output classes should be in order which is produced by JRip. Otherwise, it may give different results.");

        List<Fact> inputFacts = generateInputFacts(ambigious, attributes);
        List<Fact> outputFacts = generateOutputFacts(attributes); // for one class only (predicting values of one attribute only)
        List<Map<Fact, Value>> reformatedExamples = reformateData(inputFacts, outputFacts, examples, attributes, ambigious, normalize);
        ClassAttribute classAttribute = (ClassAttribute) attributes.stream()
                .filter(attribute -> attribute instanceof ClassAttribute).toArray()[0];
        return new DatasetImpl(Collections.unmodifiableList(inputFacts), Collections.unmodifiableList(outputFacts), reformatedExamples, file, classAttribute);
    }


    public static Triple<List<AttributeProprety>, List<List<String>>, List<Pair<String, Set<String>>>> parseDataset(File file, boolean normalize) {
        State state = State.ATTRIBUTES;
        //List<Fact> factsOrder = null;
        //List<Map<Fact, Value>> examples = new ArrayList<>();
        AttributePropertyFactory attributeFactory = new AttributePropertyFactory();
        List<AttributeProprety> attributes = new ArrayList<>();
        List<List<String>> examples = new ArrayList<>();
        List<Pair<String, Set<String>>> ambigious = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() < 1
                        || (line.trim().length() > 0 && line.trim().charAt(0) == COMMENTED_LINE_START)) {
                    continue;
                }
                switch (state) {
                    //case TRAIN_SET: parsing of train/test set not supported
                    case ATTRIBUTES:
                        if (DATA_TOKEN.equals(line.trim())) {
                            state = State.DATA;
                            break;
                        }
                        attributes = readAttribute(line, attributeFactory, attributes);
                        break;
                    case DATA:
                        if (AMBIGUOUS_TOKEN.equals(line.trim())) {
                            state = State.AMBIGUOUS;
                            break;
                        }
                        examples = readExample(line, attributes, examples);
                        break;
                    case AMBIGUOUS:
                        ambigious = readAmbiguous(line, ambigious);
                        break;
                    default:
                        System.out.println("Do not know how to parse '" + line + "'.");
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("TODO solve ambigious input (promoters)");
        return new Triple<>(attributes, examples, ambigious);
    }

    /**
     * Parses dataset, containing test and train data, from given file.
     *
     * @param file
     * @return
     */
    public static Dataset parseAndGetDataset(File file, boolean normalize) {
        return parseAndGetDatasets(file, normalize).getLeft();
    }

    private static List<Map<Fact, Value>> reformateData(List<Fact> inputFacts, List<Fact> outputFacts, List<List<String>> examples, List<AttributeProprety> attributes, List<Pair<String, Set<String>>> ambigious, boolean normalize) {
                return reformateDataWithMapping(inputFacts, outputFacts, examples, attributes, ambigious, normalize)
                        .getLeft();
    }

    private static Pair<List<Map<Fact, Value>>,Map<Map<Fact, Value>,List<String>>> reformateDataWithMapping(List<Fact> inputFacts, List<Fact> outputFacts, List<List<String>> examples, List<AttributeProprety> attributes, List<Pair<String, Set<String>>> ambigious, boolean normalize) {
        Map<String, Fact> inputs = new HashMap<>();
        Map<String, Fact> outputs = new HashMap<>();
        inputFacts.stream().forEach(fact -> inputs.put(fact.getFact(), fact));
        outputFacts.stream().forEach(fact -> outputs.put(fact.getFact(), fact));
        Map<Map<Fact, Value>, List<String>> mapping = new HashMap<>();
        List<Map<Fact,Value>> samples = new ArrayList<>();

        examples.stream()
                .forEach(example -> {
                    Map<Fact, Value> sample = reformateExample(inputs, outputs, example, attributes, ambigious, normalize);
                    samples.add(sample);
                    mapping.put(sample,example);
                });

        return new Pair<>(samples,mapping);
    }

    private static Map<Fact, Value> reformateExample(Map<String, Fact> inputs, Map<String, Fact> outputs, List<String> example, List<AttributeProprety> attributes, List<Pair<String, Set<String>>> ambiguous, boolean normalize) {
        Map<Fact, Value> sample = new HashMap<>();

        Set<String> ambiguousValues = ambiguous.stream().map(pair -> pair.getLeft()).collect(Collectors.toSet());
        Map<String, Set<String>> instead = new HashMap<>();
        ambiguous.stream().forEach(pair -> instead.put(pair.getLeft(), pair.getRight()));

        for (int idx = 0; idx < example.size(); idx++) {
            AttributeProprety attribute = attributes.get(idx);
            String exampleValue = example.get(idx);

            if (attribute instanceof ClassAttribute) {
                ClassAttribute target = (ClassAttribute) attribute;
                if (target.isBinary()) {
                    Fact fact = outputs.get(CLASS_TOKEN);
                    String value = (exampleValue.equals(target.getPositiveClass())) ? "1" : "0";
                    sample.put(fact, Value.create(value));
                } else {
                    final String finalExampleValue1 = exampleValue;
                    target.getValues().forEach(value -> {
                        sample.put(new Fact(CLASS_TOKEN + ATTRIBUTE_VALUE_DELIMITER + value), Value.create((value.equals(finalExampleValue1)) ? "1" : "0"));
                    });
                }

            } else if (attribute instanceof NominalAttribute) {
                NominalAttribute nominal = (NominalAttribute) attribute;
                if (ambiguousValues.contains(exampleValue)) {
                    final String finalExampleValue = exampleValue;
                    nominal.getValues()
                            .forEach(value ->
                                    sample.put(new Fact(nominal.getOrder() + ATTRIBUTE_VALUE_DELIMITER + value),
                                            Value.create((instead.get(finalExampleValue).contains(value)) ? Value.NAN_TOKEN : "0")));
                } else {
                    final String finalExampleValue2 = exampleValue;
                    nominal.getValues().forEach(value -> {
                        sample.put(new Fact(nominal.getOrder() + ATTRIBUTE_VALUE_DELIMITER + value), Value.create((value.equals(finalExampleValue2)) ? "1" : "0"));
                    });
                }

            } else {
                if (attribute instanceof RealAttribute) {
                    RealAttribute real = (RealAttribute) attribute;
                    Double value = formateNumber(exampleValue, normalize, real);
                    Fact fact = inputs.get(real.getOrder());
                    sample.put(fact, Value.create(value + ""));

                } else if (attribute instanceof CommentAttribute) {
                }
            }
        }
        return sample;
    }

    private static List<Fact> generateInputFacts(List<Pair<String, Set<String>>> ambigious, List<AttributeProprety> attributes) {
        List<Fact> facts = new ArrayList<>();
        int idx = 0;
        for (int idxWithComments = 0; idxWithComments < attributes.size(); idxWithComments++) {
            AttributeProprety attribute = attributes.get(idxWithComments);
            if (attribute instanceof CommentAttribute) {
                continue;
            } else if (attribute instanceof ClassAttribute) {
                idx++;
                continue;
            }

            if (attribute instanceof RealAttribute) {
                facts.add(new Fact(idx + ""));
                idx++;
            } else if (attribute instanceof NominalAttribute) {
                NominalAttribute nominal = (NominalAttribute) attributes.get(idxWithComments);
                Set<String> values = new HashSet<>(nominal.getValues());
                for (Pair<String, Set<String>> pair : ambigious) {
                    String head = pair.getLeft();
                    if (values.contains(head)) {
                        values.remove(head); // inserting missing values is done in reformating dataset
                    }
                }
                final int finalIdx = idx;
                values.forEach(value -> facts.add(new Fact(finalIdx + ATTRIBUTE_VALUE_DELIMITER + value)));
                idx++;
            }
        }
        return facts;
    }

    private static List<Fact> generateOutputFacts(List<AttributeProprety> attributes) {
        int idx = 0;
        for (int idxWithComments = 0; idxWithComments < attributes.size(); idxWithComments++) {
            AttributeProprety attribute = attributes.get(idxWithComments);
            if (attribute instanceof ClassAttribute) {
                idx = idxWithComments;
                break;
            } else if (attribute instanceof CommentAttribute) {

            } else {
                //idx++;
            }
        }
        if (!(attributes.get(idx) instanceof ClassAttribute)) {
            throw new IllegalStateException("No class attribute found!");
        }
        ClassAttribute target = (ClassAttribute) attributes.get(idx);
        List<Fact> facts = new ArrayList<>();

        if (target.isBinary()) {
            facts.add(new Fact(CLASS_TOKEN));
        } else {
            target.getValues().forEach(value -> facts.add(new Fact(CLASS_TOKEN + ATTRIBUTE_VALUE_DELIMITER + value)));
        }

        return facts;
    }

    private static List<Pair<String, Set<String>>> readAmbiguous(String line, List<Pair<String, Set<String>>> ambigious) {
        String[] splitted = line.split(AMBIGUOUS_DELIMITER);
        Set<String> instead = new HashSet<>();
        String head = splitted[0].trim();
        for (int idx = 1; idx < splitted.length; idx++) {
            instead.add(splitted[idx].trim());
        }
        Pair<String, Set<String>> pair = new Pair<>(head, instead);
        ambigious.add(pair);
        return ambigious;
    }

    private static List<List<String>> readExample(String line, List<AttributeProprety> attributes, List<List<String>> examples) {
        String[] splitted = line.split(DATA_DELIMITER);
        List<String> list = Arrays.stream(splitted).map(value -> value.trim()).collect(Collectors.toCollection(ArrayList::new));

        boolean missingClass = false;
        for (int idx = 0; idx < list.size(); idx++) {
            if (attributes.get(idx) instanceof ClassAttribute) {
                if (list.get(idx).equals("?")) {
                    missingClass = true;
                }
                break;
            }
        }
        // do not import trainSamples with missing class
        if (!missingClass) {
            IntStream.range(0, list.size()).forEach(idx -> attributes.get(idx).addValue(list.get(idx)));
            examples.add(list);
        }

        return examples;
    }

    private static List<AttributeProprety> readAttribute(String line, AttributePropertyFactory factory, List<AttributeProprety> attributes) {
        AttributeProprety property = factory.create(line);
        if (null != property) {
            attributes.add(property);
        }
        return attributes;
    }


    /* old version for parsing format of artificial domains
    public static Dataset parseDataset(File file) {
        State state = State.TRAIN_SET;
        List<Fact> factsOrder = null;
        List<Map<Fact, Value>> examples = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                switch (state) {
                    case TRAIN_SET:
                        if (!TRAIN_TOKEN.equals(line)) {
                            throw new IllegalStateException("Train set file must start with '" + TRAIN_TOKEN + "'.");
                        }
                        state = State.FACT_LINE;
                        break;
                    case FACT_LINE:
                        factsOrder = readFactLine(line);
                        state = State.EXAMPLES;
                        break;
                    case EXAMPLES:
                        examples = readAndAddExample(line, factsOrder, examples);
                        break;
                    default:
                        System.out.println("Do not know how to parse '" + line + "'.");
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //final List<Fact> finalFactsOrder = factsOrder;
        //Map<Fact, Integer> order = IntStream.range(0, factsOrder.size()).mapToObj(idx -> new Pair<>(idx, finalFactsOrder.get(idx))).filter(pair -> null != pair.getRight()).collect(Collectors.toMap(pair -> pair.getRight(), p -> p.getLeft()));
        Pair<List<Fact>, List<Fact>> factsOrderWrapper = selectInputAndOutputFacts(factsOrder);
        return new DatasetImpl(Collections.unmodifiableList(factsOrderWrapper.getLeft()), Collections.unmodifiableList(factsOrderWrapper.getRight()), examples, file);
    }
    */

    private static Pair<List<Fact>, List<Fact>> selectInputAndOutputFacts(List<Fact> factsOrder) {
        List<Fact> input = new ArrayList<>();
        List<Fact> output = new ArrayList<>();
        boolean inputFacts = true;
        for (Fact fact : factsOrder) {
            if (null == fact) {
                inputFacts = false;
            } else if (inputFacts) {
                input.add(fact);
            } else {
                output.add(fact);
            }
        }
        return new Pair<>(input, output);
    }

    private static List<Map<Fact, Value>> readAndAddExample(String line, List<Fact> factsOrder, List<Map<Fact, Value>> examples) {
        examples.add(readExample(line, factsOrder));
        return examples;
    }

    private static Map<Fact, Value> readExample(String line, List<Fact> factsOrder) {
        Map<Fact, Value> example = new HashMap<>();
        String[] splitted = line.split(FACT_DELIMITER);
        for (int idx = 0; idx < splitted.length; idx++) {
            Fact fact = factsOrder.get(idx);
            if (null != fact) {
                example.put(fact, Value.create(splitted[idx].trim()));
            }
        }
        return example;
    }

    private static List<Fact> readFactLine(String line) {
        List<Fact> list = new ArrayList<>();
        String[] splitted = line.split(FACT_DELIMITER);
        LiteralFactory factory = new LiteralFactory();
        for (int idx = 0; idx < splitted.length; idx++) {
            if (INPUT_OUTPUT_DELIMITER.equals(splitted[idx])) {
                list.add(null);
            } else {
                Literal literal = factory.getLiteral(splitted[idx]);
                if (!literal.isPositive()) {
                    throw new IllegalStateException("I do know how to parse negative literal in dataset.");
                }
                list.add(literal.getFact());
            }
        }
        return list;
    }

}
