package main.java.cz.cvut.ida.nesisl.modules.dataset;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.logic.Literal;
import main.java.cz.cvut.ida.nesisl.api.logic.LiteralFactory;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.MissingValues;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by EL on 13.2.2016.
 */
public class DatasetImpl implements Dataset {

    public static final String FACT_DELIMITER = "\\s+";
    public static final String INPUT_OUTPUT_DELIMITER = "|";
    public static final String TRAIN_TOKEN = "TRAIN SET";


    private final File originalFile;
    private final List<Map<Fact, Value>> samples;
    private final List<Fact> inputFacts;
    private final List<Fact> outputFacts;

    private List<Fact> cachedInputOrder;
    private List<Fact> cachedOutputOrder;
    private List<Sample> cachedSamples;
    private List<Double> cachedOutputAverage;

    public DatasetImpl(List<Fact> inputFacts, List<Fact> outputFacts, List<Map<Fact, Value>> samples, File file) {
        this.inputFacts = inputFacts;
        this.outputFacts = outputFacts;
        this.samples = samples;
        this.originalFile = file;
    }

    @Override
    public List<Sample> getTrainData(NeuralNetwork network) {
        return getTrainData(network.getInputFactOrder(), network.getOutputFactOrder(), network);
    }

    @Override
    public List<Fact> getInputFactOrder() {
        return inputFacts;
    }

    @Override
    public List<Fact> getOutputFactOrder() {
        return outputFacts;
    }

    @Override
    public List<Double> getAverageOutputs(NeuralNetwork network) {
        synchronized (this) {
            if (null == cachedOutputAverage || !areDataCached(network)) {
                List<List<Value>> outputs = getTrainData(network).parallelStream().map(sample -> sample.getOutput()).collect(Collectors.toCollection(ArrayList::new));
                cachedOutputAverage = Tools.computeAverages(outputs);
            }
            return cachedOutputAverage;
        }
    }

    @Override
    public File getOriginalFile() {
        return this.originalFile;
    }

    @Override
    public List<Sample> getNodeTrainData(NeuralNetwork network) {
        return getTrainData(network); // TODO  v pripade ze pustim topGen s dvema rozdelenyma trainSetama, tak normalne se train data jako jedna cast at to nemusim predelatvat :)
    }

    @Override
    public List<Sample> getTestData(NeuralNetwork network) {
        // tohle jeste predelat ;)
        return getTrainData(network);
    }

    private boolean areDataCached(NeuralNetwork network) {
        return areDataCached(network.getInputFactOrder(), getInputFactOrder());
    }

    private List<Sample> getTrainData(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, NeuralNetwork network) {
        synchronized (this) {
            if (areDataCached(inputFactPermutation, outputFactPermutation)) {
                return this.cachedSamples;
            }
            this.cachedSamples = permuteSamples(inputFactPermutation, outputFactPermutation, samples, network);
            return this.cachedSamples;
        }
    }

    private List<Sample> permuteSamples(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, List<Map<Fact, Value>> cachedSamples, NeuralNetwork network) {
        List<Sample> samples = cachedSamples.parallelStream().map(map -> permuteSample(map, inputFactPermutation, outputFactPermutation, network.getMissingValuesProcessor())).collect(Collectors.toCollection(ArrayList::new));
        this.cachedInputOrder = Collections.unmodifiableList(inputFactPermutation);
        this.cachedOutputOrder = Collections.unmodifiableList(outputFactPermutation);
        return samples;
    }

    private static Sample permuteSample(Map<Fact, Value> sample, List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, MissingValues missingValuesProcessor) {
        List<Value> inputList = permutateSamplePart(sample, inputFactPermutation, missingValuesProcessor);
        List<Value> outputList = permutateSamplePart(sample, outputFactPermutation, missingValuesProcessor);
        return new SampleImpl(inputList, outputList);
    }

    private static List<Value> permutateSamplePart(Map<Fact, Value> sample, List<Fact> inputFactPermutation, MissingValues missingValuesProcessor) {
        return inputFactPermutation.stream().map(fact -> missingValuesProcessor.processMissingValueToValue(sample.get(fact))).collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean areDataCached(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation) {
        synchronized (this) {
            if (null == cachedInputOrder || null == cachedOutputOrder || !inputFactPermutation.equals(cachedInputOrder) || !outputFactPermutation.equals(cachedOutputOrder)) {
                return false;
            }
            return true;
        }
    }


    enum State {
        TRAIN_SET,
        FACT_LINE,
        EXAMPLES
    }


    public static Dataset createDataset(File file) {
        // naparsovat a rozradit dataset
        throw new NotImplementedException();
    }

    public static Dataset parseDataset(String pathToFile) {
        return parseDataset(new File(pathToFile));
    }


    /**
     * Parses dataset, containing test and train data, from given file.
     *
     * @param file
     * @return
     */
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
