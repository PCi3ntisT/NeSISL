package main.java.cz.cvut.ida.nesisl.modules.dataset;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
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
            List<Map<Fact, Value>> test = selected.getRawData();

            List<Map<Fact, Value>> others = new ArrayList<>();
            for (int îdx = 0; îdx < datasets.size(); îdx++) {
                if (fold == îdx) {
                    continue;
                }
                others.addAll(datasets.get(îdx).getRawData());
            }

            Dataset split = new DatasetImpl(selected.getInputFactOrder(), selected.getOutputFactOrder(), test, others, selected.getOriginalFile());
            fold = (fold + 1) % datasets.size();
            return split;
        }
    }

    public Dataset getDataset(int testFoldIdx) {
            Dataset selected = datasets.get(testFoldIdx);
            List<Map<Fact, Value>> test = selected.getRawData();

            List<Map<Fact, Value>> others = new ArrayList<>();
            for (int îdx = 0; îdx < datasets.size(); îdx++) {
                if (testFoldIdx == îdx) {
                    continue;
                }
                others.addAll(datasets.get(îdx).getRawData());
            }

            Dataset split = new DatasetImpl(selected.getInputFactOrder(), selected.getOutputFactOrder(), test, others, selected.getOriginalFile());
            return split;
    }

    public File getOriginalFile() {
        return originalFile;
    }

    public static Crossvalidation createStratified(Dataset dataset, RandomGeneratorImpl randomGenerator, Integer numberOfFolds) {
        synchronized (dataset) {
            List<List<Map<Fact, Value>>> folds = new ArrayList<>();
            IntStream.range(0,numberOfFolds).forEach(i -> folds.add(new ArrayList<>()));
            List<List<Map<Fact, Value>>> splitted = splitAccordingToResults(dataset);
            splitted.forEach(group -> {
                if (!group.isEmpty()) {
                    Collections.shuffle(group, randomGenerator.getRandom());
                    int idx = 0;
                    for (Map<Fact,Value> sample : group){
                        folds.get(idx % folds.size()).add(sample);
                        idx++;
                    }
                }
            });
            List<Dataset> datasets = new ArrayList<>();
            folds.forEach(sampleSet -> datasets.add(new DatasetImpl(dataset.getInputFactOrder(),dataset.getOutputFactOrder(),sampleSet,dataset.getOriginalFile())));
            return new Crossvalidation(datasets,dataset.getOriginalFile());
        }
    }

    private static List<List<Map<Fact, Value>>> splitAccordingToResults(Dataset dataset) {
        synchronized (dataset) {
            return dataset
                    .getRawData()
                    .stream()
                    .collect(Collectors.groupingBy(e -> dataset.cannonicalOutput(e), Collectors.toCollection(ArrayList::new)))
                    .values()
                    .stream()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }
}
