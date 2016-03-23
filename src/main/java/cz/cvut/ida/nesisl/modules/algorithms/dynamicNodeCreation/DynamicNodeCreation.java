package main.java.cz.cvut.ida.nesisl.modules.algorithms.dynamicNodeCreation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.*;

/**
 * Created by EL on 13.3.2016.
 */
public class DynamicNodeCreation {
    private NeuralNetwork network;
    private RandomGenerator randomGenerator;

    public DynamicNodeCreation(List<Fact> inputFactOrder, List<Fact> outputFactOrder, RandomGenerator randomGenerator) {
        this.network = constructNetwork(inputFactOrder, outputFactOrder, new MissingValueKBANN(), randomGenerator);
        this.randomGenerator = randomGenerator;
    }

    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    public void learn(Dataset dataset, WeightLearningSetting wls, DNCSetting dncSetting) {
        double error = Double.MAX_VALUE;
        double eps = Double.MAX_VALUE;
        Map<Long, Double> averagesErrors = new HashMap<>();
        long iteration = 0;
        long timeOfAddingLastNode = 0;

        Map<Sample, Map<Edge, Double>> previousDeltas = Backpropagation.initPreviousDeltas(dataset, network);

        while (true) { // or other stopping criterion
            for (Sample sample : dataset.getTrainData(network)) {
                Pair<List<Double>, Results> resultDiff = Tools.computeErrorResults(network, sample.getInput(), sample.getOutput());
                Backpropagation.updateWeights(network, resultDiff.getLeft(), resultDiff.getRight(), wls, 2, previousDeltas.get(sample));
            }
            double currentError = Tools.computeAverageSuqaredTotalError(network, dataset);
            eps = Math.abs(error - currentError);
            error = currentError;

            averagesErrors.put(iteration, currentError);

            double maxError = Tools.maxSquaredError(network, dataset);

            if (canStopNodeGrowth(iteration, averagesErrors, maxError, dncSetting) || dncSetting.getHiddenNodesLimit() < network.getNumberOfHiddenNodes()) {
                break;
            }

            if (addNewNode(averagesErrors, iteration, timeOfAddingLastNode, dncSetting)) {
                addAnotherNode(network);
                timeOfAddingLastNode = iteration;
            }

            iteration++;
            System.out.println(iteration + "\t" + error + "\t" + eps);
        }
    }

    private boolean canStopNodeGrowth(long time, Map<Long, Double> averagesErrors, double maxError, DNCSetting dncSetting) {
        double aT = averagesErrors.get(time);
        /*System.out.println("stop?");
        System.out.println("\t" + aT +" < " + dncSetting.getCa());
        System.out.println("\t" + maxError + " < " + dncSetting.getCm());
        */
        return aT < dncSetting.getCa() && maxError < dncSetting.getCm();
    }

    private boolean addNewNode(Map<Long, Double> averagesErrors, long time, long timeOfAddingLastNode, DNCSetting dncSetting) {
        if (timeOfAddingLastNode > time - dncSetting.getTimeWindow()) {
            return false;
        }

        Double averT = averagesErrors.get(time);
        Double averTMinusWindow = averagesErrors.get(time - dncSetting.getTimeWindow());
        Double averAtAddedNode = averagesErrors.get(timeOfAddingLastNode);

        /*System.out.println("add?");
        System.out.println("\t" + (Math.abs(averT - averTMinusWindow) / averAtAddedNode) + "\t" +  dncSetting.getDeltaT());
        */
        return Math.abs(averT - averTMinusWindow) / averAtAddedNode < dncSetting.getDeltaT();
    }

    private void addAnotherNode(NeuralNetwork network) {
        Node node = NodeFactory.create(Sigmoid.getFunction());
        network.addNodeAtLayerStateful(node, 0);
        List<Node> inputs = new ArrayList<>();
        inputs.add(network.getBias());
        inputs.addAll(network.getInputNodes());
        inputs.forEach(source ->
                        network.addEdgeStateful(source, node, randomGenerator.nextDouble(), Edge.Type.FORWARD)
        );


        List<Node> outputs = network.getOutputNodes();
        outputs.forEach(target ->
                        network.addEdgeStateful(node, target, randomGenerator.nextDouble(), Edge.Type.FORWARD)
        );
    }

    public static NeuralNetwork constructNetwork(List<Fact> inputFactOrder, List<Fact> outputFactOrder, MissingValues missingValues, RandomGenerator randomGenerator) {
        List<Node> inputs = NodeFactory.generateNodes(inputFactOrder, Identity.getFunction());
        List<Node> output = NodeFactory.generateNodes(outputFactOrder, Sigmoid.getFunction());
        NeuralNetwork network = new NeuralNetworkImpl(inputs, output, missingValues);

        Node hiddenNode = NodeFactory.create(Sigmoid.getFunction());
        network.addNodeAtLayerStateful(hiddenNode, 0);

        List<Node> list = new LinkedList<>();
        list.add(hiddenNode);

        inputs.add(network.getBias());
        Tools.makeFullInterLayerForwardConnections(inputs, list, network, randomGenerator);

        List<Node> inter = new ArrayList<>();
        inter.add(hiddenNode);
        inter.add(network.getBias());
        Tools.makeFullInterLayerForwardConnections(inter, output, network, randomGenerator);

        return network;
    }
}
