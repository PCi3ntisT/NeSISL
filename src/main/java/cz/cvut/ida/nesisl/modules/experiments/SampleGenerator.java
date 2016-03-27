package main.java.cz.cvut.ida.nesisl.modules.experiments;

import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Created by EL on 16.3.2016.
 */
public class SampleGenerator {

    public static void main(String arg[]) {
        int seed = 13;
        double sigma = 1.0d;
        double mu = 0.0d;
        RandomGeneratorImpl random = new RandomGeneratorImpl(sigma, mu, seed);

        SampleGenerator sample = new SampleGenerator();


        //sample.generateXor3(20, random);
        //sample.generateDNF4(50, random);
        //sample.generateAnd3(12, random);
        //sample.generateAnd2(4, random);
        //sample.generateAnd2Reversed(4, random);
        //sample.generateXor2(4, random);
        //sample.generateXor2Reversed(4, random);
        //sample.generateXor4(16, random);
        //sample.generateXor4Reversed(16, random);
        //sample.generateOr2Clauses(16, random);
        //sample.generateAllOrNothing(16, random);
        //sample.generateDoubleImplication(32, random);
        //sample.generateTwoIndependentClauses(32, random);
        //sample.generateThreeIndependentClauses(32, random);
        //sample.generateThreeHierarchyClauses(32, random);
        sample.generateThreeHierarchyClausesWithXor(32,random);
    }

    private void generateThreeHierarchyClauses(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\te\t|\tx");
        int numberOfLiterals = 5;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);
            Boolean e = input.get(4);

            StringBuilder sb = new StringBuilder();
            input.forEach(bol -> sb.append(ExperimentsTool.booleanToZeroOne(bol)).append("\t"));
            sb.append("|\t");

            Boolean m1 = !a && b;
            Boolean m2 = c && e;
            Boolean n = !m1 && m2;
            Boolean m = (a && b) || !c;
            Boolean p = !m && e;
            Boolean q = p || n;
            Boolean x = (p && !n) || !m || q;

            sb.append(ExperimentsTool.booleanToZeroOne(x));
            System.out.println(sb);
        });
    }

    private void generateThreeHierarchyClausesWithXor(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\te\t|\tx");
        int numberOfLiterals = 5;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);
            Boolean e = input.get(4);

            StringBuilder sb = new StringBuilder();
            input.forEach(bol -> sb.append(ExperimentsTool.booleanToZeroOne(bol)).append("\t"));
            sb.append("|\t");

            Boolean m1 = !a && b;
            Boolean m2 = c && e;
            Boolean n = !m1 && m2;
            Boolean m = (a && b) || !c;
            Boolean p = !m && e;
            Boolean q = p || n;
            Boolean v = (p && !n) != !m;
            Boolean x = v != q;

            sb.append(ExperimentsTool.booleanToZeroOne(x));
            System.out.println(sb);
        });
    }

    private void generateAnd2Reversed(int numberOfSamples, Object random) {
        System.out.println("a\tb\t|\tx\ty");
        int numberOfLiterals = 2;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);

            StringBuilder sb = new StringBuilder();
            input.forEach(bo -> sb.append(ExperimentsTool.booleanToZeroOne(bo)).append("\t"));
            sb.append("|\t");

            sb.append(ExperimentsTool.booleanToZeroOne(a && b)).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne(!(a && b)));

            System.out.println(sb);
        });
    }

    private void generateAnd2(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\t|\tx");
        int numberOfLiterals = 2;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);

            StringBuilder sb = new StringBuilder();
            input.forEach(bo -> sb.append(ExperimentsTool.booleanToZeroOne(bo)).append("\t"));
            sb.append("|\t");

            sb.append(ExperimentsTool.booleanToZeroOne(a && b));
            System.out.println(sb);
        });
    }

    private void generateAnd3(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\t|\tx\ty");
        int numberOfLIterals = 3;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLIterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);

            StringBuilder sb = new StringBuilder();
            input.forEach(bo -> sb.append(ExperimentsTool.booleanToZeroOne(bo)).append("\t"));
            sb.append("|\t");

            sb.append(ExperimentsTool.booleanToZeroOne(a && b && c)).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne(!a && !b && !c));
            System.out.println(sb);
        });
    }

    private void generateDNF4(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\t|\tx\ty\tz");
        int numberOfLIterals = 4;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLIterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);

            StringBuilder sb = new StringBuilder();
            input.forEach(bo -> sb.append(ExperimentsTool.booleanToZeroOne(bo)).append("\t"));
            sb.append("|\t");


            sb.append(ExperimentsTool.booleanToZeroOne((a && b && c && d))).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne((a && b) || (c && d))).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne(!a && !b));
            System.out.println(sb);
        });
    }

    private void generateXor3(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\t|\tx");
        int numberOfLIterals = 3;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLIterals);
            Boolean output = generateXor(input);

            StringBuilder sb = new StringBuilder();
            input.forEach(b -> sb.append(ExperimentsTool.booleanToZeroOne(b)).append("\t"));
            sb.append("|\t");

            sb.append(ExperimentsTool.booleanToZeroOne(output));
            System.out.println(sb);
        });
    }

    private void generateXor4(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\t|\tx");
        int numberOfLiterals = 4;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean output = generateXor(input);

            StringBuilder sb = new StringBuilder();
            input.forEach(b -> sb.append(ExperimentsTool.booleanToZeroOne(b)).append("\t"));
            sb.append("|\t");

            sb.append(ExperimentsTool.booleanToZeroOne(output));
            System.out.println(sb);
        });
    }

    private void generateXor4Reversed(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\t|\tx\ty");
        int numberOfLiterals = 4;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean output = generateXor(input);

            StringBuilder sb = new StringBuilder();
            input.forEach(b -> sb.append(ExperimentsTool.booleanToZeroOne(b)).append("\t"));
            sb.append("|\t");

            sb.append(ExperimentsTool.booleanToZeroOne(output)).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne(!output));
            System.out.println(sb);
        });
    }

    private void generateAllOrNothing(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\t|\tx");
        int numberOfLiterals = 4;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);

            StringBuilder sb = new StringBuilder();
            input.forEach(bol -> sb.append(ExperimentsTool.booleanToZeroOne(bol)).append("\t"));
            sb.append("|\t");

            Boolean output = (!a && !b && !c && !d) || (b && a && d && c);
            sb.append(ExperimentsTool.booleanToZeroOne(output));
            System.out.println(sb);
        });
    }

    private void generateDoubleImplication(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\te\t|\tx");
        int numberOfLiterals = 5;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);
            Boolean e = input.get(4);

            StringBuilder sb = new StringBuilder();
            input.forEach(bol -> sb.append(ExperimentsTool.booleanToZeroOne(bol)).append("\t"));
            sb.append("|\t");

            Boolean v1 = a && !b;
            Boolean v2 = !c && b;
            Boolean w = !(v1 || v2) || d;
            Boolean output = !(w && a) || e;
            sb.append(ExperimentsTool.booleanToZeroOne(output));
            System.out.println(sb);
        });
    }

    private void generateTwoIndependentClauses(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\te\t|\tx\ty");
        int numberOfLiterals = 5;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);
            Boolean e = input.get(4);

            StringBuilder sb = new StringBuilder();
            input.forEach(bol -> sb.append(ExperimentsTool.booleanToZeroOne(bol)).append("\t"));
            sb.append("|\t");

            Boolean x = (a && e && c);
            Boolean y = (!b && !d);
            sb.append(ExperimentsTool.booleanToZeroOne(x)).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne(y));
            System.out.println(sb);
        });
    }

    private void generateThreeIndependentClauses(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\te\t|\tx\ty\tz");
        int numberOfLiterals = 5;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);
            Boolean e = input.get(4);

            StringBuilder sb = new StringBuilder();
            input.forEach(bol -> sb.append(ExperimentsTool.booleanToZeroOne(bol)).append("\t"));
            sb.append("|\t");

            Boolean x = (a && e && c);
            Boolean y = (!b && !d);
            Boolean z = !(b && d) || (c && a);
            sb.append(ExperimentsTool.booleanToZeroOne(x)).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne(y)).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne(z));
            System.out.println(sb);
        });
    }

    private void generateOr2Clauses(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\t|\tx");
        int numberOfLiterals = 4;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);

            StringBuilder sb = new StringBuilder();
            input.forEach(bol -> sb.append(ExperimentsTool.booleanToZeroOne(bol)).append("\t"));
            sb.append("|\t");

            Boolean output = (a && !b && c) || (b && !a && !d);
            sb.append(ExperimentsTool.booleanToZeroOne(output));
            System.out.println(sb);
        });
    }

    private void generateXor2(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\t|\tx");
        int numberOfLiterals = 2;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean output = generateXor(input);

            StringBuilder sb = new StringBuilder();
            input.forEach(b -> sb.append(ExperimentsTool.booleanToZeroOne(b)).append("\t"));
            sb.append("|\t");

            sb.append(ExperimentsTool.booleanToZeroOne(output));
            System.out.println(sb);
        });
    }

    private void generateXor2Reversed(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\t|\tx\ty");
        int numberOfLiterals = 2;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = ExperimentsTool.longBitToBooleanList(i, numberOfLiterals);
            Boolean output = generateXor(input);

            StringBuilder sb = new StringBuilder();
            input.forEach(b -> sb.append(ExperimentsTool.booleanToZeroOne(b)).append("\t"));
            sb.append("|\t");

            sb.append(ExperimentsTool.booleanToZeroOne(output)).append("\t");
            sb.append(ExperimentsTool.booleanToZeroOne(!output));
            System.out.println(sb);
        });
    }



    private Boolean generateXor(List<Boolean> input) {
        return 1 == input.stream().filter(b -> b).count();
    }

    private List<Boolean> generateList(int numberOfLiterals, RandomGeneratorImpl random) {
        return IntStream.range(0, numberOfLiterals).mapToObj(idx -> random.nextDouble() > 0.5).collect(Collectors.toCollection(ArrayList::new));
    }


}
