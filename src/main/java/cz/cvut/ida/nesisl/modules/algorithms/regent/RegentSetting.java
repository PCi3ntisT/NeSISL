package main.java.cz.cvut.ida.nesisl.modules.algorithms.regent;

import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGenSettings;

/**
 * Created by EL on 23.3.2016.
 */
public class RegentSetting {
    private final long tournamentSize;
    private final long populationSize;
    private final TopGenSettings topGenSettings;
    private final Integer numberOfMutationOfPopulation;
    private final Integer numberOfMutationOfCrossovers;
    private final  KBANNSettings KBANNSetting;
    private final Double probabilityOfNodeDeletion;
    private final Long maxAllowedFitness;
    private final Integer numberOfCrossoverChildren;
    private final Integer numberOfElites;
    private Long computedFitness = 0l;

    public RegentSetting(long tournamentSize, long populationSize, TopGenSettings topGenSettings, Integer numberOfMutationOfPopulation, Integer numberOfMutationOfCrossovers, KBANNSettings KBANNSetting, Double probabilityOfNodeDeletion, Long maxAllowedFitness, Integer numberOfCrossoverChildren, Integer numberOfElites) {
        this.tournamentSize = tournamentSize;
        this.populationSize = populationSize;
        this.topGenSettings = topGenSettings;
        this.numberOfMutationOfPopulation = numberOfMutationOfPopulation;
        this.numberOfMutationOfCrossovers = numberOfMutationOfCrossovers;
        this.KBANNSetting = KBANNSetting;
        this.probabilityOfNodeDeletion = probabilityOfNodeDeletion;
        this.maxAllowedFitness = maxAllowedFitness;
        this.numberOfCrossoverChildren = numberOfCrossoverChildren;
        this.numberOfElites = numberOfElites;
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
        synchronized (this.computedFitness){
            return this.computedFitness;
        }
    }

    public Integer getNumberOfCrossoverChildren() {
        return numberOfCrossoverChildren;
    }

    public Integer getNumberOfElites() {
        return numberOfElites;
    }

    public void increaseFitnessCountSynchronized() {
        synchronized (this.computedFitness){
            this.computedFitness++;
        }
    }

    public Integer getNumberOfMutationOfPopulation() {
        return numberOfMutationOfPopulation;
    }

    public Integer getNumberOfMutationOfCrossovers() {
        return numberOfMutationOfCrossovers;
    }

    public KBANNSettings getKBANNSetting() {
        return KBANNSetting;
    }

    public Double getProbabilityOfNodeDeletion() {
        return probabilityOfNodeDeletion;
    }
}
