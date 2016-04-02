package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
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
import main.java.cz.cvut.ida.nesisl.modules.algorithms.structuralLearningWithSelectiveForgetting.SLFSetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.structuralLearningWithSelectiveForgetting.StructuralLearningWithSelectiveForgetting;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGen;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.topGen.TopGenSettings;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Initable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.Learnable;
import main.java.cz.cvut.ida.nesisl.modules.experiments.NeuralNetworkOwner;
import main.java.cz.cvut.ida.nesisl.modules.experiments.evaluation.ExperimentResult;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            throw new IllegalStateException("Right setting in form 'algorithm numberOfRuns dataset weightLearningSetting [...]'. Possible algorithms KBANN, CasCor, DNC, SLF, TopGen, REGENT; write as first argument to see more.");
        }
        // algName  #runs   datasetFile wlsFile ruleFile KBANNsetting ruleSpecificFile

        double simga = 1d;
        double mu = 0.0d;
        int seed = 7;
        int numberOfRepeats = Integer.valueOf(arg[1]);
        File datasetFile = new File(arg[2]);
        File wlsFile = new File(arg[3]);
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);
        Dataset dataset = DatasetImpl.createDataset(datasetFile);
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

        //System.out.println("zkontrolovat jestli vsechny forEach jsou spravne a nemely by byt nahrazeny forEachOrdered");
    }

    private void runAndStoreExperiments(Initable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, Dataset dataset) {
        List<ExperimentResult> results = runExperiments(initialize, learn, numberOfRepeats, algName, dataset);
        ExperimentResult.storeResultsResults(results, algName, dataset.getOriginalFile());
    }

    private List<ExperimentResult> runExperiments(Initable<? extends NeuralNetworkOwner> initialize, Learnable learn, int numberOfRepeats, String algName, Dataset dataset) {
        return IntStream.range(0, numberOfRepeats).parallel().mapToObj(idx -> {
            ExperimentResult currentResult = new ExperimentResult(idx, algName, dataset.getOriginalFile());
            NeuralNetworkOwner alg = initialize.initialize();
            currentResult.setInitNetwork(alg.getNeuralNetwork().getCopy());

            long start = System.nanoTime();
            NeuralNetwork learnedNetwork = learn.learn(alg);
            long end = System.nanoTime();

            Tools.printEvaluation(learnedNetwork, dataset);

            currentResult.addExperiment(learnedNetwork, start, end, dataset);
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private void runKBANN(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run KBANN use 'KBANN   #ofRepeats  datasetFile weightLearningSettingsFile  ruleFile    KBANNsetting    [ruleSpecificFile]'");
        }

        String algName = "KBANN";
        File ruleFile = new File(arg[4]);
        KBANNSettings kbannSettings = KBANNSettings.create(randomGenerator, new File(arg[5]));

        if (arg.length > 6) {
            throw new UnsupportedOperationException("Specific rules are not implemented yet. (None parser nor KBANN inner usage of specific rules are implemented.");
        }
        List<Pair<Integer, ActivationFunction>> specificRules = new ArrayList<>();

        Initable<KBANN> initialize = () -> KBANN.create(ruleFile, specificRules, kbannSettings);
        Learnable learn = (kbann) -> ((KBANN) kbann).learn(dataset, wls);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset);
    }

    private void runCasCor(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 5) {
            throw new IllegalStateException("Need more arguments. To run Cascade Correlation use 'CasCor   #ofRepeats  datasetFile  weightLearningSettingsFile  cascadeCorrelationSetting'");
        }
        String algName = "CasCor";

        CascadeCorrelationSetting ccSetting = CascadeCorrelationSetting.create(new File(arg[4]));

        Initable<CascadeCorrelation> initialize = () -> CascadeCorrelation.create(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator, new MissingValueKBANN());
        Learnable learn = (cascadeCorrelation) -> ((CascadeCorrelation) cascadeCorrelation).learn(dataset, wls, ccSetting);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset);
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
        DNCSetting dncSetting = DNCSetting.create(new File(arg[4]));

        Initable<DynamicNodeCreation> initialize = () -> DynamicNodeCreation.create(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator, new MissingValueKBANN());
        Learnable learn = (dnc) -> ((DynamicNodeCreation) dnc).learn(dataset, wls, dncSetting);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset);
    }

    private void runSLF(String[] arg, int numberOfRepeats, Dataset dataset, WeightLearningSetting wls, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        if (arg.length < 6) {
            throw new IllegalStateException("Need more arguments. To run Structure Learning with Forgetting use 'SLF   #ofRepeats  datasetFile  weightLearningSettingsFile  initialNetworkStructure '");
        }
        String algName = "SLF";

        /*Double penaltyEpsilon = 0.0001;
        Double treshold = 0.1;//0.1;*/

        Initable<StructuralLearningWithSelectiveForgetting> initialize = () -> StructuralLearningWithSelectiveForgetting.create(new File(arg[4]), dataset, randomGenerator);
        Learnable learn = (slf) -> ((StructuralLearningWithSelectiveForgetting) slf).learn(dataset, wls);

        runAndStoreExperiments(initialize, learn, numberOfRepeats, algName, dataset);
    }


    private void runREGENT(String[] arg, int numberOfRepeats, File datasetFile, File wlsFile, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "REGENT";
        File file = new File(arg[4]);
        Double omega = 4.0;
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        Double treshold = 0.0000002;
        Long lengthOfOpenList = 2l;
        Long numberOfSuccessors = 1l;

        TopGenSettings tgSetting = new TopGenSettings(treshold, numberOfSuccessors, lengthOfOpenList);

        long populationSize = 4;
        long tournamentSize = 1;
        Integer numberOfMutationOfPopulation = 1;
        Integer numberOfMutationOfCrossovers = 1;
        KBANNSettings kbannSetting = new KBANNSettings(randomGenerator, omega);
        Double probabilityOfNodeDeletion = 0.4;
        Long maxFitness = 1000l;
        Integer numberOfCrossoverChildren = 1;
        Integer numberOfElites = 1;
        RegentSetting regentSetting = new RegentSetting(tournamentSize, populationSize, tgSetting, numberOfMutationOfPopulation, numberOfMutationOfCrossovers, kbannSetting, probabilityOfNodeDeletion, maxFitness, numberOfCrossoverChildren, numberOfElites);


        // nedodelana funkcionalita setting pro KBANN algoritmus (omega) a dalsich pravidel, ktere se mohou vkladat do site (specific) (ty jsou totiz delany expertem)
        Dataset dataset = DatasetImpl.parseDataset(datasetFile);

        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);
        List<ExperimentResult> results = IntStream.range(0, numberOfRepeats).parallel().mapToObj(idx -> {
            Regent regent = Regent.create(file, specific, randomGenerator, omega);

            ExperimentResult currentResult = new ExperimentResult(idx, algName, datasetFile);
            System.out.println(regent.getNeuralNetwork());
            currentResult.setInitNetwork(regent.getNeuralNetwork().getCopy());


            long start = System.nanoTime();
            regent.learn(dataset, wls, regentSetting, kbannSetting);
            long end = System.nanoTime();

            Tools.printEvaluation(regent.getNeuralNetwork(), dataset);

            currentResult.setRunningTime(end - start);
            currentResult.setAverageSquaredTotalError(Tools.computeAverageSquaredTotalError(regent.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(regent.getNeuralNetwork().getCopy());
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.storeResultsResults(results, algName, datasetFile);
    }

    private void runTopGen(String[] arg, int numberOfRepeats, File datasetFile, File wlsFile, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "TopGen";
        File file = new File(arg[4]);
        Double omega = 4.0;
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        Double treshold = 0.0001;
        Long lengthOfOpenList = 100l;
        Long numberOfSuccessors = 10l;

        TopGenSettings tgSetting = new TopGenSettings(treshold, numberOfSuccessors, lengthOfOpenList);

        KBANNSettings kbannSetting = new KBANNSettings(randomGenerator, omega);

        // nedodelana funkcionalita setting pro KBANN algoritmus (omega) a dalsich pravidel, ktere se mohou vkladat do site (specific) (ty jsou totiz delany expertem)
        Dataset dataset = DatasetImpl.parseDataset(datasetFile);

        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);
        List<ExperimentResult> results = IntStream.range(0, numberOfRepeats).parallel().mapToObj(idx -> {
            //List<ExperimentResult> results = IntStream.range(0, numberOfRepeats).mapToObj(idx -> {
            System.out.println("starting topget\t" + idx);
            TopGen topgen = TopGen.create(file, specific, randomGenerator, omega);

            ExperimentResult currentResult = new ExperimentResult(idx, algName, datasetFile);
            currentResult.setInitNetwork(topgen.getNeuralNetwork().getCopy());

            long start = System.nanoTime();
            topgen.learn(dataset, wls, tgSetting, kbannSetting);
            long end = System.nanoTime();

            System.out.println("from\t" + idx);
            Tools.printEvaluation(topgen.getNeuralNetwork(), dataset);
            System.out.println("to\t" + idx);

            currentResult.setRunningTime(end - start);
            currentResult.setAverageSquaredTotalError(Tools.computeAverageSquaredTotalError(topgen.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(topgen.getNeuralNetwork().getCopy());

            System.out.println("ending topget\t" + idx);
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.storeResultsResults(results, algName, datasetFile);
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





    /*
    private void runKBANN(String[] arg, int numberOfRepeats, File datasetFile, File wlsFile, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "KBANN";
        File file = new File(arg[4]);
        Double omega = 4.0;
        List<Pair<Integer, ActivationFunction>> specific = new ArrayList<>();

        // nedodelana funkcionalita setting pro KBANN algoritmus (omega) a dalsich pravidel, ktere se mohou vkladat do site (specific) (ty jsou totiz delany expertem)
        Dataset dataset = DatasetImpl.parseDataset(datasetFile);

        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);
        List<ExperimentResult> results = IntStream.range(0, numberOfRepeats).parallel().mapToObj(idx -> {
            KBANN kbann = new KBANN(file, specific, randomGenerator, omega);
            ExperimentResult currentResult = new ExperimentResult(idx, algName, datasetFile);
            currentResult.setInitNetwork(kbann.getNeuralNetwork().getCopy());


            TADY TO ZPARAMETRIZOVAT DO LAMBDA FUNKCE !!!!
            long start = System.nanoTime();
            kbann.learn(dataset, wls);
            long end = System.nanoTime();

            Tools.printEvaluation(kbann.getNeuralNetwork(), dataset);

            System.out.println("ponovu");
            System.out.println(kbann.getNeuralNetwork().getClassifier().getTreshold());
            System.out.println("AUC\t" + AUCCalculation.create(kbann.getNeuralNetwork(),dataset).computeAUC());

            currentResult.setRunningTime(end - start);
            currentResult.setAverageSquaredTotalError(Tools.computeAverageSquaredTotalError(kbann.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(kbann.getNeuralNetwork().getCopy());
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.storeResultsResults(results, algName, datasetFile);
    }
     */
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
    }*/

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
    }


}
