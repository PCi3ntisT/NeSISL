package main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation;

import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.util.List;

/**
 * Created by EL on 16.3.2016.
 */
public class CascadeCorrelationSetting {

    public static final String POOL_SIZE_LIMIT_TOKEN = "poolSizeLimit";
    public static final String HIDDEN_NODE_LIMIT_TOKEN = "hiddenNodeLimit";
    public static final String EPSILON_TOKEN = "epsilon";
    public static final String SHORT_TIME_WINDOW_TOKEN = "shortTimeWindow";
    public static final String LONG_TIME_WINDOW_TOKEN = "longTimeWindow";
    public static final String CANDIDATE_ITERATION_LEARNING_LIMIT_TOKEN = "candidateIterationLearningLimit";

    private long sizeOfCasCorPool;
    private long maximumNumberOfHiddenNodes;
    private final Double epsilonDifference;
    private final Integer shortTimeWindow;
    private final Integer longTimeWindow;
    private final Long maxCandidateIteration;

    public CascadeCorrelationSetting(Double epsilonDifference, Integer shortTimeWindow, Integer longTimeWindow, long sizeOfCasCorPool, long maximumNumberOfHiddenNodes, long maxCandidateIteration) {
        this.epsilonDifference = epsilonDifference;
        this.shortTimeWindow = shortTimeWindow;
        this.longTimeWindow = longTimeWindow;
        this.sizeOfCasCorPool = sizeOfCasCorPool;
        this.maximumNumberOfHiddenNodes = maximumNumberOfHiddenNodes;
        this.maxCandidateIteration = maxCandidateIteration;
    }

    public Double getEpsilonDifference() {
        return epsilonDifference;
    }

    public Integer getShortTimeWindow() {
        return shortTimeWindow;
    }

    public Integer getLongTimeWindow() {
        return longTimeWindow;
    }

    public long getSizeOfCasCorPool() {
        return sizeOfCasCorPool;
    }

    public void setSizeOfCasCorPool(int sizeOfCasCorPool) {
        this.sizeOfCasCorPool = sizeOfCasCorPool;
    }

    public long getMaximumNumberOfHiddenNodes() {
        return maximumNumberOfHiddenNodes;
    }

    public void setMaximumNumberOfHiddenNodes(long maximumNumberOfHiddenNodes) {
        this.maximumNumberOfHiddenNodes = maximumNumberOfHiddenNodes;
    }

    public boolean canStopLearningCandidatConnection(List<Double> correlations, long iteration) {
        return  iteration > maxCandidateIteration || Tools.hasConverged(correlations,getLongTimeWindow(),getShortTimeWindow(),getEpsilonDifference());
    }

    public boolean stopCascadeCorrelation(long numberOfAddedNodes, List<Double> errors) {
        return numberOfAddedNodes > getMaximumNumberOfHiddenNodes() || Tools.hasConverged(errors,getLongTimeWindow(),getShortTimeWindow(),getEpsilonDifference());
    }

    public static CascadeCorrelationSetting create(File file) {
        Long hiddenNodeLimit = null;
        Long poolSizeLimit = null;
        Long candidateIterationLimit = null;
        Integer shortTimeWindow = null;
        Integer longTimeWindow = null;
        Double epsilonDifference = null;
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
                    case EPSILON_TOKEN:
                        epsilonDifference = Double.valueOf(value);
                        break;
                    case POOL_SIZE_LIMIT_TOKEN:
                        poolSizeLimit = Long.valueOf(value);
                        break;
                    case HIDDEN_NODE_LIMIT_TOKEN:
                        hiddenNodeLimit = Long.valueOf(value);
                        break;
                    case SHORT_TIME_WINDOW_TOKEN:
                        shortTimeWindow = Integer.valueOf(value);
                        break;
                    case LONG_TIME_WINDOW_TOKEN:
                        longTimeWindow = Integer.valueOf(value);
                        break;
                    case CANDIDATE_ITERATION_LEARNING_LIMIT_TOKEN:
                        candidateIterationLimit = Long.valueOf(value);
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
        return new CascadeCorrelationSetting(epsilonDifference, shortTimeWindow, longTimeWindow, poolSizeLimit, hiddenNodeLimit, candidateIterationLimit);
    }
}