package main.java.cz.cvut.ida.nesisl.modules.experiments;

import main.java.cz.cvut.ida.nesisl.api.data.Sample;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.SampleImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.Value;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        List<Operator> l = new ArrayList<>();
        l.add(Operator.AND);
        l.add(Operator.OR);
        l.add(Operator.XOR);
        //l.add(Operator.IMPLICATION);
        PropositionalFormulaeGenerator prop = new PropositionalFormulaeGenerator(4, 4, 4, l);
        File target = new File("." + File.separator + "experiments" + File.separator + "artificial");
        prop.generateAndStoreToFolder(target, 600, 0.2);
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
        List<Formula> formulae = generate(maxNumberOfGenerated);
        List<Formula> allSpace = generateNegatedFormulae(formulae);
        List<Pair<Formula, List<Sample>>> datasets = generateDatasets(allSpace);
        List<Pair<Formula, List<Sample>>> fileteredDatasets = filterDatasets(datasets, minimalTresholdForEachClass);
        storeToFolder(folder, fileteredDatasets);
    }

    private void storeToFolder(File folder, List<Pair<Formula, List<Sample>>> fileteredDatasets) {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        fileteredDatasets.forEach(p -> storeToFolder(p.getLeft(), p.getRight(), folder));
    }

    private void storeToFolder(Formula formula, List<Sample> samples, File parent) {
        File folder = new File(parent.getAbsoluteFile() + File.separator + formula.getScore());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        //folder.length()
        int children = folder.listFiles().length;
        File target = new File(parent.getAbsoluteFile() + File.separator + formula.getScore() + File.separator + (children + 1));

        if (!target.exists()) {
            target.mkdirs();
        }
        File formulaDescription = new File(target + File.separator + "formula.txt");
        File data = new File(target + File.separator + "data.txt");

        try {
            FileWriter formulaWriter = new FileWriter(formulaDescription);
            formulaWriter.write(formula.toString());
            formulaWriter.close();

            FileWriter dataWriter = new FileWriter(data);
            dataWriter.write(DatasetImpl.TRAIN_TOKEN + "\n");
            for (Sample sample : samples) {
                dataWriter.write(sample + "\n");
            }

            dataWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Pair<Formula, List<Sample>>> filterDatasets(List<Pair<Formula, List<Sample>>> datasets, double minimalTresholdForEachClass) {
        return datasets.parallelStream().filter(pair -> filterFormula(pair.getRight(), minimalTresholdForEachClass)).collect(Collectors.toCollection(ArrayList::new));
    }

    private Boolean filterFormula(List<Sample> samples, double minimalTresholdForEachClass) {
        Predicate<? super Sample> predicate = (sample) -> Math.abs(sample.getOutput().get(0).getValue() - 1.0) < 0.00001;
        long numberOfPositives = samples.parallelStream().filter(predicate).count();
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
        Stream<Pair<Formula, List<Sample>>> stream = formulae.parallelStream().map(formula ->
                        new Pair<Formula, List<Sample>>(formula,
                                inputs.stream().map(p -> evaluateInputs(formula, p.getLeft(), p.getRight())).collect(Collectors.toCollection(ArrayList::new)))
        );
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
        }

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

        if (sameOperator) {
            return first.getWidth() + second.getWidth() <= maximalNumberOfFormulasInFormula;
        }
        return maxDepth < maximalDepth;
    }

    private List<Formula> generateSuccessors(Formula first, Formula second) {
        return operators.parallelStream().map(operator -> generateSuccessor(operator, first, second)).collect(Collectors.toCollection(ArrayList::new));
    }

    private Formula generateSuccessor(Operator operator, Formula first, Formula second) {
        return new Formula(first, second, operator);
    }

    private List<Formula> initializeClauses(int nubmerOfAtoms) {
        return IntStream.range(0, nubmerOfAtoms).mapToObj(idx -> new Formula(new Literal(idx))).collect(Collectors.toCollection(ArrayList::new));
    }
}
