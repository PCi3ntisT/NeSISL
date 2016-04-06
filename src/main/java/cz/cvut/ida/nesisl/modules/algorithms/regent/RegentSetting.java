package main.java.cz.cvut.ida.nesisl.modules.algorithms.regent;

import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGenSettings;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.*;
import java.util.List;

/**
 * Created by EL on 23.3.2016.
 */
public class RegentSetting {
    public static final String TOURNAMENT_SIZE_TOKEN = "tournamentSize";
    public static final String POPULATION_SIZE_TOKEN = "populationSize";
    public static final String MUTATION_OF_POPULATION_TOKEN = "percentageOfMutationInPopulation";
    public static final String MUTATION_OF_CROSSOVERS_TOKEN = "percentageOfMutationOfCrossovers";
    public static final String NODE_DELETION_PROBABILITY_TOKEN = "probabilityOfNodeDeletion";
    public static final String FITNESS_LIMIT_TOKEN = "maxAllowedFitness";
    public static final String CROSSOVER_CHILDREN_TOKEN = "percentageOfCrossoverChildrenPairs";
    public static final String ELITES_TOKEN = "numberOfElites";
    public static final String EDGE_WEIGHT_CROSSOVER_LIMIT_TOKEN = "edgeWeightCrossoverLimit";
    public static final String SHORT_TIME_WINDOW_TOKEN = "shortTimeWindow";
    public static final String LONG_TIME_WINDOW_TOKEN = "longTimeWindow";
    public static final String EPSILON_LIMIT_TOKEN = "epsilonConvergent";

    private final long tournamentSize;
    private final long populationSize;
    private final TopGenSettings topGenSettings;
    private final Integer percentageOfMutationOfPopulation;
    private final Integer percentageOfMutationOfCrossovers;
    private final KBANNSettings KBANNSetting;
    private final Double probabilityOfNodeDeletion;
    private final Long maxAllowedFitness;
    private final Integer percentageOfCrossoverChildrenPairs;
    private final Integer numberOfElites;
    private Long computedFitness = 0l;
    private final Double edgeWeightLimitAfterCrossover;
    private final Double epsilonConvergent;
    private final Integer shortTimeWindow;
    private final Integer longTimeWindow;

    public RegentSetting(long tournamentSize, long populationSize, TopGenSettings topGenSettings, Integer numberOfMutationOfPopulation, Integer numberOfMutationOfCrossovers, KBANNSettings KBANNSetting, Double probabilityOfNodeDeletion, Long maxAllowedFitness, Integer numberOfCrossoverChildren, Integer numberOfElites, Double edgeWeightLimitAfterCrossover, Integer shortTimewindow, Integer longTimewindow, Double epsilonConvergent) {
        this.tournamentSize = tournamentSize;
        this.populationSize = populationSize;
        this.topGenSettings = topGenSettings;
        this.percentageOfMutationOfPopulation = numberOfMutationOfPopulation;
        this.percentageOfMutationOfCrossovers = numberOfMutationOfCrossovers;
        this.KBANNSetting = KBANNSetting;
        this.probabilityOfNodeDeletion = probabilityOfNodeDeletion;
        this.maxAllowedFitness = maxAllowedFitness;
        this.percentageOfCrossoverChildrenPairs = numberOfCrossoverChildren;
        this.numberOfElites = numberOfElites;
        this.edgeWeightLimitAfterCrossover = edgeWeightLimitAfterCrossover;
        this.longTimeWindow = longTimewindow;
        this.shortTimeWindow = shortTimewindow;
        this.epsilonConvergent = epsilonConvergent;
    }

    public long getTournamentSize() {
        return tournamentSize;
    }

    public long getPopulationSize() {
        return populationSize;
    }

    public TopGenSettings getTopGenSettings() {
        return topGenSettings;
    }

    @Override
    public String toString() {
        return "RegentSetting{" +
                "tournamentSize=" + tournamentSize +
                ", populationSize=" + populationSize +
                ", topGenSettings=" + topGenSettings +
                '}';
    }

    public Long getMaxAllowedFitness() {
        return maxAllowedFitness;
    }

    public Long computedFitness() {
        synchronized (this.computedFitness) {
            return this.computedFitness;
        }
    }

    public Integer getNumberOfCrossoverChildrenPairs() {
        return (int) (populationSize * (percentageOfCrossoverChildrenPairs / 100.0));
    }

    public Integer getNumberOfElites() {
        return numberOfElites;
    }

    public void increaseFitnessCountSynchronized() {
        synchronized (this.computedFitness) {
            this.computedFitness++;
        }
    }

    public Integer getNumberOfMutationOfPopulation() {
        return (int) (populationSize * (percentageOfMutationOfPopulation / 100.0));
    }

    public Integer getNumberOfMutationOfCrossovers() {
        return (int) (populationSize * (percentageOfMutationOfCrossovers / 100.0));
    }

    public KBANNSettings getKBANNSetting() {
        return KBANNSetting;
    }

    public Double getProbabilityOfNodeDeletion() {
        return probabilityOfNodeDeletion;
    }

    public Integer getLongTimeWindow() {
        return longTimeWindow;
    }

    public Integer getShortTimeWindow() {
        return shortTimeWindow;
    }

    public Double getEpsilonConvergent() {
        return epsilonConvergent;
    }

    public Long getComputedFitness() {
        return computedFitness;
    }

    public static RegentSetting create(File file, RandomGeneratorImpl randomGenerator) {
        TopGenSettings tgSetting = TopGenSettings.create(file);
        Long tournamentSize = null;
        Long populationSize = null;
        Long maxAllowedFitness = null;
        Integer percentageOfMutationOfPopulation = null;
        Integer percentageOfMutationOfCrossovers = null;
        Integer percentageOfCrossoverChildren = null;
        Integer numberOfElites = null;
        Double probabilityOfNodeDeletion = null;
        Double edgeLimitCrossOver = null;
        Double epsilonConvergent = null;
        Integer shortTimeWindow = null;
        Integer longTimeWindow = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String token;
            String value = null;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(":", 2);
                if (1 >= splitted.length) {
                    token = null;
                } else {
                    token = splitted[0].trim();
                    value = splitted[1].trim();
                }

                if (null == token) {
                    continue;
                }

                switch (token) {
                    case CROSSOVER_CHILDREN_TOKEN:
                        percentageOfCrossoverChildren = Integer.valueOf(value);
                        break;
                    case ELITES_TOKEN:
                        numberOfElites = Integer.valueOf(value);
                        break;
                    case FITNESS_LIMIT_TOKEN:
                        maxAllowedFitness = Long.valueOf(value);
                        break;
                    case MUTATION_OF_CROSSOVERS_TOKEN:
                        percentageOfMutationOfCrossovers = Integer.valueOf(value);
                        break;
                    case MUTATION_OF_POPULATION_TOKEN:
                        percentageOfMutationOfPopulation = Integer.valueOf(value);
                        break;
                    case NODE_DELETION_PROBABILITY_TOKEN:
                        probabilityOfNodeDeletion = Double.valueOf(value);
                        break;
                    case POPULATION_SIZE_TOKEN:
                        populationSize = Long.valueOf(value);
                        break;
                    case TOURNAMENT_SIZE_TOKEN:
                        tournamentSize = Long.valueOf(value);
                        break;
                    case EDGE_WEIGHT_CROSSOVER_LIMIT_TOKEN:
                        edgeLimitCrossOver = Double.valueOf(value);
                        break;
                    case EPSILON_LIMIT_TOKEN:
                        epsilonConvergent = Double.valueOf(value);
                        break;
                    case SHORT_TIME_WINDOW_TOKEN:
                        shortTimeWindow = Integer.valueOf(value);
                        break;
                    case LONG_TIME_WINDOW_TOKEN:
                        longTimeWindow = Integer.valueOf(value);
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

        return new RegentSetting(tournamentSize, populationSize, tgSetting, percentageOfMutationOfPopulation, percentageOfMutationOfCrossovers, new KBANNSettings(randomGenerator, tgSetting.getOmega()), probabilityOfNodeDeletion, maxAllowedFitness, percentageOfCrossoverChildren, numberOfElites, edgeLimitCrossOver, shortTimeWindow, longTimeWindow, epsilonConvergent);
    }

    public Double getEdgeWeightLimitAfterCrossover() {
        return edgeWeightLimitAfterCrossover;
    }

    public boolean canContinue(Long usedFitness, List<Double> errors) {
        return !(usedFitness > getMaxAllowedFitness() || Tools.hasConverged(errors, getLongTimeWindow(), getShortTimeWindow(), getEpsilonConvergent()));
    }
}
