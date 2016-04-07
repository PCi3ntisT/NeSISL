package main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen;

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

    private final Double epsilonLimit;
    private final Double omega;
    private final Double epsilonConvergent;
    private final Long numberOfSuccessors;
    private final Long lengthOfOpenList;
    private final Integer shortTimeWindow;
    private final Integer longTimeWindow;
    private final Double learningRateDecay;

    public TopGenSettings(Double epsilonLimit, Long numberOfSuccessors, Long lengthOfOpenList, Double omega,Integer  longTimeWindow, Integer shortTimeWindow, Double epsilon, Double learningRateDecay) {
        this.epsilonLimit = epsilonLimit;
        this.numberOfSuccessors = numberOfSuccessors;
        this.lengthOfOpenList = lengthOfOpenList;
        this.omega = omega;
        this.shortTimeWindow = shortTimeWindow;
        this.longTimeWindow = longTimeWindow;
        this.epsilonConvergent = epsilon;
        this.learningRateDecay = learningRateDecay;
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

    public static TopGenSettings create(File file) {
        Double thresholdErrorToStop = null;
        Double omega = null;
        Double epsilon = null;
        Double learningRateDecay = null;
        Long numberOfSuccessors = null;
        Long lengthOfOpenList = null;
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
        return new TopGenSettings(thresholdErrorToStop, numberOfSuccessors, lengthOfOpenList, omega, longTimeWindow, shortTimeWindow, epsilon, learningRateDecay);
    }

    public boolean canContinue(long iteration, List<Double> errors) {
        return (errors.size() > longTimeWindow) ? !Tools.hasConverged(errors,longTimeWindow,shortTimeWindow, epsilonConvergent) : true;
    }

    public Double getLearningRateDecay() {
        return learningRateDecay;
    }

}
