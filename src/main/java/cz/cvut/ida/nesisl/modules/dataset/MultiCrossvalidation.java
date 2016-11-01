package main.java.cz.cvut.ida.nesisl.modules.dataset;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import weka.core.Instances;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by EL on 24.10.2016.
 */
public class MultiCrossvalidation {

    private final MultiRepresentationDataset multiRepreDataset;
    private final Crossvalidation crossvalidation;

    public MultiCrossvalidation(MultiRepresentationDataset multiRepreDataset, Crossvalidation crossvalidation) {
        this.multiRepreDataset = multiRepreDataset;
        this.crossvalidation = crossvalidation;
    }

    public Dataset nextSplit(){
            return crossvalidation.nextSplit();
    }

    public Dataset getDataset(int testFoldIdx){
        return crossvalidation.getDataset(testFoldIdx);
    }

    public Instances getTrainWekaDataset(Dataset dataset){
        return getWekaDataset(dataset.getTrainRawData(), dataset);
    }

    public Instances getTestWekaDataset(Dataset dataset){
        return getWekaDataset(dataset.getRawTestData(), dataset);
    }

    private Instances getWekaDataset(List<Map<Fact, Value>> data, Dataset dataset) {
        Map<Map<Fact, Value>, List<String>> mapping = multiRepreDataset.getMappingExampleToString();
        List<List<String>> selectedExamples = data.stream()
                .filter(nesislSample -> mapping.containsKey(nesislSample))
                .map(nesislSample -> mapping.get(nesislSample))
                .collect(Collectors.toCollection(ArrayList::new));
        return DatasetImpl.createWekaDataset(multiRepreDataset.getAmbigious(),multiRepreDataset.getAttributes(), selectedExamples, multiRepreDataset.getDatasetFile(),multiRepreDataset.isNormalize());
    }

    public File getOriginalFile() {
        return multiRepreDataset.getDatasetFile();
    }

    public static MultiCrossvalidation createStratified(MultiRepresentationDataset dataset, RandomGeneratorImpl randomGenerator, int numberOfRepeats) {
        Crossvalidation crossvalidation = Crossvalidation.createStratified(dataset.getNesislDataset(),randomGenerator,numberOfRepeats);
        return new MultiCrossvalidation(dataset,crossvalidation);
    }
}
