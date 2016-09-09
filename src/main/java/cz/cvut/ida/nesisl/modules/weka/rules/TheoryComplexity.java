package main.java.cz.cvut.ida.nesisl.modules.weka.rules;

import java.util.Arrays;

/**
 * Created by EL on 9.9.2016.
 */
public class TheoryComplexity {

    public TheoryComplexity getDefault(){
        return new TheoryComplexity();
    }

    public long getComplexity(String theory){
        return Arrays.stream(theory.split("\n"))
        .filter(line -> line.length() > 0)
        .count();
    }
}
