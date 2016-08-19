package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.tool.RandomGenerator;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation.CascadeCorrelationSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation.CascadeCorrelation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.dynamicNodeCreation.DNCSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.dynamicNodeCreation.DynamicNodeCreation;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.KBANNSettings;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.MissingValueKBANN;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.neuralNetwork.weightLearning.WeightLearningSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.regent.Regent;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.regent.RegentSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.structuralLearningWithSelectiveForgetting.StructuralLearningWithSelectiveForgetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGen;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGenSettings;
import main.java.cz.cvut.ida.nesisl.modules.dataset.Crossvalidation;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Initable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Learnable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation.ExperimentResult;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.trepan.Trepan;
import main.java.cz.cvut.ida.nesisl.modules.trepan.TrepanResults;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by EL on 9.2.2016.
 */
public class Main {

    // note that, by default, if there are multiple (more than two class) to predict, softmax with crossentropy is used (better to modularize this ;) ); otherwise one output is used for prediciting the first class in data file
    // numeric attributes are not possible to predict in this version

    public static final boolean TREPAN_RUN = true;

    private Integer numberOfFolds = 10;

    public static void main(String arg[]) throws FileNotFoundException {
        //arg = new String[]{"DNC", "1", "./ruleExamples/sampleInput/sampleOne.txt", "./ruleExamples/sampleInput/wlsSettings.txt", "./ruleExamples/KBANN/sampleOne.txt"};
        //arg = new String[]{"SLF", "1", "./ruleExamples/sampleInput/xor.txt", "./ruleExamples/sampleInput/wlsSettings.txt", ""};
        //arg = new String[]{"SLF", "1", "./ruleExamples/sampleInput/xor.txt", "./ruleExamples/sampleInput/wlsSettings.txt", "./ruleExamples/sampleInput/SLFinput.txt"};
        if (arg.length < 4) {
            System.out.println("Not enought arguments. Right setting in form 'algorithmName numberOfRuns datasetFile weightLearningSettingFile [...]'. Possible algorithms KBANN, CasCor, DNC, SLSF, TopGen, REGENT; write as first argument to see more. TODO this MSG");
            System.exit(0);
        }

        double simga = 1d;
        double mu = 0.0d;
        int seed = 13;
        int numberOfRepeats = 0;
        try {
            numberOfRepeats = Integer.valueOf(arg[1]);
        } catch (Exception ex) {
            System.out.println("The second argument (number of repeats) must be integer.");
            System.out.println("Argument input instead '" + arg[1] + "'.");
            System.exit(0);
        }
        File datasetFile = new File(arg[2]);
        if (!datasetFile.exists()) {
            System.out.println("The third argument (datasetFile) does not exist.");
            System.out.println("Argument input instead '" + arg[2] + "'.");
            System.exit(0);
        }
        File wlsFile = new File(arg[3]);
        if (!wlsFile.exists()) {
            System.out.println("The fourth argument (weightLearningSettingFile) does not exist.");
            System.out.println("Argument input instead '" + arg[3] + "'.");
            System.exit(0);
        }
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);
        if (!"SLSF".equals(arg[0])) {
            wls = WeightLearningSetting.turnOffRegularization(wls);
        }


        // TODO NACITANI NORMALIZACE ZAJISTIT :) & zkontrolovat

        boolean normalize = true;
        Dataset dataset = DatasetImpl.parseDataset(datasetFile, normalize);
        RandomGeneratorImpl randomGenerator = new RandomGeneratorImpl(simga, mu, seed);

        Main main = new Main();
        switch (arg[0]) {
            case "KBANN":
                main.runKBANN(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "backprop":
                main.runBackprop(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "CasCor":
                main.runCasCor(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "DNC":
                main.runDNC(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "SLSF":
                main.runSLSF(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "TopGen":
                main.runTopGen(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "REGENT":
                main.runREGENT(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            default:
                System.out.println("Unknown algorithm '" + arg[0] + "'.");
                break;
        }
    }

    private void runAndStoreExperiments(Initable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, Crossvalidation crossval, File settingFile, WeightLearningSetting wls) {
        List<ExperimentResult> results = runExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, wls);
        ExperimentResult.storeResults(results, algName, crossval.getOriginalFile(), settingFile, wls);
    }

    private List<ExperimentResult> runExperiments(Initable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, Crossvalidation crossval, File settingFile, WeightLearningSetting wls) {
        return IntStream.range(0, numberOfRepeats)
                //.parallel()
                .mapToObj(idx -> {
                    ExperimentResult currentResult = new ExperimentResult(idx, algName, crossval.getOriginalFile(), settingFile, wls);
                    NeuralNetworkOwner alg = initialize.initialize();
                    currentResult.setInitNetwork(alg.getNeuralNetwork().getCopy());

                    Dataset dataset = crossval.getDataset(idx);

                    long start = System.currentTimeMillis();
                    NeuralNetwork learnedNetwork = learn.learn(alg, dataset);
                    long end = System.currentTimeMillis();

                    /*
                    Tools.printEvaluation(learnedNetwork, dataset);
                    System.out.println("\t" + learnedNetwork.getClassifier().getThreshold());
                    */

                    if (TREPAN_RUN) {
                        TrepanResults trepan = Trepan.create(learnedNetwork, dataset, algName, idx, currentResult.getMyAdress()).run();
                        currentResult.addExperiment(learnedNetwork, start, end, dataset, trepan);
                    } else {
                        currentResult.addExperiment(learnedNetwork, start, end, dataset);
                    }
                    return currentResult;
                }).collect(Collectors.toCollection(ArrayList::new));
    }

    private void runKBANN(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run KBANN use 'KBANN   #ofRepeats  datasetFile weightLearningSettingsFile  ruleFile    KBANNsetting    [ruleSpecificFile]'");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }

        String algName = "KBANN";
        File ruleFile = new File(arg[4]);
        File settingFile = new File(arg[5]);
        KBANNSettings kbannSettings = KBANNSettings.create(randomGenerator, settingFile);

        List<Pair<Integer, ActivationFunction>> specificRules = new ArrayList<>();

        Initable<KBANN> initialize = () -> KBANN.create(ruleFile, dataset, specificRules, kbannSettings);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        Learnable learn = (kbann, learningDataset) -> ((KBANN) kbann).learn(learningDataset, finalWls);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

    private void runBackprop(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run backprop only on KBANN use 'backprop   #ofRepeats  datasetFile weightLearningSettingsFile  ruleFile   '");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }

        String algName = "backprop";
        File ruleFile = new File(arg[4]);
        File settingFile = new File(arg[5]);
        KBANNSettings kbannSettings = new KBANNSettings(randomGenerator, 1.0, true);

        List<Pair<Integer, ActivationFunction>> specificRules = new ArrayList<>();

        Initable<KBANN> initialize = () -> KBANN.create(ruleFile, dataset, specificRules, kbannSettings);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        Learnable learn = (kbann, learningDataset) -> ((KBANN) kbann).learn(learningDataset, finalWls);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

    private void runCasCor(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Cascade Correlation use 'CasCor   #ofRepeats  datasetFile  weightLearningSettingsFile  cascadeCorrelationSetting'");
        }
        String algName = "CasCor";

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        File settingFile = new File(arg[4]);
        CascadeCorrelationSetting ccSetting = CascadeCorrelationSetting.create(settingFile);

        Initable<CascadeCorrelation> initialize = () -> CascadeCorrelation.create(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator, new MissingValueKBANN());
        Learnable learn = (cascadeCorrelation, learningDataset) -> ((CascadeCorrelation) cascadeCorrelation).learn(learningDataset, finalWls, ccSetting);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

    private void runDNC(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Dynamic Node Creation use 'DNC   #ofRepeats  datasetFile  weightLearningSettingsFile  DNCSetting'");
        }
        String algName = "DNC";

        File settingFile = new File(arg[4]);
        DNCSetting dncSetting = DNCSetting.create(settingFile);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);

        Initable<DynamicNodeCreation> initialize = () -> DynamicNodeCreation.create(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator, new MissingValueKBANN());
        Learnable learn = (dnc, learningDataset) -> ((DynamicNodeCreation) dnc).learn(learningDataset, finalWls, dncSetting);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

    private void runSLSF(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Structure Learning with Forgetting use 'SLSF   #ofRepeats  datasetFile  weightLearningSettingsFile  initialNetworkStructure'");
        }
        String algName = "SLSF";

        File structureFile = new File(arg[4]);
        Initable<StructuralLearningWithSelectiveForgetting> initialize = () -> StructuralLearningWithSelectiveForgetting.create(structureFile, dataset, randomGenerator);
        Learnable learn = (slsf, learningDataset) -> ((StructuralLearningWithSelectiveForgetting) slsf).learn(learningDataset, wls);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, wls.getFile(), wls);
    }

    private void runTopGen(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "TopGen";
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run TopGen use 'TopGen   #ofRepeats  datasetFile  weightLearningSettingsFile ruleFile TopGenSettings'");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        File settingFile = new File(arg[5]);
        TopGenSettings tgSetting = TopGenSettings.create(settingFile);

        Initable<TopGen> initialize = () -> TopGen.create(new File(arg[4]), specific, randomGenerator, tgSetting, dataset);
        Learnable learn = (topGen, learningDataset) -> ((TopGen) topGen).learn(learningDataset, finalWls, tgSetting);

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }


    private void runREGENT(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "REGENT";
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run REGENT use 'REGENT   #ofRepeats  datasetFile  weightLearningSettingsFile ruleFile RegentSetting'");
        }
        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);

        File settingFile = new File(arg[5]);
        RegentSetting regentSetting = RegentSetting.create(settingFile, randomGenerator);

        Initable<Regent> initialize = () -> Regent.create(new File(arg[4]), specific, randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting, dataset);
        Learnable learn = (regent, learningDataset) -> ((Regent) regent).learn(learningDataset, finalWls, regentSetting, new KBANNSettings(randomGenerator, regentSetting.getTopGenSettings().getOmega(), regentSetting.getTopGenSettings().perturbationMagnitude()));

        Crossvalidation crossval = Crossvalidation.createStratified(dataset, randomGenerator, numberOfFolds);
        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, crossval, settingFile, finalWls);
    }

    /*private void runMAC() {
        File wlsFile = new File("./ruleExamples/sampleInput/wlsSettings.txt");
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);


        Dataset dataset = DatasetImpl.parseDataset("./ruleExamples/sampleInput/xor.txt");
        RandomGeneratorImpl randomGenerator = new RandomGeneratorImpl(0.3d, 0.0d, 7);
        MultilayeredConstructiveArchitecture mca = new MultilayeredConstructiveArchitecture(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator);

        mca.learn(dataset, wls);
        TexFile tex = TikzExporter.export(mca.getNeuralNetwork());
        File output = tex.saveAs("./pdfexport/MAC/xor/network.tex");
        TexFile.build(output);
    }*/


    /*private void run() {
        NeuralNetwork network = simpleNetwork();
        List<Double> input = new ArrayList<>();
        input.add(1d);
        input.add(-5d);
        input.add(0.5);

        List<Double> output = network.evaluate(input);
        System.out.println("results");
        output.forEach(System.out::println);

        texFile tex = TikzExporter.export(network);
        File file = tex.saveAs("./pdfexport/network.tex");
        texFile.build(file);
    }

    private NeuralNetwork simpleNetwork() {
        System.out.println("otestovat jeste jak se to chova s copy");

        NeuralNetworkImpl network = new NeuralNetworkImpl(3, 3, new MissingValueKBANN());
        List<Node> input = network.getInputNodes();
        List<Node> output = network.getOutputNodes();

        Node n11 = NodeFactory.create(Sigmoid.getFunction());
        Node n12 = NodeFactory.create(Sigmoid.getFunction());
        Node n13 = NodeFactory.create(Sigmoid.getFunction());

        Node n20 = NodeFactory.create(Sigmoid.getFunction());
        //network.addNodeAtLayerStateful(n20, 1);

        network.addNodeAtLayerStateful(n11, 0);
        network.addNodeAtLayerStateful(n12, 0);
        network.addNodeAtLayerStateful(n13, 0);

        System.out.println("adding edges from n00");
        network.addEdgeStateful(input.get(0), n11, 1d, Edge.Type.FORWARD);
        network.addEdgeStateful(input.get(0), n12, 0.5, Edge.Type.FORWARD);
        network.addEdgeStateful(input.get(0), n13, -0.5, Edge.Type.FORWARD);

        System.out.println("adding edges from n01");
        network.addEdgeStateful(input.get(1), n12, 0.0, Edge.Type.FORWARD);
        //network.addEdgeStateful(input.get(1), n13, -0.5, Edge.Type.FORWARD);

        System.out.println("adding edges from n02");
        network.addEdgeStateful(input.get(2), n13, 1d, Edge.Type.FORWARD);

        System.out.println("adding edges to n30");
        network.addEdgeStateful(n11, output.get(0), 1d, Edge.Type.FORWARD);
        network.addEdgeStateful(n12, output.get(0), 1d, Edge.Type.FORWARD);
        network.addEdgeStateful(n13, output.get(0), 1d, Edge.Type.FORWARD);

        System.out.println("adding edges to n32");
        network.addEdgeStateful(n12, output.get(1), 1d, Edge.Type.FORWARD);

        System.out.println("adding edges to n33");
        network.addEdgeStateful(n13, output.get(2), 1d, Edge.Type.FORWARD);

        System.out.println("edges");

        System.out.println("out z n00");
        Set<Edge> q = network.getOutgoingEdges(network.getInputNodes().get(0));
        q.forEach(System.out::println);

        System.out.println("in do n10");
        Set<Edge> qq = network.getIncomingEdges(n11);
        qq.forEach(System.out::println);

        System.out.println(q.size());

        System.out.println("////\n");
        return network;
    }*/

}
