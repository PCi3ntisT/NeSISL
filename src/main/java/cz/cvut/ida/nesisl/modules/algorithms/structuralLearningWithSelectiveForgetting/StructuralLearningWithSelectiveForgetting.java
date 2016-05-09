package main.java.cz.cvut.ida.nesisl.modules.algorithms.structuralLearningWithSelectiveForgetting;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by EL on 14.3.2016.
 */
public class StructuralLearningWithSelectiveForgetting implements NeuralNetworkOwner {

    private NeuralNetwork network;

    public StructuralLearningWithSelectiveForgetting(NeuralNetwork network) {
        this.network = network;
    }


    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    public NeuralNetwork learn(Dataset dataset, WeightLearningSetting wls) {
        Backpropagation.feedforwardBackpropagationStateful(network, dataset, wls);
        return network;
    }

    public static NeuralNetwork createInitNetwork(File file, Dataset dataset, RandomGeneratorImpl randomGenerator) {
        List<Node> inputs = NodeFactory.generateNodes(dataset.getInputFactOrder(), Identity.getFunction());
        List<Node> outputs = NodeFactory.generateNodes(dataset.getOutputFactOrder(), Sigmoid.getFunction());
        NeuralNetwork network = new NeuralNetworkImpl(inputs, outputs, new MissingValueKBANN());

        String content = null;
        try {
            content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringTokenizer st = new StringTokenizer(content);
        int layer = 0;
        while (st.hasMoreTokens()) {
            int size = Integer.valueOf(st.nextToken());
            List<Node> nodes = Tools.generateNodes(size, Sigmoid.getFunction());
            network.addNodesAtLayerStateful(nodes, layer);
            layer++;
        }

        List<Node> previous = new ArrayList<>();
        previous.addAll(network.getInputNodes());
        previous.add(network.getBias());
        List<Node> current = null;
        for (layer = 0; layer <= network.getMaximalNumberOfHiddenLayer() + 1; layer++) {
            current = new ArrayList<>();
            if (layer > network.getMaximalNumberOfHiddenLayer()) {
                current.addAll(network.getOutputNodes());
            } else if (network.getHiddenNodesInLayer(layer).isEmpty()) {
                continue;
            } else {
                current.addAll(network.getHiddenNodesInLayer(layer));
            }

            Tools.makeFullInterLayerForwardConnections(previous, current, network, randomGenerator);

            current.add(network.getBias());
            previous = current;
        }
        network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        return network;
    }

    public static StructuralLearningWithSelectiveForgetting create(File initialStructure, Dataset dataset, RandomGeneratorImpl randomGenerator) {
        return new StructuralLearningWithSelectiveForgetting(StructuralLearningWithSelectiveForgetting.createInitNetwork(initialStructure, dataset, randomGenerator));
    }
}
