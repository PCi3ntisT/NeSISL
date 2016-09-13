package main.java.cz.cvut.ida.nesisl.modules.algorithms.dynamicNodeCreation;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.*;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.tresholdClassificator.ThresholdClassificator;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Identity;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.SoftMax;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.util.*;

/**
 * Created by EL on 13.3.2016.
 */
public class DynamicNodeCreation implements NeuralNetworkOwner {
    private NeuralNetwork network;
    private RandomGenerator randomGenerator;

    public DynamicNodeCreation(NeuralNetwork network, RandomGenerator randomGenerator) {
        this.network = network;
        this.randomGenerator = randomGenerator;
    }

    public NeuralNetwork getNeuralNetwork() {
        return network;
    }

    public NeuralNetwork learn(Dataset dataset, WeightLearningSetting wls, DNCSetting dncSetting) {
        Map<Long, Double> averagesErrors = new HashMap<>();
        long iteration = 0;
        long timeOfAddingLastNode = 0;

        Map<Sample, Map<Edge, Double>> previousDeltas = Backpropagation.initPreviousDeltas(dataset, network);

        List<Double> errors = new ArrayList<>();

        while (true) {
            for (Sample sample : dataset.getTrainData(network)) {
                Pair<List<Double>, Results> resultDiff = Tools.computeErrorResults(network, sample.getInput(), sample.getOutput());
                Backpropagation.updateWeights(network, resultDiff.getLeft(), resultDiff.getRight(), wls, 2, previousDeltas.get(sample));
            }
            double currentError = Tools.computeAverageSquaredTrainTotalErrorPlusEdgePenalty(network, dataset, wls);

            //System.out.println(iteration  + "\t" + currentError);

            averagesErrors.put(iteration, currentError);
            errors.add(currentError);

            double maxError = Tools.maxSquaredError(network, dataset);

            if (canStopNodeGrowth(iteration, averagesErrors, maxError, dncSetting)
                    || (errors.size() > 0 && errors.get(errors.size()-1) < Tools.convergedError())){
                break;
            }

            if (canAddNewNode(averagesErrors, iteration, timeOfAddingLastNode, dncSetting, network.getNumberOfHiddenNodes())) {
                addAnotherNode(network);
                timeOfAddingLastNode = iteration;
                System.out.println("adding node\t" + iteration + "\t" + network.getNumberOfHiddenNodes());
            }

            iteration++;

            if(network.getNumberOfHiddenNodes() == dncSetting.getHiddenNodesLimit()){
                if(!wls.canContinueBackpropagation(iteration, errors)){
                    break;
                }
            }

        }
        this.network.setClassifierStateful(ThresholdClassificator.create(network, dataset));
        return network;
    }

    private boolean canStopNodeGrowth(long time, Map<Long, Double> averagesErrors, double maxError, DNCSetting dncSetting) {
        double aT = averagesErrors.get(time);
        return aT < dncSetting.getCa() && maxError < dncSetting.getCm();
    }

    private boolean canAddNewNode(Map<Long, Double> averagesErrors, long time, long timeOfAddingLastNode, DNCSetting dncSetting, long numberOfHiddenNodes) {
        if (timeOfAddingLastNode > time - dncSetting.getTimeWindow()
                || network.getNumberOfHiddenNodes() >= dncSetting.getHiddenNodesLimit()) {
            return false;
        }

        Double averT = averagesErrors.get(time);
        Double averTMinusWindow = averagesErrors.get(time - dncSetting.getTimeWindow());
        Double averAtAddedNode = averagesErrors.get(timeOfAddingLastNode);

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

    public static NeuralNetwork constructNetwork(List<Fact> inputFactOrder, List<Fact> outputFactOrder, MissingValues missingValues, RandomGenerator randomGenerator,boolean softmaxOutputs) {
        List<Node> inputs = NodeFactory.generateNodes(inputFactOrder, Identity.getFunction());
        // automatci creation of softmax when multiclass classification
        ActivationFunction outputFce = (softmaxOutputs) ? SoftMax.getFunction() : Sigmoid.getFunction();
        List<Node> output = NodeFactory.generateNodes(outputFactOrder, outputFce);
        NeuralNetwork network = new NeuralNetworkImpl(inputs, output, missingValues,softmaxOutputs);

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

    public static DynamicNodeCreation create(List<Fact> inputFactOrder, List<Fact> outputFactOrder, RandomGeneratorImpl randomGenerator, MissingValueKBANN missingValue,boolean softmaxOutputs) {
        NeuralNetwork network = constructNetwork(inputFactOrder, outputFactOrder, missingValue, randomGenerator,softmaxOutputs);
        return new DynamicNodeCreation(network, randomGenerator);
    }
}
