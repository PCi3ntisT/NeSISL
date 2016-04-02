package main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning;

import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.*;
import java.util.List;

/**
 * Created by EL on 13.2.2016.
 */
public class WeightLearningSetting {
    public static final String EPSILON_TOKEN = "epsilon";
    public static final String EPOCH_TOKEN = "epochLimit";
    public static final String LEARNING_RATE_TOKEN = "learningRate";
    public static final String SHORT_TIME_WINDOW_TOKEN = "shortTimeWindow";
    public static final String LONG_TIME_WINDOW_TOKEN = "longTimeWindow";
    public static final String QUICKPROP_ALFA_TOKEN = "alpha";
    public static final String QUICKPROP_EPSILON_TOKEN = "quickpropEpsilon";
    public static final String MOMENTUM_ALPHA_TOKEN = "momentumAlpha";
    public static final String PENALTY_EPSILON_TOKEN = "SLFPenaltyEpsilon";
    public static final String SLF_THRESHOLD_TOKEN = "slfThreshold";

    private final Double epsilonDifference;
    private final Double quickpropEpsilon;
    private final Double alpha;
    private final Double learningRate;
    private final Double momentumAlpha;
    private final Long epochLimit;
    private final Integer shortTimeWindow;
    private final Integer longTimeWindow;
    private final Double slfThreshold;
    private final Double penaltyEpsilon;

    public WeightLearningSetting(Double epsilonDifference, Double quickpropEpsilon, Double alpha, Double learningRate, Double momentumAlpha, Long epochLimit, Integer shortTimeWindow, Integer longTimeWindow, Double penaltyEpsilon, Double slfThreshold) {
        this.epsilonDifference = epsilonDifference;
        this.quickpropEpsilon = quickpropEpsilon;
        this.alpha = alpha;
        this.learningRate = learningRate;
        this.momentumAlpha = momentumAlpha;
        this.epochLimit = epochLimit;
        this.shortTimeWindow = shortTimeWindow;
        this.longTimeWindow = longTimeWindow;
        this.penaltyEpsilon = penaltyEpsilon;
        this.slfThreshold = slfThreshold;
    }


    public static WeightLearningSetting parse(File file) {
        Double learningRate = null;
        Double alpha = null;
        Double quickpropEpsilon = null;
        Double epsilonDifference = null;
        Long epochLimit = null;
        Double momentumAlpha = null;
        Integer shortTimeWindow = null;
        Integer longTimeWindow = null;
        Double penaltyEpsilon = null;
        Double slfThreshold = null;
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
                    case MOMENTUM_ALPHA_TOKEN:
                        momentumAlpha = Double.valueOf(value);
                        break;
                    case SHORT_TIME_WINDOW_TOKEN:
                        shortTimeWindow = Integer.valueOf(value);
                        break;
                    case LONG_TIME_WINDOW_TOKEN:
                        longTimeWindow = Integer.valueOf(value);
                        break;
                    case PENALTY_EPSILON_TOKEN:
                        penaltyEpsilon = Double.valueOf(value);
                        break;
                    case SLF_THRESHOLD_TOKEN:
                        slfThreshold = Double.valueOf(value);
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
        return new WeightLearningSetting(epsilonDifference, quickpropEpsilon, alpha, learningRate, momentumAlpha, epochLimit, shortTimeWindow, longTimeWindow, penaltyEpsilon, slfThreshold);
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
            System.out.println("Be aware that 'alpha' contains null value.");
        }
        return alpha;
    }

    public Double getQuickpropEpsilon() {
        if (null == quickpropEpsilon) {
            System.out.println("Be aware that 'quickpropEpsilon' contains null value.");
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

    public Double getPenaltyEpsilon() {
        return penaltyEpsilon;
    }

    public Double getSLFThreshold() {
        return slfThreshold;
    }

    public Double getAlpha() {
        return alpha;
    }

    public Integer getShortTimeWindow() {
        return shortTimeWindow;
    }

    public Integer getLongTimeWindow() {
        return longTimeWindow;
    }

    public boolean canContinueBackpropagation(long iteration, List<Double> errors) {
        if (iteration > epochLimit) {
            return false;
        }
        return !(errors.size() > longTimeWindow && Tools.hasConverged(errors, longTimeWindow, shortTimeWindow, epsilonDifference));
    }
}
