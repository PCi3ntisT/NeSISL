package main.java.cz.cvut.ida.nesisl.modules.dataset;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.data.Value;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.AttributeProprety;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by EL on 18.10.2016.
 */
public class MultiRepresentationDataset {

    private final Map<Map<Fact, Value>, List<String>> mappingExampleToString;
    private final List<AttributeProprety> attributes;
    private final List<List<String>> examples;
    private final File datasetFile;
    private final boolean normalize;
    private final Dataset nesislDataset;
    private final List<Pair<String, Set<String>>> ambigious;

    public MultiRepresentationDataset(List<AttributeProprety> attributes, List<List<String>> examples, File file, boolean normalize, Dataset nesislDataset, Map<Map<Fact, Value>, List<String>> mappingExampleToString, List<Pair<String, Set<String>>> ambigious) {
        this.attributes = attributes;
        this.examples = examples;
        this.datasetFile = file;
        this.normalize = normalize;
        this.nesislDataset = nesislDataset;
        this.mappingExampleToString = mappingExampleToString;
        this.ambigious = ambigious;
    }

    public Map<Map<Fact, Value>, List<String>> getMappingExampleToString() {
        return Collections.unmodifiableMap(mappingExampleToString);
    }

    public List<AttributeProprety> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public List<List<String>> getExamples() {
        return examples;
    }

    public File getDatasetFile() {
        return datasetFile;
    }

    public boolean isNormalize() {
        return normalize;
    }

    public Dataset getNesislDataset() {
        return nesislDataset;
    }

    public static MultiRepresentationDataset create(List<AttributeProprety> attributes, List<List<String>> examples, File file, boolean normalize, Dataset nesislDataset, Map<Map<Fact, Value>, List<String>> mappingExampleToString,List<Pair<String, Set<String>>> ambigious) {
        return new MultiRepresentationDataset(attributes,examples,file,normalize,nesislDataset,mappingExampleToString,ambigious);
    }

    public List<Pair<String, Set<String>>> getAmbigious() {
        return Collections.unmodifiableList(ambigious);
    }
}
