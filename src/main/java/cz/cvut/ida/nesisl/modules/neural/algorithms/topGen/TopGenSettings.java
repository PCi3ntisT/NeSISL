package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.topGen;

import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

import java.io.*;
import java.util.List;

/**
 * Created by EL on 21.3.2016.
 */
public class TopGenSettings {

    public static final String EPSILON_LIMIT_TOKEN = "epsilonLimit";
    public static final String OMEGA_TOKEN = "omega";
    public static final String SUCCESSORS_GENERATED_LIMIT_TOKEN = "successorsLimit";
    public static final String OPEN_LIST_LIMIT_TOKEN = "openListLength";
    public static final String SHORT_TIME_WINDOW_TOKEN = "shortTimeWindow";
    public static final String LONG_TIME_WINDOW_TOKEN = "longTimeWindow";
    public static final String EPSILON_CONVERGENT_TOKEN = "epsilonConvergent";
    public static final String LEARNING_RATE_DECAY_TOKEN = "learningRateDecay";
    public static final String NODE_ACTIVATION_THRESHOLD_TOKEN = "nodeActivationThreshold";
    public static final String PERTURBATION_TOKEN = "perturbation";
    public static final String NODE_SEARCH_LIMIT = "nodeSearchLimit"; // its not node limit, but picked nodes from the queue
    public static final String INNER_CROSSVALIDATION_RATIO = "innerCrossvalidationRatioFolds";

    private final Integer innerCrossvalidationRatio;
    private final Double perturbationMagnitude;
    private final Double epsilonLimit;
    private final Double omega;
    private final Double epsilonConvergent;
    private final Long numberOfSuccessors;
    private final Long lengthOfOpenList;
    private final Integer shortTimeWindow;
    private final Integer longTimeWindow;
    private final Double learningRateDecay;
    private final Double nodeActivationThreshold;
    private final Integer nodeSearchLimit;
    private int invoked = 0;

    public TopGenSettings(Double epsilonLimit, Long numberOfSuccessors, Long lengthOfOpenList, Double omega, Integer longTimeWindow, Integer shortTimeWindow, Double epsilonConvergent, Double learningRateDecay, Double nodeActivationThreshold, Double perturbationMagnitude, Integer nodeSearchLimit, Integer innerCrossvalidationRatio) {
        this.epsilonLimit = epsilonLimit;
        this.numberOfSuccessors = numberOfSuccessors;
        this.lengthOfOpenList = lengthOfOpenList;
        this.omega = omega;
        this.shortTimeWindow = shortTimeWindow;
        this.longTimeWindow = longTimeWindow;
        this.epsilonConvergent = epsilonConvergent;
        this.learningRateDecay = learningRateDecay;
        this.nodeActivationThreshold = nodeActivationThreshold;
        this.perturbationMagnitude = perturbationMagnitude;
        this.nodeSearchLimit = nodeSearchLimit;
        this.innerCrossvalidationRatio = innerCrossvalidationRatio;
    }

    public Integer getInnerCrossvalidationRatio() {
        return innerCrossvalidationRatio;
    }

    public Double getEpsilonLimit() {
        return epsilonLimit;
    }

    public Long getNumberOfSuccessors() {
        return numberOfSuccessors;
    }

    public Long getLengthOfOpenList() {
        return lengthOfOpenList;
    }

    public Double getOmega() {
        return omega;
    }

    public Double getLearningRateDecay() {
        return learningRateDecay;
    }

    public Double getNodeActivationThreshold() {
        return nodeActivationThreshold;
    }

    public static TopGenSettings create(File file) {
        Double thresholdErrorToStop = null;
        Double omega = null;
        Double epsilon = null;
        Double learningRateDecay = null;
        Long numberOfSuccessors = null;
        Long lengthOfOpenList = null;
        Integer shortTimeWindow = null;
        Integer longTimeWindow = null;
        Double nodeActivationThreshold = null;
        Double perturbation = null;
        Integer nodeSearchLimit = null;
        Integer innerCrossvalidationRatio = null;
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
                    case EPSILON_LIMIT_TOKEN:
                        thresholdErrorToStop = Double.valueOf(value);
                        break;
                    case OMEGA_TOKEN:
                        omega = Double.valueOf(value);
                        break;
                    case SUCCESSORS_GENERATED_LIMIT_TOKEN:
                        numberOfSuccessors = Long.valueOf(value);
                        break;
                    case OPEN_LIST_LIMIT_TOKEN:
                        lengthOfOpenList = Long.valueOf(value);
                        break;
                    case SHORT_TIME_WINDOW_TOKEN:
                        shortTimeWindow = Integer.valueOf(value);
                        break;
                    case LONG_TIME_WINDOW_TOKEN:
                        longTimeWindow = Integer.valueOf(value);
                        break;
                    case EPSILON_CONVERGENT_TOKEN:
                        epsilon = Double.valueOf(value);
                        break;
                    case LEARNING_RATE_DECAY_TOKEN:
                        learningRateDecay = Double.valueOf(value);
                        break;
                    case NODE_ACTIVATION_THRESHOLD_TOKEN:
                        nodeActivationThreshold = Double.valueOf(value);
                        break;
                    case PERTURBATION_TOKEN:
                        perturbation = Double.valueOf(value);
                        break;
                    case NODE_SEARCH_LIMIT:
                        nodeSearchLimit = Integer.valueOf(value);
                        break;
                    case INNER_CROSSVALIDATION_RATIO:
                        innerCrossvalidationRatio = Integer.valueOf(value);
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
        return new TopGenSettings(thresholdErrorToStop, numberOfSuccessors, lengthOfOpenList, omega, longTimeWindow, shortTimeWindow, epsilon, learningRateDecay, nodeActivationThreshold, perturbation, nodeSearchLimit,innerCrossvalidationRatio);
    }

    public boolean canContinue(long iteration, List<Double> errors) {
        if (errors.size() > 0 && errors.get(errors.size() - 1) < Tools.convergedError()) {
            return false;
        }else if(invoked > nodeSearchLimit){
            return false;
        }
        invoked++;
        return (errors.size() > longTimeWindow) ? !Tools.hasConverged(errors, longTimeWindow, shortTimeWindow, epsilonConvergent) : true;
    }

    public Double perturbationMagnitude() {
        return perturbationMagnitude;
    }

    public Double getPerturbationMagnitude() {
        return perturbationMagnitude;
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

    public Integer getNodeSearchLimit() {
        return nodeSearchLimit;
    }

    public int getInvoked() {
        return invoked;
    }

    public static TopGenSettings create(TopGenSettings tgSetting) {
        return new TopGenSettings(tgSetting.getEpsilonLimit(),
                tgSetting.getNumberOfSuccessors(),
                tgSetting.getLengthOfOpenList(),
                tgSetting.getOmega(),
                tgSetting.getLongTimeWindow(),
                tgSetting.getShortTimeWindow(),
                tgSetting.getEpsilonConvergent(),
                tgSetting.getLearningRateDecay(),
                tgSetting.getNodeActivationThreshold(),
                tgSetting.getPerturbationMagnitude(),
                tgSetting.getNodeSearchLimit(),
                tgSetting.getInnerCrossvalidationRatio());
    }
}
