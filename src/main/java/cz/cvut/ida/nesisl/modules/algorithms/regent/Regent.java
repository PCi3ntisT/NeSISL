package main.java.cz.cvut.ida.nesisl.modules.algorithms.regent;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.Backpropagation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGen;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Created by EL on 23.3.2016.
 */
public class Regent {
    private final KBANN kbann;
    private final RandomGeneratorImpl randomGenerator;
    private NeuralNetwork neuralNetwork;
    private NeuralNetwork bestSoFarNetwork;
    private Double bestSoFarScore = Double.MAX_VALUE;

    public Regent(KBANN kbann, RandomGeneratorImpl randomGenerator) {
        this.randomGenerator = randomGenerator;
        this.kbann = kbann;
        this.neuralNetwork = kbann.getNeuralNetwork();
    }

    public NeuralNetwork getNeuralNetwork() {
        return bestSoFarNetwork;
    }

    private void updateBestSoFar(NeuralNetwork network, Double error) {
        if (error < bestSoFarScore) {
            bestSoFarScore = error;
            this.bestSoFarNetwork = network;
            this.neuralNetwork = network;
        }
    }

    public static Regent create(File file, List<Pair<Integer, ActivationFunction>> specific, RandomGeneratorImpl randomGenerator, Double omega) {
        KBANN kbann = new KBANN(file, specific, randomGenerator, omega);
        return new Regent(kbann, randomGenerator);
    }

    public void learn(Dataset dataset, WeightLearningSetting wls, RegentSetting regentSetting, KBANNSettings kbannSetting) {
        this.kbann.learn(dataset, wls);
        this.neuralNetwork = kbann.getNeuralNetwork();
        this.bestSoFarScore = Tools.computeAverageSuqaredTotalError(neuralNetwork, dataset);

        List<NeuralNetwork> children = mutateInitialNetworkToMakeChildrens(this.neuralNetwork, dataset, regentSetting);
        List<Individual> population = computeFitness(children, dataset);

        Comparator<Individual> comparator = (p1, p2) -> {
            if (Math.abs(p1.getFitness() - p2.getFitness()) < 0.000000001) {
                return Long.compare(p1.getNeuralNetwork().getNumberOfHiddenNodes(), p2.getNeuralNetwork().getNumberOfHiddenNodes());
            }
            return Double.compare(p1.getFitness(), p2.getFitness());
        };

        while(regentSetting.getMaxFitness() > regentSetting.computedFitness()){
            List<Pair<NeuralNetwork, NeuralNetwork>> selectedForCrossover = tournamentSelectionForCrossover(population, regentSetting);
            List<NeuralNetwork> crossovered = crossover(selectedForCrossover, regentSetting);
            List<NeuralNetwork> mutation = new ArrayList<>();
            addToMutationFromCrossovers(mutation, crossovered, regentSetting);
            addToMutationFromPopulation(mutation, population, regentSetting);
            List<NeuralNetwork> mutated = mutateNetwork(mutation, dataset, regentSetting);

            List<Individual> successors = new ArrayList<>();
            addEvaluatedElites(population, successors, regentSetting, dataset, wls);
            addEvaluatedSuccessors(mutated, successors, regentSetting, dataset, wls);
            addEvaluatedSuccessors(crossovered, successors, regentSetting, dataset, wls);

            Collections.sort(population, comparator);
            updateBestSoFar(population.get(0));
        }
    }

    private List<NeuralNetwork> crossover(List<Pair<NeuralNetwork,NeuralNetwork>> pairs, RegentSetting regentSetting) {
        return pairs.parallelStream().map(pair -> generatedSuccesor(pair.getLeft(),pair.getRight(), regentSetting)).flatMap(l -> l.stream()).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<NeuralNetwork> generatedSuccesor(NeuralNetwork first, NeuralNetwork second, RegentSetting regentSetting) {
        // rozhodit na set A, B potom zacit skladat znova ty dva potomky a upravit biasy u uzlu (AND,OR)

        throw new NotImplementedException(); // todo
    }

    private List<Pair<NeuralNetwork, NeuralNetwork>> tournamentSelectionForCrossover(List<Individual> population, RegentSetting regentSetting) {
        return tournamentSelection(population, regentSetting, regentSetting.getNumberOfCrossoverChildren());
    }

    private void addEvaluatedSuccessors(List<NeuralNetwork> parents, List<Individual> successors, RegentSetting regentSetting, Dataset dataset, WeightLearningSetting wls) {
        List<Individual> evaluated = parents.parallelStream().map(network -> evaluate(network, regentSetting, dataset, wls)).collect(Collectors.toList());
        successors.addAll(evaluated);
    }

    private void addEvaluatedElites(List<Individual> population, List<Individual> successors, RegentSetting regentSetting, Dataset dataset, WeightLearningSetting wls) {
        List<Individual> selected = population.subList(0, regentSetting.getNumberOfElites());
        List<Individual> evaluated = selected.parallelStream().map(individual -> individual.getNeuralNetwork()).parallel().map(network -> evaluate(network, regentSetting, dataset, wls)).collect(Collectors.toList());
        successors.addAll(evaluated);
    }

    private Individual evaluate(NeuralNetwork network, RegentSetting regentSetting, Dataset dataset, WeightLearningSetting wls) {
        regentSetting.increaseFitnessCountSynchronized();
        Backpropagation.feedforwardBackpropagationStateful(network, dataset, wls);
        double error = Tools.computeAverageSuqaredTotalError(network, dataset);
        return new Individual(network, error);
    }

    private void addToMutationFromPopulation(List<NeuralNetwork> mutation, List<Individual> population, RegentSetting regentSetting) {
        for (int iter = 0; iter < regentSetting.getNumberOfMutationOfPopulation(); iter++) {
            Integer which = randomGenerator.nextIntegerTo(population.size());
            NeuralNetwork network = population.get(which).getNeuralNetwork();
            mutation.add(network);
            population.remove(which);
        }
    }

    private void addToMutationFromCrossovers(List<NeuralNetwork> mutation, List<NeuralNetwork> crossOvered, RegentSetting regentSetting) {
        for (int iter = 0; iter < regentSetting.getNumberOfMutationOfCrossovers(); iter++) {
            Integer which = randomGenerator.nextIntegerTo(crossOvered.size());
            NeuralNetwork network = crossOvered.get(which);
            mutation.add(network);
            crossOvered.remove(which);
        }
    }

    private List<NeuralNetwork> mutateNetwork(List<NeuralNetwork> mutation, Dataset dataset, RegentSetting regentSetting) {
        return mutation.parallelStream().map(network -> mutate(network, dataset, regentSetting, false)).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<NeuralNetwork> mutateInitialNetworkToMakeChildrens(NeuralNetwork network, Dataset dataset, RegentSetting regentSetting) {
        return LongStream.range(0, regentSetting.getPopulationSize()).parallel().mapToObj(i -> mutate(network, dataset, regentSetting, true)).collect(Collectors.toCollection(ArrayList::new));
    }

    private NeuralNetwork mutate(NeuralNetwork network, Dataset dataset, RegentSetting regentSetting, boolean canDeleteNode) {
        if (canDelete(canDeleteNode, regentSetting)) {
            return mutationByNodeDeletion(network);
        } else {
            return mutationByAddingNode(network, dataset, canDeleteNode, regentSetting);
        }
    }

    private NeuralNetwork mutationByAddingNode(NeuralNetwork network, Dataset dataset, boolean isPopulationInitialization, RegentSetting regentSetting) {
        int which = 0;
        if (isPopulationInitialization) {
            which = randomGenerator.nextIntegerTo((int) network.getNumberOfHiddenNodes());
        }
        randomGenerator.nextIntegerTo((int) network.getNumberOfHiddenNodes());
        return TopGen.generateSuccesor(network, dataset, which, regentSetting.getKBANNSetting(), randomGenerator);
    }

    private NeuralNetwork mutationByNodeDeletion(NeuralNetwork network) {
        NeuralNetwork copied = network.getCopy();
        long size = copied.getNumberOfHiddenNodes();
        long idx = randomGenerator.nextLongTo(size);
        Node node = copied.getHiddenNodes().get((int) idx);
        copied.removeHiddenNode(node);
        return copied;
    }

    private boolean canDelete(boolean canDeleteNode, RegentSetting regentSetting) {
        return canDeleteNode && randomGenerator.isProbable(regentSetting.getProbabilityOfNodeDeletion());
    }

    private List<Pair<NeuralNetwork, NeuralNetwork>> tournamentSelection(List<Individual> population, RegentSetting regentSetting, long howMany) {
        List<Pair<NeuralNetwork, NeuralNetwork>> selected = new ArrayList<>();
        while (selected.size() < howMany) {
            Individual first = tournamentSelection(population, regentSetting, null);
            Individual second = tournamentSelection(population, regentSetting, first);
            selected.add(new Pair<>(first.getNeuralNetwork(), second.getNeuralNetwork()));
        }
        return selected;
    }

    private Individual tournamentSelection(List<Individual> population, RegentSetting regentSetting, Individual alreadySelected) {
        long tournamentSize = regentSetting.getTournamentSize();
        Individual selected = chooseIndividual(population);
        while (tournamentSize - 1 > 0) {
            Individual second = chooseIndividual(population);
            if (selected == alreadySelected || selected == alreadySelected) {
                continue;
            }
            selected = (selected.getFitness() < second.getFitness()) ? selected : second;
            tournamentSize--;
        }
        return selected;
    }

    private Individual chooseIndividual(List<Individual> population) {
        Integer start = randomGenerator.nextIntegerTo(population.size() - 1);
        return population.get(start);
    }

    private void updateBestSoFar(Pair<NeuralNetwork, Double> neuralNetworkDoublePair) {
        updateBestSoFar(neuralNetworkDoublePair.getLeft(), neuralNetworkDoublePair.getRight());
    }

    private List<Individual> computeFitness(List<NeuralNetwork> children, Dataset dataset) {
        return children.parallelStream().map(network -> computeFitness(network, dataset)).collect(Collectors.toCollection(ArrayList::new));
    }

    private Individual computeFitness(NeuralNetwork network, Dataset dataset) {
        return new Individual(network, Tools.computeAverageSuqaredTotalError(network, dataset));
    }
}
