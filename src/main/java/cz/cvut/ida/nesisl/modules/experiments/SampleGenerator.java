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


        //sample.generateXor2(10, random);
        //sample.generateXor3(20, random);
        //sample.generateDNF4(50, random);
        sample.generateAnd3(12, random);
    }

    private void generateAnd3(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\t|\tx\ty");
        int numberOfLIterals = 3;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = longBitToBooleanList(i, numberOfLIterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);

            StringBuilder sb = new StringBuilder();
            input.forEach(bo -> sb.append(booleanToZeroOne(bo)).append("\t"));
            sb.append("|\t");

            sb.append(booleanToZeroOne(a && b && c)).append("\t");
            sb.append(booleanToZeroOne(!a && !b && !c));
            System.out.println(sb);
        });
    }

    private void generateDNF4(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\td\t|\tx\ty\tz");
        int numberOfLIterals = 4;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = longBitToBooleanList(i, numberOfLIterals);
            Boolean a = input.get(0);
            Boolean b = input.get(1);
            Boolean c = input.get(2);
            Boolean d = input.get(3);

            StringBuilder sb = new StringBuilder();
            input.forEach(bo -> sb.append(booleanToZeroOne(bo)).append("\t"));
            sb.append("|\t");


            sb.append(booleanToZeroOne((a && b && c && d))).append("\t");
            sb.append(booleanToZeroOne((a && b) || (c && d))).append("\t");
            sb.append(booleanToZeroOne(!a && !b));
            System.out.println(sb);
        });
    }

    private void generateXor3(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\tc\t|\tx");
        int numberOfLIterals = 3;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = longBitToBooleanList(i, numberOfLIterals);
            Boolean output = generateXor(input);

            StringBuilder sb = new StringBuilder();
            input.forEach(b -> sb.append(booleanToZeroOne(b)).append("\t"));
            sb.append("|\t");

            sb.append(booleanToZeroOne(output));
            System.out.println(sb);
        });
    }

    private void generateXor2(int numberOfSamples, RandomGeneratorImpl random) {
        System.out.println("a\tb\t|\tx");
        int numberOfLIterals = 2;
        LongStream.range(0, numberOfSamples).forEach(i -> {
            List<Boolean> input = longBitToBooleanList(i, numberOfLIterals);
            Boolean output = generateXor(input);

            StringBuilder sb = new StringBuilder();
            input.forEach(b -> sb.append(booleanToZeroOne(b)).append("\t"));
            sb.append("|\t");

            sb.append(booleanToZeroOne(output));
            System.out.println(sb);
        });
    }


    private List<Boolean> longBitToBooleanList(long number, int numberOfLiterals) {
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

    private String booleanToZeroOne(Boolean bool) {
        return bool ? "1" : "0";
    }

    private Boolean generateXor(List<Boolean> input) {
        return 1 == input.stream().filter(b -> b).count();
    }

    private List<Boolean> generateList(int numberOfLiterals, RandomGeneratorImpl random) {
        return IntStream.range(0, numberOfLiterals).mapToObj(idx -> random.nextDouble() > 0.5).collect(Collectors.toCollection(ArrayList::new));
    }


}
