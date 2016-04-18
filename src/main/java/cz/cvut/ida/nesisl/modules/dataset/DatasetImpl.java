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
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.tool.Triple;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by EL on 13.2.2016.
 */
public class DatasetImpl implements Dataset {

    public static final String FACT_DELIMITER = "\\s+";
    public static final String INPUT_OUTPUT_DELIMITER = "|";
    public static final String TRAIN_TOKEN = "TRAIN SET";


    private final File originalFile;
    private final List<Map<Fact, Value>> samples;
    private final List<Map<Fact, Value>> nodeTrainSamples;
    private final List<Fact> inputFacts;
    private final List<Fact> outputFacts;

    private List<Fact> cachedInputOrder;
    private List<Fact> cachedOutputOrder;
    private List<Sample> cachedSamples;
    private List<Double> cachedOutputAverage;

    private List<Fact> cachedTrainNodeInputOrder;
    private List<Fact> cachedTrainNodeOutputOrder;
    private List<Sample> cachedTrainNode;
    private List<Double> cachedTrainNodeOutputAverage;

    public DatasetImpl(List<Fact> inputFacts, List<Fact> outputFacts, List<Map<Fact, Value>> samples, File originalFile) {
        this.inputFacts = new ArrayList<>(inputFacts);
        this.outputFacts = new ArrayList<>(outputFacts);
        this.samples = new ArrayList<>(samples);
        this.originalFile = originalFile;
        this.nodeTrainSamples = new ArrayList<>(samples);
    }

    public DatasetImpl(List<Fact> inputFactOrder, List<Fact> outputFactOrder, List<Map<Fact, Value>> trainData, List<Map<Fact, Value>> nodeTrainData,File originalFile) {
        this.inputFacts = new ArrayList<>(inputFactOrder);
        this.outputFacts = new ArrayList<>(outputFactOrder);
        this.samples = new ArrayList<>(trainData);
        this.originalFile = originalFile;
        this.nodeTrainSamples = new ArrayList<>(nodeTrainData);
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
            return  cachedTrainNodeOutputAverage;
        }
    }

    public List<Double> getAverageOutputs(NeuralNetwork network,List<Double> cachedAverage,List<Sample> data,List<Fact> cachedInput, List<Fact> cachedOutput) {
        synchronized (this) {
            List<Double> result = cachedAverage;
            if (null == cachedOutput || !areDataCached(network,cachedInput,cachedOutput)) {
                List<List<Value>> outputs = data.parallelStream().map(sample -> sample.getOutput()).collect(Collectors.toCollection(ArrayList::new));
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
    public List<Map<Fact, Value>> getRawData() {
        return new ArrayList<>(samples);
    }

    @Override
    public String cannonicalOutput(Map<Fact, Value> sample) {
        synchronized (outputFacts){
            return IntStream.range(0,outputFacts.size())
                    .mapToObj(idx -> sample.get(outputFacts.get(idx)))
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();
        }
    }

    @Override
    public List<Sample> getTestData(NeuralNetwork network) {
        return getNodeTrainData(network.getInputFactOrder(), network.getOutputFactOrder(), network);
    }

    private boolean areDataCached(NeuralNetwork network,List<Fact> cachedInputFactOrder,List<Fact> cachedOutputFactOrder) {
        return areDataCached(network.getInputFactOrder(), getInputFactOrder(), cachedInputFactOrder, cachedOutputFactOrder);
    }

    private List<Sample> getTrainData(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, NeuralNetwork network) {
        synchronized (this) {
            if (areDataCached(inputFactPermutation, outputFactPermutation, cachedInputOrder,cachedOutputOrder)) {
                return this.cachedSamples;
            }
            Triple<List<Sample>,List<Fact>,List<Fact>> permuted = permuteSamples(inputFactPermutation, outputFactPermutation, samples, network);
            this.cachedSamples = permuted.getK();
            this.cachedInputOrder = permuted.getT();
            this.cachedOutputOrder = permuted.getW();
            return this.cachedSamples;
        }
    }

    private List<Sample> getNodeTrainData(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, NeuralNetwork network) {
        synchronized (this){
            if (areDataCached(inputFactPermutation, outputFactPermutation,cachedTrainNodeInputOrder,cachedTrainNodeOutputOrder)) {
                return this.cachedTrainNode;
            }
            Triple<List<Sample>,List<Fact>,List<Fact>> permuted = permuteSamples(inputFactPermutation, outputFactPermutation, nodeTrainSamples, network);
            this.cachedTrainNode = permuted.getK();
            this.cachedTrainNodeInputOrder = permuted.getT();
            this.cachedTrainNodeOutputOrder = permuted.getW();
            return this.cachedTrainNode;
        }
    }

    private Triple<List<Sample>,List<Fact>,List<Fact>> permuteSamples(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, List<Map<Fact, Value>> cachedSamples, NeuralNetwork network) {
        List<Sample> samples = cachedSamples.parallelStream().map(map -> permuteSample(map, inputFactPermutation, outputFactPermutation, network.getMissingValuesProcessor())).collect(Collectors.toCollection(ArrayList::new));
        return new Triple<>(samples,Collections.unmodifiableList(inputFactPermutation),Collections.unmodifiableList(outputFactPermutation));
    }

    private static Sample permuteSample(Map<Fact, Value> sample, List<Fact> inputFactPermutation, List<Fact> outputFactPermutation, MissingValues missingValuesProcessor) {
        List<Value> inputList = permuteSamplePart(sample, inputFactPermutation, missingValuesProcessor);
        List<Value> outputList = permuteSamplePart(sample, outputFactPermutation, missingValuesProcessor);
        return new SampleImpl(inputList, outputList);
    }

    private static List<Value> permuteSamplePart(Map<Fact, Value> sample, List<Fact> inputFactPermutation, MissingValues missingValuesProcessor) {
        return inputFactPermutation.stream().map(fact -> missingValuesProcessor.processMissingValueToValue(sample.get(fact))).collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean areDataCached(List<Fact> inputFactPermutation, List<Fact> outputFactPermutation,List<Fact> cachedInputOrder,List<Fact> cachedOutputOrder) {
        synchronized (this) {
            if (null == cachedInputOrder || null == cachedOutputOrder || !inputFactPermutation.equals(cachedInputOrder) || !outputFactPermutation.equals(cachedOutputOrder)) {
                return false;
            }
            return true;
        }
    }

    public static Dataset stratifiedSplit(Dataset dataset, RandomGenerator randomGenerator, int numberOfFolds) {
        synchronized (dataset) {
            List<Map<Fact, Value>> trainData = new ArrayList<>();
            List<Map<Fact, Value>> nodeTrainData = new ArrayList<>();
            List<List<Map<Fact, Value>>> splitted = splitAccordingToResults(dataset);
            splitted.forEach(group -> {
                if(!group.isEmpty()) {
                    Collections.shuffle(group, randomGenerator.getRandom());
                    int border = group.size() / numberOfFolds;
                    if(2*border != group.size()
                            && group.size() > 2
                            && randomGenerator.nextDouble() < 0.5){
                        border++;
                    }
                    border = Math.min(border,group.size());
                    border = Math.max(border,0);
                    trainData.addAll(group.subList(0, border));
                    nodeTrainData.addAll(group.subList(border,group.size()));
                }
            });
            return new DatasetImpl(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), trainData, nodeTrainData, dataset.getOriginalFile());
        }
    }


    public static Dataset stratifiedSplitHalfToHalf(Dataset dataset, RandomGenerator randomGenerator) {
        return stratifiedSplit(dataset,randomGenerator,2);
    }

    private static List<List<Map<Fact, Value>>> splitAccordingToResults(Dataset dataset) {
        synchronized (dataset) {
            return dataset
                    .getRawData()
                    .stream()
                    .collect(Collectors.groupingBy(e -> dataset.cannonicalOutput(e), Collectors.toCollection(ArrayList::new)))
                    .values()
                    .stream()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    enum State {
        TRAIN_SET,
        FACT_LINE,
        EXAMPLES
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
