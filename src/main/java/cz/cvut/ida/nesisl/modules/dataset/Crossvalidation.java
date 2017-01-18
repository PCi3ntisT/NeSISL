package main.java.cz.cvut.ida.nesisl.modules.dataset;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.application.Main;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by EL on 14.8.2016.
 */
public class Crossvalidation {

    private final List<Dataset> datasets;
    private final File originalFile;
    private int fold;

    private Crossvalidation(List<Dataset> datasets, File file) {
        this.datasets = datasets;
        this.originalFile = file;
    }

    public Dataset nextSplit() {
        synchronized (datasets) {
            Dataset selected = datasets.get(fold);
            List<Map<Fact, Value>> test = selected.getTrainRawData();

            List<Map<Fact, Value>> others = new ArrayList<>();
            for (int îdx = 0; îdx < datasets.size(); îdx++) {
                if (fold == îdx) {
                    continue;
                }
                others.addAll(datasets.get(îdx).getTrainRawData());
            }

            Dataset split = new DatasetImpl(selected.getInputFactOrder(), selected.getOutputFactOrder(), others, test, selected.getOriginalFile(),selected.getClassAttribute());
            fold = (fold + 1) % datasets.size();
            return split;
        }
    }

    public Dataset getDataset(int testFoldIdx) {
            Dataset selected = datasets.get(testFoldIdx);
            List<Map<Fact, Value>> test = selected.getTrainRawData();

            List<Map<Fact, Value>> others = new ArrayList<>();
            for (int îdx = 0; îdx < datasets.size(); îdx++) {
                if (testFoldIdx == îdx) {
                    continue;
                }
                others.addAll(datasets.get(îdx).getTrainRawData());
            }

            Dataset split = new DatasetImpl(selected.getInputFactOrder(), selected.getOutputFactOrder(), others, test, selected.getOriginalFile(),selected.getClassAttribute());
            return split;
    }

    public File getOriginalFile() {
        return originalFile;
    }

    public static Crossvalidation createStratified(Dataset dataset, RandomGeneratorImpl randomGenerator, Integer numberOfFolds) {
        synchronized (dataset) {
            System.out.println("creating different random generator for crossvalidation splits (with seed 187372 in order to be consistent with earlier setting fold by fold) - change in the future");
            randomGenerator = new RandomGeneratorImpl(Main.SIGMA,Main.MU,187372);
            List<List<Map<Fact, Value>>> folds = new ArrayList<>();
            IntStream.range(0,numberOfFolds).forEach(i -> folds.add(new ArrayList<>()));
            List<List<Map<Fact, Value>>> splitted = splitAccordingToResults(dataset);
            final RandomGeneratorImpl finalRandomGenerator = randomGenerator;
            splitted.forEach(group -> {
                if (!group.isEmpty()) {
                    Collections.shuffle(group, finalRandomGenerator.getRandom());
                    int idx = 0;
                    for (Map<Fact,Value> sample : group){
                        folds.get(idx % folds.size()).add(sample);
                        idx++;
                    }
                }
            });
            List<Dataset> datasets = new ArrayList<>();
            folds.forEach(sampleSet -> datasets.add(new DatasetImpl(dataset.getInputFactOrder(),dataset.getOutputFactOrder(),sampleSet,dataset.getOriginalFile(),dataset.getClassAttribute())));

            /*System.out.println("folds size");
            folds.forEach(f -> System.out.println("\t" + f.size()));
            System.out.println();*/

            return new Crossvalidation(datasets,dataset.getOriginalFile());
        }
    }

    private static List<List<Map<Fact, Value>>> splitAccordingToResults(Dataset dataset) {
        synchronized (dataset) {
            return dataset
                    .getTrainRawData()
                    .stream()
                    .collect(Collectors.groupingBy(e -> dataset.cannonicalOutput(e), Collectors.toCollection(ArrayList::new)))
                    .values()
                    .stream()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }
}
