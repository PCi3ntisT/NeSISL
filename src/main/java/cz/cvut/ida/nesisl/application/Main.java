package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
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
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Initable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Learnable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation.ExperimentResult;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

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

    public static void main(String arg[]) throws FileNotFoundException {
        //arg = new String[]{"DNC", "1", "./ruleExamples/sampleInput/sampleOne.txt", "./ruleExamples/sampleInput/wlsSettings.txt", "./ruleExamples/KBANN/sampleOne.txt"};
        //arg = new String[]{"SLF", "1", "./ruleExamples/sampleInput/xor.txt", "./ruleExamples/sampleInput/wlsSettings.txt", ""};
        //arg = new String[]{"SLF", "1", "./ruleExamples/sampleInput/xor.txt", "./ruleExamples/sampleInput/wlsSettings.txt", "./ruleExamples/sampleInput/SLFinput.txt"};
        if (arg.length < 4) {
            System.out.println("Not enought arguments. Right setting in form 'algorithmName numberOfRuns datasetFile weightLearningSettingFile [...]'. Possible algorithms KBANN, CasCor, DNC, SLF, TopGen, REGENT; write as first argument to see more.");
            System.exit(0);
        }

        double simga = 1d;
        double mu = 0.0d;
        int seed = 13;
        int numberOfRepeats = 0;
        try{
            numberOfRepeats = Integer.valueOf(arg[1]);
        }catch (Exception ex){
            System.out.println("The second argument (number of repeats) must be integer.");
            System.out.println("Argument input instead '" + arg[1]+ "'.");
            System.exit(0);
        }
        File datasetFile = new File(arg[2]);
        if(!datasetFile.exists()){
            System.out.println("The third argument (datasetFile) does not exist.");
            System.out.println("Argument input instead '" + arg[2]+ "'.");
            System.exit(0);
        }
        File wlsFile = new File(arg[3]);
        if(!wlsFile.exists()){
            System.out.println("The fourth argument (weightLearningSettingFile) does not exist.");
            System.out.println("Argument input instead '" + arg[3]+ "'.");
            System.exit(0);
        }
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);
        Dataset dataset = DatasetImpl.parseDataset(datasetFile);
        RandomGeneratorImpl randomGenerator = new RandomGeneratorImpl(simga, mu, seed);

        Main main = new Main();
        switch (arg[0]) {
            case "KBANN":
                main.runKBANN(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "CasCor":
                main.runCasCor(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "DNC":
                main.runDNC(arg, numberOfRepeats, dataset, wls, randomGenerator);
                break;
            case "SLF":
                main.runSLF(arg, numberOfRepeats, dataset, wls, randomGenerator);
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

    private void runAndStoreExperiments(Initable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, Dataset dataset, File settingFile, WeightLearningSetting wls) {
        List<ExperimentResult> results = runExperiments(initialize, learn, numberOfRepeats, algName, dataset, settingFile, wls);
        ExperimentResult.storeResults(results, algName, dataset.getOriginalFile(), settingFile, wls);
    }

    private List<ExperimentResult> runExperiments(Initable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, Dataset dataset, File settingFile, WeightLearningSetting wls) {
        return IntStream.range(0, numberOfRepeats).parallel().mapToObj(idx -> {
            ExperimentResult currentResult = new ExperimentResult(idx, algName, dataset.getOriginalFile(), settingFile, wls);
            NeuralNetworkOwner alg = initialize.initialize();
            currentResult.setInitNetwork(alg.getNeuralNetwork().getCopy());

            long start = System.currentTimeMillis();
            NeuralNetwork learnedNetwork = learn.learn(alg);
            long end = System.currentTimeMillis();

            /*
            Tools.printEvaluation(learnedNetwork, dataset);
            System.out.println("\t" + learnedNetwork.getClassifier().getTreshold());
            */

            currentResult.addExperiment(learnedNetwork, start, end, dataset);
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

        Initable<KBANN> initialize = () -> KBANN.create(ruleFile, specificRules, kbannSettings);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);
        Learnable learn = (kbann) -> ((KBANN) kbann).learn(dataset, finalWls);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset, settingFile, finalWls);
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
        Learnable learn = (cascadeCorrelation) -> ((CascadeCorrelation) cascadeCorrelation).learn(dataset, finalWls, ccSetting);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset, settingFile, finalWls);
    }

    private void runDNC(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Dynamic Node Creation use 'DNC   #ofRepeats  datasetFile  weightLearningSettingsFile  DNCSetting'");
        }
        String algName = "DNC";


        /*
        double deltaT = 0.05;
        long timeWindow = 5l;
        double cm = 0.01;
        double ca = 0.001;
        long hiddenNodesLimit = 70;
         */
        File settingFile = new File(arg[4]);
        DNCSetting dncSetting = DNCSetting.create(settingFile);
        final WeightLearningSetting finalWls = WeightLearningSetting.turnOffRegularization(wls);


        Initable<DynamicNodeCreation> initialize = () -> DynamicNodeCreation.create(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator, new MissingValueKBANN());
        Learnable learn = (dnc) -> ((DynamicNodeCreation) dnc).learn(dataset, finalWls, dncSetting);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset, settingFile, finalWls);
    }

    private void runSLF(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Structure Learning with Forgetting use 'SLF   #ofRepeats  datasetFile  weightLearningSettingsFile  initialNetworkStructure'");
        }
        String algName = "SLF";

        /*Double penaltyEpsilon = 0.0001;
        Double treshold = 0.1;//0.1;*/

        File settingFile = new File(arg[4]);
        Initable<StructuralLearningWithSelectiveForgetting> initialize = () -> StructuralLearningWithSelectiveForgetting.create(settingFile, dataset, randomGenerator);
        Learnable learn = (slf) -> ((StructuralLearningWithSelectiveForgetting) slf).learn(dataset, wls);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset, wls.getFile(), wls);
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
        /*Double treshold = 0.0001;
        Long lengthOfOpenList = 100l;
        Long numberOfSuccessors = 10l;*/
        File settingFile = new File(arg[5]);
        TopGenSettings tgSetting = TopGenSettings.create(settingFile);

        Initable<TopGen> initialize = () -> TopGen.create(new File(arg[4]), specific, randomGenerator, tgSetting);
        Learnable learn = (topGen) -> ((TopGen) topGen).learn(dataset, finalWls, tgSetting);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset, settingFile, finalWls);
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
        /*long populationSize = 4;
        long tournamentSize = 1;
        Integer numberOfMutationOfPopulation = 1;
        Integer numberOfMutationOfCrossovers = 1;
        Double probabilityOfNodeDeletion = 0.4;
        Long maxFitness = 1000l;
        Integer numberOfCrossoverChildren = 1;
        Integer numberOfElites = 1;*/

        File settingFile = new File(arg[5]);
        RegentSetting regentSetting = RegentSetting.create(settingFile, randomGenerator);

        Initable<Regent> initialize = () -> Regent.create(new File(arg[4]), specific, randomGenerator, regentSetting.getTopGenSettings().getOmega());
        Learnable learn = (regent) -> ((Regent) regent).learn(dataset , finalWls, regentSetting, new KBANNSettings(randomGenerator, regentSetting.getTopGenSettings().getOmega()));

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset, settingFile, finalWls);
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
