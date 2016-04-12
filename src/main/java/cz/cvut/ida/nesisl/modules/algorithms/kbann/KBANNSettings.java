package main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann;

import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;

import java.io.*;

/**
 * Created by EL on 6.3.2016.
 */
public class KBANNSettings {

    public static final String OMEGA_TOKEN = "omega";

    private final RandomGenerator randomGenerator;
    private final Double omega;

    public KBANNSettings(RandomGenerator randomGenerator, Double omega) {
        this.randomGenerator = randomGenerator;
        this.omega = omega;
    }

    public RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public Double getOmega() {
        return omega;
    }

    @Override
    public String toString() {
        return "KBANNSettings{" +
                "randomGenerator=" + randomGenerator +
                ", omega=" + omega +
                '}';
    }

    public static KBANNSettings create(RandomGeneratorImpl randomGenerator, File file) {
        Double omega = null;
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
                    case OMEGA_TOKEN:
                        omega = Double.valueOf(value);
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
        return new KBANNSettings(randomGenerator, omega);
    }

}
