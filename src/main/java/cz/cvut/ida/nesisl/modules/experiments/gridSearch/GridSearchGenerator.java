package main.java.cz.cvut.ida.nesisl.modules.experiments.gridSearch;

import main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation.CascadeCorrelationSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.dynamicNodeCreation.DNCSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.regent.RegentSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGenSettings;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by EL on 5.4.2016.
 */
public class GridSearchGenerator {

    private final Map<String, List<String>> properties;
    private final File folder;
    private final String settingName;

    public GridSearchGenerator(Map<String, List<String>> map, File folder, String settingName) {
        this.properties = map;
        this.folder = folder;
        this.settingName = settingName;
    }

    public static void main(String[] args) {
        List<GridSearchGenerator> grid = new ArrayList<>();

        String root = "experiments" + File.separator + "settings" + File.separator;
        grid.add(generateWLSSearch(root + "WLS", "wlsSetting.txt"));
        grid.add(generateKBANNSearch(root + "KBANN", "kbannSetting.txt"));
        grid.add(generateDNCSearch(root + "DNC", "DNCSetting.txt"));
        grid.add(generateCascadeCorrelationSearch(root + "CasCor", "cascorSetting.txt"));
        grid.add(generateTopGenSearch(root + "TopGen", "TopGenSetting.txt"));
        grid.add(generateREGENTSearch(root + "REGENT", "REGENTSetting.txt"));

        grid.forEach(search -> search.generateAndStore());
    }

    private static GridSearchGenerator generateCascadeCorrelationSearch(String folder, String settingName) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(CascadeCorrelationSetting.POOL_SIZE_LIMIT_TOKEN, orderedList("4")); //"1", "4", "8"));
        map.put(CascadeCorrelationSetting.EPSILON_CONVERGENT_TOKEN, orderedList("0.1")); //, "0.3"));
        map.put(CascadeCorrelationSetting.HIDDEN_NODE_LIMIT_TOKEN, orderedList("200"));
        map.put(CascadeCorrelationSetting.SHORT_TIME_WINDOW_TOKEN, orderedList("10")); //"10", "20"));
        map.put(CascadeCorrelationSetting.LONG_TIME_WINDOW_TOKEN, orderedList("30")); //"30", "60"));
        map.put(CascadeCorrelationSetting.CANDIDATE_ITERATION_LEARNING_LIMIT_TOKEN, orderedList("2500"));
        return new GridSearchGenerator(map, new File(folder), settingName);
    }

    private static GridSearchGenerator generateREGENTSearch(String folder, String settingName) {
        Map<String, List<String>> map = new HashMap<>();
        // regent part
        map.put(RegentSetting.TOURNAMENT_SIZE_TOKEN, orderedList("2")); //"2", "4"));
        map.put(RegentSetting.POPULATION_SIZE_TOKEN, orderedList("40")); //"20", "50", "100"));
        map.put(RegentSetting.MUTATION_OF_POPULATION_TOKEN, orderedList("60")); //"10", "30", "60"));
        map.put(RegentSetting.MUTATION_OF_CROSSOVERS_TOKEN, orderedList("20")); //"10", "20", "40"));
        map.put(RegentSetting.NODE_DELETION_PROBABILITY_TOKEN, orderedList("0.5")); //"0.1", "0.3", "0.7"));
        map.put(RegentSetting.CROSSOVER_CHILDREN_TOKEN, orderedList("20")); //"10", "20", "50"));
        map.put(RegentSetting.ELITES_TOKEN, orderedList("1")); //"1", "2"));
        map.put(RegentSetting.EDGE_WEIGHT_CROSSOVER_LIMIT_TOKEN, orderedList("0.2"));
        map.put(RegentSetting.FITNESS_LIMIT_TOKEN, orderedList("40000"));

        map.put(RegentSetting.SHORT_TIME_WINDOW_TOKEN, orderedList("10"));
        map.put(RegentSetting.LONG_TIME_WINDOW_TOKEN, orderedList("30"));
        map.put(RegentSetting.EPSILON_CONVERGENT_TOKEN, orderedList("0.1"));


        // TopGen part
        map.put(TopGenSettings.EPSILON_LIMIT_TOKEN, orderedList("0.1")); //"0.01", "0.1", "0.3"));
        map.put(TopGenSettings.OMEGA_TOKEN, orderedList("4.0")); //"2.0", "4.0", "6.0"));
        map.put(TopGenSettings.PERTURBATION_TOKEN, orderedList("0.3")); //"2.0", "4.0", "6.0"));
        map.put(TopGenSettings.SUCCESSORS_GENERATED_LIMIT_TOKEN, orderedList("10")); //"5", "10", "25"));
        map.put(TopGenSettings.OPEN_LIST_LIMIT_TOKEN, orderedList("60")); //"30", "65", "100"));
        map.put(TopGenSettings.SHORT_TIME_WINDOW_TOKEN, orderedList("10")); //"10", "20"));
        map.put(TopGenSettings.LONG_TIME_WINDOW_TOKEN, orderedList("30")); //"30", "60"));
        map.put(TopGenSettings.EPSILON_CONVERGENT_TOKEN, orderedList("0.1")); //"0.001", "0.01", "0.1", "0.3"));
        map.put(TopGenSettings.LEARNING_RATE_DECAY_TOKEN, orderedList("0.9")); //"0.1", "0.5", "0.8"));
        map.put(TopGenSettings.NODE_ACTIVATION_THRESHOLD_TOKEN, orderedList("0.15"));
        return new GridSearchGenerator(map, new File(folder),settingName);
    }

    private static GridSearchGenerator generateTopGenSearch(String folder, String settingName) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(TopGenSettings.EPSILON_LIMIT_TOKEN, orderedList("0.1")); //"0.01", "0.1", "0.3"));
        map.put(TopGenSettings.OMEGA_TOKEN, orderedList("4.0")); //"2.0", "4.0", "6.0"));
        map.put(TopGenSettings.SUCCESSORS_GENERATED_LIMIT_TOKEN, orderedList("10")); //"5", "10", "25"));
        map.put(TopGenSettings.OPEN_LIST_LIMIT_TOKEN, orderedList("60")); //"30", "65", "100"));
        map.put(TopGenSettings.SHORT_TIME_WINDOW_TOKEN, orderedList("10")); //"10", "20"));
        map.put(TopGenSettings.LONG_TIME_WINDOW_TOKEN, orderedList("30")); //"30", "60"));
        map.put(TopGenSettings.EPSILON_CONVERGENT_TOKEN, orderedList("0.1")); //"0.001", "0.01", "0.1", "0.3"));
        map.put(TopGenSettings.LEARNING_RATE_DECAY_TOKEN, orderedList("0.5")); //"0.1", "0.5", "0.8"));
        map.put(TopGenSettings.NODE_ACTIVATION_THRESHOLD_TOKEN, orderedList("0.15"));
        map.put(TopGenSettings.PERTURBATION_TOKEN, orderedList("0.3")); //"2.0", "4.0", "6.0"));
        return new GridSearchGenerator(map, new File(folder),settingName);
    }

    private static GridSearchGenerator generateDNCSearch(String folder, String settingName) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(DNCSetting.DELTA_T_TOKEN, orderedList("0.05"));
        map.put(DNCSetting.TIME_WINDOW_TOKEN, orderedList("100"));
        map.put(DNCSetting.CA_TOKEN, orderedList("0.001"));
        map.put(DNCSetting.CM_TOKEN, orderedList("0.01"));
        map.put(DNCSetting.HIDDEN_NODE_LIMIT_TOKEN, orderedList("200"));
        return new GridSearchGenerator(map, new File(folder),settingName);
    }

    private static GridSearchGenerator generateKBANNSearch(String folder, String settingName) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(KBANNSettings.OMEGA_TOKEN, orderedList("4.0")); //"2.0", "4.0", "6.0"));
        map.put(KBANNSettings.PERTURBATION_TOKEN, orderedList("0.3")); //"2.0", "4.0", "6.0"));
        return new GridSearchGenerator(map, new File(folder),settingName);
    }

    private static GridSearchGenerator generateWLSSearch(String folder, String settingName) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(WeightLearningSetting.EPSILON_CONVERGENT_TOKEN, orderedList("0.1")); //"0.1", "0.3"));
        map.put(WeightLearningSetting.EPOCH_TOKEN, orderedList("2500"));
        map.put(WeightLearningSetting.LEARNING_RATE_TOKEN, orderedList("0.7")); //"0.1", "0.4", "0.9"));
        map.put(WeightLearningSetting.MOMENTUM_ALPHA_TOKEN, orderedList("0.3")); //"0.01", "0.1", "0.3", "0.9"));
        map.put(WeightLearningSetting.SHORT_TIME_WINDOW_TOKEN, orderedList("10")); //"10", "20"));
        map.put(WeightLearningSetting.LONG_TIME_WINDOW_TOKEN, orderedList("30")); //"30", "60"));

        map.put(WeightLearningSetting.PENALTY_EPSILON_TOKEN, orderedList("0.0001")); //"0.1", "1", "10"));
        map.put(WeightLearningSetting.SLSF_THRESHOLD_TOKEN, orderedList("0.1")); //"0.01", "0.1", "0.3"));

        return new GridSearchGenerator(map, new File(folder),settingName);
    }

    private static List<String> orderedList(String... args) {
        if (null == args) {
            return new ArrayList<>();
        }
        Arrays.sort(args);
        return Arrays.asList(args);
    }


    public void generateAndStore() {
        List<Pair<String, List<String>>> list = properties.entrySet().stream().map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toCollection(ArrayList::new));
        String mark = "";
        generateAndStore(list, 0, mark, "", settingName);
    }

    private void generateAndStore(List<Pair<String, List<String>>> list, int propertyIdx, String mark, String contentSoFar, String folderName) {
        String delimiter = ":";
        if (propertyIdx >= list.size()) {
            store(folderName, mark, contentSoFar);
        } else {
            Pair<String, List<String>> possibilities = list.get(propertyIdx);
            IntStream.range(0, possibilities.getRight().size()).forEach(possibilityIdx -> {
                        String value = possibilities.getLeft() + " " + delimiter + " " + possibilities.getRight().get(possibilityIdx) + "\n" + contentSoFar;
                        generateAndStore(list, propertyIdx + 1, mark + "-" + possibilityIdx, value, folderName);
                    }
            );
        }
    }

    private void store(String fileName, String folderName, String content) {
        String parent = folder + File.separator + folderName;
        File parentFolder = new File(parent);
        if(!parentFolder.exists()){
            parentFolder.mkdirs();
        }
        File file = new File(parent + File.separator + fileName);
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

}
