package main.java.cz.cvut.ida.nesisl.application;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.ActivationFunction;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Edge;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.NeuralNetwork;
import main.java.cz.cvut.ida.nesisl.api.neuralNetwork.Node;
import main.java.cz.cvut.ida.nesisl.modules.algorithms.cascadeCorrelation.CasCorSetting;
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
import main.java.cz.cvut.ida.nesisl.modules.experiments.ExperimentResult;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NeuralNetworkImpl;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.NodeFactory;
import main.java.cz.cvut.ida.nesisl.modules.neuralNetwork.activationFunctions.Sigmoid;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;
import main.java.cz.cvut.ida.nesisl.modules.tool.RandomGeneratorImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Tools;

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
            throw new IllegalStateException("Right setting in form 'algorithm numberOfRuns dataset weightLearningSetting [ruleFile structureLearningSetting]'");
        }

        double simga = 1d;
        double mu = 0.0d;
        int seed = 7;
        int numberOfRepeats = Integer.valueOf(arg[1]);
        File datasetFile = new File(arg[2]);
        File wlsFile = new File(arg[3]);
        RandomGeneratorImpl randomGenerator = new RandomGeneratorImpl(simga, mu, seed);
        Main main = new Main();
        switch (arg[0]) {
            case "KBANN":
                main.runKBANN(arg, numberOfRepeats, datasetFile, wlsFile, randomGenerator);
                break;
            case "CasCor":
                main.runCasCor(arg, numberOfRepeats, datasetFile, wlsFile, randomGenerator);
                break;
            case "DNC":
                main.runDNC(arg, numberOfRepeats, datasetFile, wlsFile, randomGenerator);
                break;
            case "SLF":
                main.runSLF(arg, numberOfRepeats, datasetFile, wlsFile, randomGenerator);
                break;
            case "REGENT":
                main.runREGENT(arg, numberOfRepeats, datasetFile, wlsFile, randomGenerator);
                break;
            case "TopGen":
                main.runTopGen(arg, numberOfRepeats, datasetFile, wlsFile, randomGenerator);
                break;
            default:

                System.out.println("Unknown algorithm '" + arg[0] + "'.");
                break;
        }

        //System.out.println("zkontrolovat jestli vsechny forEach jsou spravne a nemely by byt nahrazeny forEachOrdered");
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
            currentResult.setAverageSquaredError(Tools.computeAverageSuqaredTotalError(regent.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(regent.getNeuralNetwork().getCopy());
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.printResults(results, algName, datasetFile);
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
            currentResult.setAverageSquaredError(Tools.computeAverageSuqaredTotalError(topgen.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(topgen.getNeuralNetwork().getCopy());

            System.out.println("ending topget\t" + idx);
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.printResults(results, algName, datasetFile);
    }


    private void runSLF(String[] arg, int numberOfRepeats, File datasetFile, File wlsFile, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "SLF";

        Dataset dataset = DatasetImpl.parseDataset(datasetFile);
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);

        NeuralNetwork network = StructuralLearningWithSelectiveForgetting.createInitNetwork(new File(arg[4]), dataset, randomGenerator);

        double penaltyEpsilon = 0.0001;
        double treshold = 0.1;//0.1;
        SLFSetting slfSetting = new SLFSetting(penaltyEpsilon, treshold);
        List<ExperimentResult> results = IntStream.range(0, numberOfRepeats).parallel().mapToObj(idx -> {
            ExperimentResult currentResult = new ExperimentResult(idx, algName, datasetFile);

            NeuralNetwork copied = network.getCopy();
            copied.getWeights().entrySet().forEach(entry -> copied.setEdgeWeight(entry.getKey(), randomGenerator.nextDouble()));

            StructuralLearningWithSelectiveForgetting slf = new StructuralLearningWithSelectiveForgetting(copied);

            currentResult.setInitNetwork(slf.getNeuralNetwork().getCopy());

            long start = System.nanoTime();
            slf.learn(dataset, wls, slfSetting);
            long end = System.nanoTime();

            Tools.printEvaluation(slf.getNeuralNetwork(), dataset);

            currentResult.setRunningTime(end - start);
            currentResult.setAverageSquaredError(Tools.computeAverageSuqaredTotalError(slf.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(slf.getNeuralNetwork().getCopy());
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.printResults(results, algName, datasetFile);
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

    private void runDNC(String[] arg, int numberOfRepeats, File datasetFile, File wlsFile, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "DNC";
        Dataset dataset = DatasetImpl.parseDataset(datasetFile);
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);


        // tohle predelat a naparsovat ze vstupu
        double deltaT = 0.05;
        long timeWindow = 5l;
        double cm = 0.01;
        double ca = 0.001;
        long hiddenNodesLimit = 70;
        DNCSetting dncSetting = new DNCSetting(deltaT, timeWindow, cm, ca, hiddenNodesLimit);

        List<ExperimentResult> results = IntStream.range(0, numberOfRepeats).parallel().mapToObj(idx -> {
            ExperimentResult currentResult = new ExperimentResult(idx, algName, datasetFile);

            DynamicNodeCreation dnc = new DynamicNodeCreation(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator);

            currentResult.setInitNetwork(dnc.getNeuralNetwork().getCopy());

            long start = System.nanoTime();
            dnc.learn(dataset, wls, dncSetting);
            long end = System.nanoTime();

            currentResult.setRunningTime(end - start);
            currentResult.setAverageSquaredError(Tools.computeAverageSuqaredTotalError(dnc.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(dnc.getNeuralNetwork().getCopy());
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.printResults(results, algName, datasetFile);
    }

    private void runCasCor(String[] arg, int numberOfRepeats, File datasetFile, File wlsFile, RandomGeneratorImpl randomGenerator) throws FileNotFoundException {
        String algName = "CasCor";

        Dataset dataset = DatasetImpl.parseDataset(datasetFile);
        WeightLearningSetting wls = WeightLearningSetting.parse(wlsFile);

        // udelat parser na to
        CasCorSetting ccSetting = new CasCorSetting(wls.getSizeOfCasCorPool(), wls.getMaximumNumberOfHiddenNodes());

        List<ExperimentResult> results = IntStream.range(0, numberOfRepeats).parallel().mapToObj(idx -> {
            ExperimentResult currentResult = new ExperimentResult(idx, algName, datasetFile);

            CascadeCorrelation cascor = new CascadeCorrelation(dataset.getInputFactOrder(), dataset.getOutputFactOrder(), randomGenerator);

            currentResult.setInitNetwork(cascor.getNeuralNetwork().getCopy());

            long start = System.nanoTime();
            cascor.learn(dataset, wls, ccSetting);
            long end = System.nanoTime();

            currentResult.setRunningTime(end - start);
            currentResult.setAverageSquaredError(Tools.computeAverageSuqaredTotalError(cascor.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(cascor.getNeuralNetwork().getCopy());
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.printResults(results, algName, datasetFile);
    }

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

            long start = System.nanoTime();
            kbann.learn(dataset, wls);
            long end = System.nanoTime();

            Tools.printEvaluation(kbann.getNeuralNetwork(), dataset);

            currentResult.setRunningTime(end - start);
            currentResult.setAverageSquaredError(Tools.computeAverageSuqaredTotalError(kbann.getNeuralNetwork(), dataset));
            currentResult.setFinalNetwork(kbann.getNeuralNetwork().getCopy());
            return currentResult;
        }).collect(Collectors.toCollection(ArrayList::new));

        ExperimentResult.printResults(results, algName, datasetFile);
    }

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
