package main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.dynamicNodeCreation;

import java.io.*;

/**
 * Created by EL on 14.3.2016.
 */
public class DNCSetting {
    public static final String DELTA_T_TOKEN = "deltaT";
    public static final String TIME_WINDOW_TOKEN = "timeWindow";
    public static final String CM_TOKEN = "ca";
    public static final String CA_TOKEN = "cm";
    public static final String HIDDEN_NODE_LIMIT_TOKEN = "hiddenNodeLimit";


    private final Double deltaT;
    private final Integer timeWindow;
    private final Double cm;
    private final Double ca;
    private final Long hiddenNodesLimit;

    public DNCSetting(double deltaT, int timeWindow, double cm, double ca, long hiddenNodesLimit) {
        this.deltaT = deltaT;
        this.timeWindow = timeWindow;
        this.cm = cm;
        this.ca = ca;
        this.hiddenNodesLimit = hiddenNodesLimit;
    }

    public long getTimeWindow() {
        return timeWindow;
    }

    public double getDeltaT() {
        return deltaT;
    }

    public double getCm() {
        return cm;
    }

    public double getCa() {
        return ca;
    }

    public long getHiddenNodesLimit() {
        return hiddenNodesLimit;
    }

    public static DNCSetting create(File file) {
        Double deltaT = null;
        Double cm = null;
        Double ca = null;
        Integer timeWindow = null;
        Long hiddenNodeLimit = null;
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
                    case DELTA_T_TOKEN:
                        deltaT = Double.valueOf(value);
                        break;
                    case TIME_WINDOW_TOKEN:
                        timeWindow = Integer.valueOf(value);
                        break;
                    case CM_TOKEN:
                        cm = Double.valueOf(value);
                        break;
                    case CA_TOKEN:
                        ca = Double.valueOf(value);
                        break;
                    case HIDDEN_NODE_LIMIT_TOKEN:
                        hiddenNodeLimit = Long.valueOf(value);
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
        return new DNCSetting(deltaT, timeWindow, cm, ca, hiddenNodeLimit);
    }
}
