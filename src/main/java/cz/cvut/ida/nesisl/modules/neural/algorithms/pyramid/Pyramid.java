package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.pyramid;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.neural.neuralNetwork.activationFunctions.SoftMax;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by EL on 23.1.2017.
 */
public class Pyramid implements NeuralNetworkOwner {

    private NeuralNetwork network;
    private final int numberOfHiddenLayers;
    private final RandomGeneratorImpl randomGenerator;

    public Pyramid(NeuralNetwork network, RandomGeneratorImpl randomGenerator, int numberOfHiddenLayers) {
        this.randomGenerator = randomGenerator;
        this.network = network;
        this.numberOfHiddenLayers = numberOfHiddenLayers;
    }

    public NeuralNetwork learn(Dataset dataset, WeightLearningSetting weightLearningSetting) {
        this.network = Backpropagation.feedforwardBackpropagation(this.network, dataset, weightLearningSetting);
        this.network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        return this.network;
    }

    public static NeuralNetwork constructNetwork(List<Fact> inputFactOrder, List<Fact> outputFactOrder, MissingValues missingValues, RandomGenerator randomGenerator, boolean softmaxOutputs, int numberOfHiddenLayers) {
        List<Node> inputs = NodeFactory.generateNodes(inputFactOrder, Identity.getFunction());
        // automatic creation of softmax when multiclass classification
        ActivationFunction outputFce = (softmaxOutputs) ? SoftMax.getFunction() : Sigmoid.getFunction();
        List<Node> output = NodeFactory.generateNodes(outputFactOrder, outputFce);
        NeuralNetwork network = new NeuralNetworkImpl(inputs, output, missingValues,softmaxOutputs);

        network = makePyramidStructure(inputs, output, network,numberOfHiddenLayers);
        network = makeFullyConnectWithBiases(network, randomGenerator);

        return network;
    }

    // hups, it works statefuly :(
    private static NeuralNetwork makeFullyConnectWithBiases(NeuralNetwork network, RandomGenerator random) {
        List<Node> previousLayer = new ArrayList(network.getInputNodes());
        List<Node> currentLayer = null;
        for (int layerIndex = 0; layerIndex <= network.getMaximalNumberOfHiddenLayer() + 1; layerIndex++) {
            if (layerIndex > network.getMaximalNumberOfHiddenLayer()) {
                currentLayer = new ArrayList<>(network.getOutputNodes());
            } else if (network.getHiddenNodesInLayer(layerIndex).size() < 1) {
                continue;
            } else {
                currentLayer = new ArrayList<>(network.getHiddenNodesInLayer(layerIndex));
            }
            previousLayer.add(network.getBias());
            final List<Node> finalPreviousLayer = previousLayer;
            currentLayer.stream()
                    .filter(Node::isModifiable)
                    .forEach(currentNode ->
                                    finalPreviousLayer.stream()
                                            .forEach(previousNode ->
                                                            network.addEdgeStateful(previousNode, currentNode, random.nextDouble(), Edge.Type.FORWARD)
                                            )
                    );
            previousLayer = currentLayer;
        }

        return network;
    }

    private static NeuralNetwork makePyramidStructure(List<Node> inputs, List<Node> output, NeuralNetwork network,Integer numberOfHiddenLayers) {
        System.out.println("using " + numberOfHiddenLayers + " hidden layers");

        long numberOfInputs = inputs.size();
        long numberOfOutputs = output.size();
        double r = Math.pow((numberOfInputs * 1.0 / numberOfOutputs),0.25);
        IntStream.range(0, numberOfHiddenLayers)
                .forEach(layerIdx -> {
                    List<Node> nodes = IntStream.range(0, (int) (numberOfOutputs * Math.pow(r, numberOfHiddenLayers - layerIdx)))
                            .mapToObj(idx -> NodeFactory.create(Sigmoid.getFunction()))
                            .collect(Collectors.toCollection(ArrayList::new));
                    network.addNodesAtLayerStateful(nodes, layerIdx);
                });
        return network;
    }

    public static Pyramid  create(List<Fact> inputFactOrder, List<Fact> outputFactOrder, RandomGeneratorImpl randomGenerator, MissingValueKBANN missingValue, boolean softmaxOutputs, int numberOfHiddenLayers) {
        NeuralNetwork network = constructNetwork(inputFactOrder, outputFactOrder, missingValue, randomGenerator,softmaxOutputs,numberOfHiddenLayers);
        return new Pyramid(network, randomGenerator,numberOfHiddenLayers);
    }

    @Override
    public NeuralNetwork getNeuralNetwork() {
        return network;
    }
}
