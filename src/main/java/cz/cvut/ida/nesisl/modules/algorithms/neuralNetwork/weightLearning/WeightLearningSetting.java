package main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning;

import java.io.*;

/**
 * Created by EL on 13.2.2016.
 */
public class WeightLearningSetting {
    public static final String EPSILON_TOKEN = "epsilon";
    public static final String EPOCH_TOKEN = "epochLimit";
    public static final String LEARNING_RATE_TOKEN = "learningRate";
    public static final String HIDDEN_NODES_LIMIT_TOKEN = "maximumNumberOfHiddenNodes";
    public static final String CASCOR_POOL_NODES_LIMIT_TOKEN = "cascorPoolNodesLimit";
    public static final String QUICKPROP_ALFA_TOKEN = "alpha";
    public static final String QUICKPROP_EPSILON_TOKEN = "quickpropEpsilon";
    public static final String MOMENTUM_ALPHA_TOKEN = "momentumAlpha";

    private final Double epsilonDifference;
    private final Double quickpropEpsilon;
    private final Double alpha;
    private final Double learningRate;
    private final Long maximumNumberOfHiddenNodes;
    private final Integer sizeOfCasCorPool;
    private final Long epochLimit;
    private final Double momentumAlpha;

    public WeightLearningSetting(Double epsilonDifference, Double learningRate, Long maximumNumberOfHiddenNodes, Integer sizeOfCasCorPool, Double alpha, Double quickpropEpsilon, Long epochLimit, Double momentumAlpha) {
        this.epsilonDifference = epsilonDifference;
        this.learningRate = learningRate;
        this.maximumNumberOfHiddenNodes = maximumNumberOfHiddenNodes;
        this.sizeOfCasCorPool = sizeOfCasCorPool;
        this.alpha = alpha;
        this.quickpropEpsilon = quickpropEpsilon;
        this.epochLimit = epochLimit;
        this.momentumAlpha = momentumAlpha;
    }


    public static WeightLearningSetting parse(File file) {
        Double learningRate = null;
        Double alpha = null;
        Double quickpropEpsilon = null;
        Double epsilonDifference = null;
        Long maximumNumberOfHiddenNodes = null;
        Integer sizeOfCasCorPool = null;
        Long epochLimit = null;
        Double momentumAlpha = null;
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
                    case EPOCH_TOKEN:
                        epochLimit = Long.valueOf(value);
                        break;
                    case LEARNING_RATE_TOKEN:
                        learningRate = Double.valueOf(value);
                        break;
                    case QUICKPROP_ALFA_TOKEN:
                        alpha = Double.valueOf(value);
                        break;
                    case QUICKPROP_EPSILON_TOKEN:
                        quickpropEpsilon = Double.valueOf(value);
                        break;
                    case CASCOR_POOL_NODES_LIMIT_TOKEN:
                        sizeOfCasCorPool = Integer.valueOf(value);
                        break;
                    case HIDDEN_NODES_LIMIT_TOKEN:
                        maximumNumberOfHiddenNodes = Long.valueOf(value);
                        break;
                    case MOMENTUM_ALPHA_TOKEN:
                        momentumAlpha = Double.valueOf(value);
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
        return new WeightLearningSetting(epsilonDifference, learningRate, maximumNumberOfHiddenNodes, sizeOfCasCorPool, alpha, quickpropEpsilon, epochLimit, momentumAlpha);
    }

    public long getMaximumNumberOfHiddenNodes() {
        if (null == maximumNumberOfHiddenNodes) {
            System.out.println("Be aware that 'maximumNumberOfHiddenNodes' contains null value.");
        }
        return maximumNumberOfHiddenNodes;
    }

    public int getSizeOfCasCorPool() {
        if (null == sizeOfCasCorPool) {
            System.out.println("Be aware that 'sizeOfCasCorPool' contains null value.");
        }
        return sizeOfCasCorPool;
    }


    public Double getEpsilonDifference() {
        if (null == epsilonDifference) {
            System.out.println("Be aware that 'epsilonDifference' contains null value.");
        }
        return epsilonDifference;
    }

    public Double getLearningRate() {
        if (null == learningRate) {
            System.out.println("Be aware that 'learningRate' contains null value.");
        }
        return learningRate;
    }

    public Double getMaxAlpha() {
        if (null == alpha) {
            //System.out.println("Be aware that 'alpha' contains null value.");
        }
        return alpha;
    }

    public Double getQuickpropEpsilon() {
        if (null == quickpropEpsilon) {
            //System.out.println("Be aware that 'quickpropEpsilon' contains null value.");
        }
        return quickpropEpsilon;
    }

    public long getEpochLimit() {
        if (null == epochLimit) {
            System.out.println("Be aware that 'epochLimit' contains null value.");
        }
        return epochLimit;
    }

    public Double getMomentumAlpha() {
        if (null == momentumAlpha) {
            System.out.println("Be aware that 'momentumAlpha' contains null value.");
        }
        return momentumAlpha;
    }
}
