package main.java.cz.cvut.ida.nesisl.modules.experiments.generator;

import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.SampleImpl;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.modules.experiments.ExperimentsTool;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import main.java.cz.cvut.ida.nesisl.modules.tool.Triple;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by EL on 25.3.2016.
 */
public class PropositionalFormulaeGenerator {

    private final int numberOfAtoms;
    private final int maximalNumberOfFormulasInFormula;
    private final int maximalDepth;
    private final List<Operator> operators;

    public static void main(String[] args) {
        List<Operator> selectedOperators = new ArrayList<>();
        selectedOperators.add(Operator.AND);
        selectedOperators.add(Operator.OR);
        selectedOperators.add(Operator.XOR);
        //l.add(Operator.IMPLICATION);
        PropositionalFormulaeGenerator prop = new PropositionalFormulaeGenerator(7, 6, 4, selectedOperators);
        File target = new File("." + File.separator + "experiments" + File.separator + "artificial");
        prop.generateAndStoreToFolder(target, 11000, 0.2);
    }

    public PropositionalFormulaeGenerator(int numberOfAtoms, int maximalNumberOfFormulasInFormula, int maximalDepth, List<Operator> operators) {
        this.numberOfAtoms = numberOfAtoms;
        this.maximalNumberOfFormulasInFormula = maximalNumberOfFormulasInFormula;
        this.maximalDepth = maximalDepth;
        this.operators = operators;
    }

    public void generateAndStoreToFolder(File folder) {// folder, min number of zero and ones
        generateAndStoreToFolder(folder, Integer.MAX_VALUE, 0.0d);
    }

    public void generateAndStoreToFolder(File folder, int maxNumberOfGenerated, double minimalTresholdForEachClass) {// folder, min number of zero and ones
        System.out.println("generating datasets");
        List<Formula> formulae = generate(maxNumberOfGenerated);

        System.out.println("generating negated");
        List<Formula> allSpace = generateNegatedFormulae(formulae);

        System.out.println("generating dataset");
        List<Pair<Formula, List<Sample>>> datasets = generateDatasets(allSpace);

        System.out.println("threshold filter");
        List<Pair<Formula, List<Sample>>> filteredDatasets = filterDatasets(datasets, minimalTresholdForEachClass);

        System.out.println("filtering same outputs");
        List<Pair<Formula, List<Sample>>> finalDatasets = filterSameOutputs(filteredDatasets);
        System.out.println("final size\t" + finalDatasets.size());


        System.out.println("storing");
        storeToFolder(folder, finalDatasets);

        System.out.println("done");
    }

    private List<Pair<Formula, List<Sample>>> filterSameOutputs(List<Pair<Formula, List<Sample>>> datasets) {
        List<Pair<Formula, List<Sample>>> result = new ArrayList<>();
        Set<String> alreadyIn = new HashSet<>();
        for (Pair<Formula, List<Sample>> record : datasets) {
            String cannonic = canonicalString(record.getRight());
            if (!alreadyIn.contains(cannonic)) {
                alreadyIn.add(cannonic);
                result.add(record);
            }
        }
        return result;
    }

    private String canonicalString(List<Sample> list) {
        StringBuilder sb = new StringBuilder();
        Comparator<? super Sample> comparator = (s1, s2) -> {
            for (int idx = 0; idx < s1.getInput().size(); idx++) {
                Double v1 = s1.getInput().get(idx).getValue();
                Double v2 = s2.getInput().get(idx).getValue();
                if (!Tools.isZero(Math.abs(v1 - v2))) {
                    return Double.compare(v1, v2);
                }
            }
            return 0;
        };
        Collections.sort(list, comparator);
        list.forEach(sample -> sb.append(outputToString(sample)).append("|"));
        return sb.toString();
    }

    private String outputToString(Sample sample) {
        return sample.getOutput().stream().map(value -> value.getValue() + ",")
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString().trim();
    }

    private void storeToFolder(File folder, List<Pair<Formula, List<Sample>>> filteredDatasets) {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        StringBuilder formulaeOverview = new StringBuilder();

        List<Triple<Integer, Long, Formula>> cnfList = new ArrayList<>();
        List<Triple<Integer, Long, Formula>> weightedList = new ArrayList<>();
        List<Triple<Integer, Long, Formula>> expList = new ArrayList<>();

        IntStream.range(0, filteredDatasets.size()).forEach(idx -> {
            Pair<Formula, List<Sample>> p = filteredDatasets.get(idx);
            Triple<Long, Long, Long> scores = computeScores(p.getLeft());
            storeToFolder(p.getLeft(), p.getRight(), scores, folder, idx);
            formulaeOverview.append(idx + "\t" + scores.getK() + "\t" + scores.getT() + "\t" + scores.getW() + "\t" + p.getLeft() + "\n");

            cnfList.add(new Triple<>(idx, scores.getK(), p.getLeft()));
            weightedList.add(new Triple<>(idx, scores.getT(), p.getLeft()));
            expList.add(new Triple<>(idx, scores.getW(), p.getLeft()));
        });

        try {
            File description = new File(folder.getAbsolutePath() + File.separator + "description.txt");
            FileWriter formulaWriter = new FileWriter(description);
            formulaWriter.write(formulaeOverview.toString());
            formulaWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        storeSorteredScores(cnfList, folder, "cnf.txt");
        storeSorteredScores(expList, folder, "exp.txt");
        storeSorteredScores(weightedList, folder, "weighted.txt");
    }

    private void storeSorteredScores(List<Triple<Integer, Long, Formula>> list, File folder, String fileName) {
        Comparator<? super Triple<Integer, Long, Formula>> comparator = (t1, t2) -> {
            if (t1.getT() == t2.getT()) {
                return t1.getK().compareTo(t2.getK());
            }
            return t1.getT().compareTo(t2.getT());
        };
        Collections.sort(list, comparator);
        try {
            File file = new File(folder + File.separator + fileName);
            FileWriter formulaWriter = new FileWriter(file);
            for (Triple<Integer, Long, Formula> triple : list) {
                formulaWriter.write(triple.getK() + "\t" + triple.getT() + "\t" + triple.getW() + "\n");
            }
            formulaWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Triple<Long, Long, Long> computeScores(Formula formula) {
        Long cnf = computeCNFScore(formula);
        Long weighted = computeWeightedScore(formula);
        Long exp = computeExpScore(formula);
        return new Triple<>(cnf, weighted, exp);
    }

    private Long computeCNFScore(Formula formula) {
        File tmp = storeToTmpFile(formula);
        return convertAndRetrieveCNF(tmp);
    }

    private Long convertAndRetrieveCNF(File file) {
        ProcessBuilder builder = new ProcessBuilder("python", ".." + File.separator + "PBL-master" + File.separator + "include" + File.separator + "convertor.py", file.getAbsolutePath());
        Process process = null;
        try {
            builder = builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            builder = builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = builder.start();
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            return cnfToScore(line.trim());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Error during computing CNF - CNF value not found in computed output.");
    }

    private Long cnfToScore(String cnf) {
        return (long) cnf.split("&").length;
    }


    private File storeToTmpFile(Formula formula) {
        long timeStamp = System.nanoTime();
        String threadName = Thread.currentThread().getName();
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("tempfile" + threadName + "_" + timeStamp, ".tmp");
            FileWriter formulaWriter = new FileWriter(tmpFile);
            String expressToken = "Main_Exp : ";
            formulaWriter.write(expressToken + formula.toString());
            formulaWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tmpFile;
    }

    private Long computeWeightedScore(Formula formula) {
        return formula.getWeightedScore();
    }

    private Long computeExpScore(Formula formula) {
        return formula.getScore();
    }

    private void storeToFolder(Formula formula, List<Sample> samples, Triple<Long, Long, Long> scores, File parent, int idx) {
        File folder = new File(parent.getAbsoluteFile() + File.separator + idx);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        //folder.length()
        //int children = folder.listFiles().length;
        //File target = new File(parent.getAbsoluteFile() + File.separator + formula.getScore() + File.separator + (children + 1));
        File target = folder;

        if (!target.exists()) {
            target.mkdirs();
        }
        File formulaDescription = new File(target + File.separator + "formula.txt");
        File data = new File(target + File.separator + "data.txt");

        try {
            FileWriter formulaWriter = new FileWriter(formulaDescription);
            formulaWriter.write(formula.toString() + "\n");
            formulaWriter.write("order :\t" + idx + "\n");
            formulaWriter.write("CNF :\t" + scores.getK() + "\n");
            formulaWriter.write("weighted :\t" + scores.getT() + "\n");
            formulaWriter.write("exp :\t" + scores.getW() + "\n");
            formulaWriter.close();

            FileWriter dataWriter = new FileWriter(data);
            dataWriter.write(DatasetImpl.TRAIN_TOKEN + "\n");
            dataWriter.write(input(samples.get(0)) + DatasetImpl.INPUT_OUTPUT_DELIMITER + output(samples.get(0)) + "\n");
            for (Sample sample : samples) {
                dataWriter.write(sample + "\n");
            }
            dataWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String input(Sample sample) {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, sample.getInput().size()).forEach(idx -> sb.append("\t").append(Literal.literals.charAt(idx)));
        return sb.toString();
    }

    private String output(Sample sample) {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, sample.getOutput().size()).forEach(idx -> sb.append("\t").append(Literal.outputLiterals.charAt(idx)));
        return sb.toString();
    }

    private List<Pair<Formula, List<Sample>>> filterDatasets(List<Pair<Formula, List<Sample>>> datasets, double minimalTresholdForEachClass) {
        return datasets
                //.parallelStream()
                .stream()
                .filter(pair -> filterFormula(pair.getRight(), minimalTresholdForEachClass)).collect(Collectors.toCollection(ArrayList::new));
    }

    private Boolean filterFormula(List<Sample> samples, double minimalTresholdForEachClass) {
        Predicate<? super Sample> predicate = (sample) -> Tools.isZero(sample.getOutput().get(0).getValue() - 1.0);
        long numberOfPositives = samples
                //.parallelStream()
                .stream()
                .filter(predicate).count();
        double ration = numberOfPositives / (double) samples.size();
        return ration >= minimalTresholdForEachClass && ration <= (1 - minimalTresholdForEachClass);
    }

    private List<Formula> generateNegatedFormulae(List<Formula> formulae) {
        List<Formula> list = new ArrayList<>(formulae);

        for (Formula formula : formulae) {
            list.addAll(negatedPossibilities(formula));
        }

        return list;
    }

    private List<Formula> negatedPossibilities(Formula formula) {
        if (formula.isTerminal()) {
            List<Formula> list = new ArrayList<>();
            list.add(new Formula(formula));
            return list;
        }

        List<Formula> list = new ArrayList<>();
        // only negation for first layer
        Formula negatedFirst = new Formula(formula.getFirst());
        Formula negatedSecond = new Formula(formula.getSecond());
        list.add(new Formula(formula.getFirst(), negatedSecond, formula.getOperator()));
        list.add(new Formula(negatedFirst, formula.getSecond(), formula.getOperator()));
        list.add(new Formula(negatedFirst, negatedSecond, formula.getOperator()));

        return list;
    }

    private List<Pair<Formula, List<Sample>>> generateDatasets(List<Formula> formulae) {
        List<Pair<List<Boolean>, List<Value>>> inputs = IntStream.range(0, (int) Math.pow(2, numberOfAtoms)).mapToObj(idx -> generateInput(idx)).collect(Collectors.toCollection(ArrayList::new));
        Stream<Pair<Formula, List<Sample>>> stream = formulae
                //.parallelStream()
                .stream()
                .map(formula ->
                        new Pair<Formula, List<Sample>>(formula,
                                inputs.stream().map(p -> evaluateInputs(formula, p.getLeft(), p.getRight())).collect(Collectors.toCollection(ArrayList::new))));
        return stream.collect(Collectors.toCollection(ArrayList::new));
    }

    private Sample evaluateInputs(Formula formula, List<Boolean> bools, List<Value> values) {
        ArrayList<Value> outputList = new ArrayList<>();
        Boolean output = evaluate(formula, bools);
        outputList.add(new Value(output ? 1.0 : 0.0));
        return new SampleImpl(values, outputList);
    }

    private Pair<List<Boolean>, List<Value>> generateInput(int idx) {
        List<Boolean> list = ExperimentsTool.intBitToBooleanList(idx, numberOfAtoms);
        List<Value> values = list.stream().map(i -> new Value(i ? 1.0 : 0.0)).collect(Collectors.toCollection(ArrayList::new));
        return new Pair<>(list, values);
    }


    private Boolean evaluate(Formula formula, List<Boolean> input) {
        return formula.isTrue(input);
    }

    private List<Formula> generate(int maxNumberOfGenerated) {
        List<Formula> generated = initializeClauses(numberOfAtoms);
        int previousLength = 1;

        while (previousLength > 0 && generated.size() < maxNumberOfGenerated) {
            List<Formula> successors = new ArrayList<>();
            for (int alreadyKnown = 0; alreadyKnown < generated.size(); alreadyKnown++) {
                Formula first = generated.get(alreadyKnown);
                for (int newlyAdded = Math.max(alreadyKnown + 1, previousLength); newlyAdded < generated.size(); newlyAdded++) {
                    Formula second = generated.get(newlyAdded);
                    operators.forEach(operator -> addOperatorIfPossible(operator, first, second, successors));
                }
            }
            previousLength = successors.size();
            generated.addAll(successors);
            System.out.println(generated.size());
        }
        generated = generated.subList(0, maxNumberOfGenerated);
        return generated;
    }

    private void addOperatorIfPossible(Operator operator, Formula first, Formula second, List<Formula> successors) {
        if (canGenerateSuccessor(operator, first, second)) {
            successors.add(new Formula(first, second, operator));
        }
    }

    private boolean canGenerateSuccessor(Operator operator, Formula first, Formula second) {
        if (!second.isTerminal() && second.getFirst() == first && second.getOperator() == operator) {
            return false;
        }
        switch (operator) {
            case OR:
                return canGenerateOr(first, second);
            case AND:
                return canGenerateAnd(first, second);
            case XOR:
                return canGenerateXor(first, second);
            case IMPLICATION:
                return canGenerateImplication(first, second);
            default:
                return false;
        }
    }

    private boolean canGenerateXor(Formula first, Formula second) {
        int maxDepth = Math.max(first.getDepth(), second.getDepth());
        return maxDepth < maximalDepth;
    }

    private boolean canGenerateImplication(Formula first, Formula second) {
        int maxDepth = Math.max(first.getDepth(), second.getDepth());
        return maxDepth < maximalDepth;
    }

    private boolean canGenerateAnd(Formula first, Formula second) {
        int maxDepth = Math.max(first.getDepth(), second.getDepth());
        boolean sameOperator = (first.getOperator() == Operator.AND && second.getOperator() == Operator.AND) ||
                (first.isTerminal() && second.getOperator() == Operator.AND) ||
                (second.isTerminal() && first.getOperator() == Operator.AND);

        if (sameOperator) {
            return first.getWidth() + second.getWidth() <= maximalNumberOfFormulasInFormula;
        }
        return maxDepth < maximalDepth;

    }

    private boolean canGenerateOr(Formula first, Formula second) {
        int maxDepth = Math.max(first.getDepth(), second.getDepth());
        boolean sameOperator = (first.getOperator() == Operator.OR && second.getOperator() == Operator.OR) ||
                (first.isTerminal() && second.getOperator() == Operator.OR) ||
                (second.isTerminal() && first.getOperator() == Operator.OR);

        if (sameOperator && !second.isTerminal()
                && (second.getFirst() == first || (!second.isNegation() && second.getSecond() == first))) {
            // duplicates
            return false;
        }

        if (sameOperator) {
            return first.getWidth() + second.getWidth() <= maximalNumberOfFormulasInFormula;
        }
        return maxDepth < maximalDepth;
    }

    private List<Formula> generateSuccessors(Formula first, Formula second) {
        return operators
                //.parallelStream()
                .stream()
                .map(operator -> generateSuccessor(operator, first, second)).collect(Collectors.toCollection(ArrayList::new));
    }

    private Formula generateSuccessor(Operator operator, Formula first, Formula second) {
        return new Formula(first, second, operator);
    }

    private List<Formula> initializeClauses(int nubmerOfAtoms) {
        return IntStream.range(0, nubmerOfAtoms).mapToObj(idx -> new Formula(new Literal(idx))).collect(Collectors.toCollection(ArrayList::new));
    }
}
