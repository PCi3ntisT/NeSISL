package main.java.cz.cvut.ida.nesisl.modules.algorithms.structuralLearningWithSelectiveForgetting;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Results;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
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
public class StructuralLearningWithSelectiveForgetting {

    private NeuralNetwork network;

    public StructuralLearningWithSelectiveForgetting(NeuralNetwork network) {
        this.network = network;
    }


    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    public void learn(Dataset dataset, WeightLearningSetting wls, SLFSetting slfSetting) {

        double error = Double.MAX_VALUE;
        double eps = Double.MAX_VALUE;

        long iteration = 0;

        while (iteration < wls.getEpochLimit()) { // eps > wls.getEpsilonDifference() // or other stopping criterion
            for (Sample sample : dataset.getTrainData(network)) {
                Pair<List<Double>, Results> resultDiff = Tools.computeErrorResults(network, sample.getInput(), sample.getOutput());
                updateWeights(network, resultDiff.getLeft(), resultDiff.getRight(), wls, slfSetting);
            }
            double currentError = Tools.computeAverageSuqaredTotalError(network, dataset) + Tools.computePenalty(network, slfSetting.getPenaltyEpsilon(), slfSetting.getTreshold());
            eps = Math.abs(error - currentError);
            error = currentError;
            iteration++;

            System.out.println(iteration + "\t" + error + "\t" + eps);
            //System.out.println("\t" + Tools.computeAverageSuqaredTotalError(network, dataset));
            //System.out.println("\t" + Tools.computePenalty(network, slfSetting.getPenaltyEpsilon(), slfSetting.getThreshold()));
        }

    }

    public static void updateWeights(NeuralNetwork network, List<Double> differenceExampleMinusOutput, Results results, WeightLearningSetting wls, SLFSetting slfSetting) {
        Map<Node, Double> sigma = new HashMap<>();

        IntStream.range(0, differenceExampleMinusOutput.size()).forEach(idx -> {
            Node node = network.getOutputNodes().get(idx);
            Double value = node.getFirstDerivationAtX(results.getComputedOutputs().get(idx)) * differenceExampleMinusOutput.get(idx);
            sigma.put(node, value); // ten vypocet presunout do funkce, etc.
        });

        HashMap<Edge, Double> deltas = new HashMap<>();
        List<Node> nodeLayer;
        for (long layer = network.getMaximalNumberOfHiddenLayer() + 1; layer >= 0; layer--) {
            if (layer > network.getMaximalNumberOfHiddenLayer()) {
                nodeLayer = network.getOutputNodes();
            } else {
                nodeLayer = network.getHiddenNodesInLayer(layer);
            }
            nodeLayer.forEach(node ->
                            network.getIncomingForwardEdges(node).stream().filter(Edge::isModifiable).forEach(edge -> {
                                Double value = wls.getLearningRate() * sigma.get(node) * results.getComputedValues().get(edge.getSource());

                                Double edgeWeight = network.getWeight(edge);
                                Double edgeAbsoluteWeight = Math.abs(edgeWeight);
                                if (edgeAbsoluteWeight < slfSetting.getTreshold()) {
                                    Double penaltyTerm = wls.getLearningRate() * slfSetting.getPenaltyEpsilon() * Math.signum(edgeWeight);

                                    /*System.out.println(edge);
                                    System.out.println("\t" + edgeWeight);
                                    System.out.println("\t" + value);
                                    System.out.println("\t" + penaltyTerm);
                                    System.out.println("\t" + (value - penaltyTerm));
                                    System.out.println("f\t" + (edgeAbsoluteWeight + value - penaltyTerm));
                                    */

                                    value = value - penaltyTerm;
                                }

                                deltas.put(edge, value);
                            })
            );

            network.getHiddenNodesInLayer(layer - 1).forEach(node -> {
                Double sigmaSum = network.getOutgoingForwardEdges(node).stream().filter(Edge::isModifiable).mapToDouble(edge -> network.getWeight(edge) * sigma.get(edge.getTarget())).sum();
                sigma.put(node, sigmaSum * node.getFirstDerivationAtX(results.getComputedValues().get(node)));
            });
        }

        addDeltas(network, deltas);
    }

    private static NeuralNetwork addDeltas(NeuralNetwork network, Map<Edge, Double> deltas) {
        deltas.entrySet().forEach(entry -> {
            if (entry.getKey().isModifiable()) {
                Double value = network.getWeight(entry.getKey()) + entry.getValue();
                network.setEdgeWeight(entry.getKey(), value);
            }
        });
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
            }else if(network.getHiddenNodesInLayer(layer).isEmpty()){
                continue;
            }else {
                current.addAll(network.getHiddenNodesInLayer(layer));
            }

            Tools.makeFullInterLayerForwardConnections(previous, current, network, randomGenerator);

            current.add(network.getBias());
            previous = current;
        }

        return network;
    }
}
