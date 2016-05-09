package main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation;

import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.*;
import java.util.List;

/**
 * Created by EL on 16.3.2016.
 */
public class CascadeCorrelationSetting {

    public static final String POOL_SIZE_LIMIT_TOKEN = "poolSizeLimit";
    public static final String HIDDEN_NODE_LIMIT_TOKEN = "hiddenNodeLimit";
    public static final String EPSILON_CONVERGENT_TOKEN = "epsilonConvergent";
    public static final String SHORT_TIME_WINDOW_TOKEN = "shortTimeWindow";
    public static final String LONG_TIME_WINDOW_TOKEN = "longTimeWindow";
    public static final String CANDIDATE_ITERATION_LEARNING_LIMIT_TOKEN = "candidateIterationLearningLimit";

    private long sizeOfCasCorPool;
    private long maximumNumberOfHiddenNodes;
    private final Double epsilonConvergent;
    private final Integer shortTimeWindow;
    private final Integer longTimeWindow;
    private final Long maxCandidateIteration;

    public CascadeCorrelationSetting(Double epsilonConvergent, Integer shortTimeWindow, Integer longTimeWindow, long sizeOfCasCorPool, long maximumNumberOfHiddenNodes, long maxCandidateIteration) {
        this.epsilonConvergent = epsilonConvergent;
        this.shortTimeWindow = shortTimeWindow;
        this.longTimeWindow = longTimeWindow;
        this.sizeOfCasCorPool = sizeOfCasCorPool;
        this.maximumNumberOfHiddenNodes = maximumNumberOfHiddenNodes;
        this.maxCandidateIteration = maxCandidateIteration;
    }

    public Double getEpsilonConvergent() {
        return epsilonConvergent;
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
        return  iteration > maxCandidateIteration
                || Tools.hasConverged(correlations,getLongTimeWindow(),getShortTimeWindow(), getEpsilonConvergent());
    }

    public boolean stopCascadeCorrelation(long numberOfAddedNodes, List<Double> errors) {
        return numberOfAddedNodes > getMaximumNumberOfHiddenNodes()
                || Tools.hasConverged(errors, getLongTimeWindow(), getShortTimeWindow(), getEpsilonConvergent())
                || (errors.size() > 0 && errors.get(errors.size() - 1) < Tools.convergedError());
    }

    public static CascadeCorrelationSetting create(File file) {
        Long hiddenNodeLimit = null;
        Long poolSizeLimit = null;
        Long candidateIterationLimit = null;
        Integer shortTimeWindow = null;
        Integer longTimeWindow = null;
        Double epsilonConvergent = null;
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
                    case EPSILON_CONVERGENT_TOKEN:
                        epsilonConvergent = Double.valueOf(value);
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
        return new CascadeCorrelationSetting(epsilonConvergent, shortTimeWindow, longTimeWindow, poolSizeLimit, hiddenNodeLimit, candidateIterationLimit);
    }
}
