package main.java.cz.cvut.ida.nesisl.modules.experiments;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by EL on 27.3.2016.
 */
public class ExperimentsTool {
    public static List<Boolean> intBitToBooleanList(int number, int numberOfLiterals) {
        List<Boolean> list = new ArrayList<>();
        for (int iter = 0; iter < numberOfLiterals; iter++) {
            Boolean val = false;
            if (1 == (number % 2)) {
                val = true;
            }
            list.add(val);
            number = number / 2;
        }
        return list;
    }

    public static List<Boolean> longBitToBooleanList(long number, int numberOfLiterals) {
        List<Boolean> list = new ArrayList<>();
        for (int iter = 0; iter < numberOfLiterals; iter++) {
            Boolean val = false;
            if (1 == (number % 2)) {
                val = true;
            }
            list.add(val);
            number = number / 2;
        }
        return list;
    }

    public static String booleanToZeroOne(Boolean bool) {
        return bool ? "1" : "0";
    }
}
