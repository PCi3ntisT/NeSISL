package main.java.cz.cvut.ida.nesisl.modules.trepan;

import java.io.*;

/**
 * Created by EL on 16.8.2016.
 */
public class TrepanResults {

    private final Long numberOfInnerNodes;
    private final Double trpanTrainAccuracy;
    private final Double trepanTestAccuracy;
    private final Double trainFidelity;
    private final Double testFidelity;
    private final Double networkTrainAccuracy;
    private final Double networkTestAccuracy;

    private TrepanResults(Long numberOfInnerNodes, Double trpanTrainAccuracy, Double trepanTestAccuracy, Double trainFidelity, Double testFidelity, Double networkTrainAcc, Double networkTestAcc) {
        this.numberOfInnerNodes = numberOfInnerNodes;
        this.trpanTrainAccuracy = trpanTrainAccuracy;
        this.trepanTestAccuracy = trepanTestAccuracy;
        this.trainFidelity = trainFidelity;
        this.testFidelity = testFidelity;
        this.networkTrainAccuracy = networkTrainAcc;
        this.networkTestAccuracy = networkTestAcc;
    }

    public Double getNetworkTrainAccuracy() {
        return networkTrainAccuracy;
    }

    public Double getNetworkTestAccuracy() {
        return networkTestAccuracy;
    }

    public Long getNumberOfInnerNodes() {
        return numberOfInnerNodes;
    }

    public Double getTrpanTrainAccuracy() {
        return trpanTrainAccuracy;
    }

    public Double getTrepanTestAccuracy() {
        return trepanTestAccuracy;
    }

    public Double getTrainFidelity() {
        return trainFidelity;
    }

    public Double getTestFidelity() {
        return testFidelity;
    }

    public static TrepanResults create(File tree, File fidelity,File accuracyFile) {
        System.out.println("trepan disabled");
        return null;
        //long finalNumberOfInnerNodes = getNumberOfNodes(tree);
        //return retrieveValuesFromFidelityAndAccuracy(finalNumberOfInnerNodes, fidelity, accuracyFile);
    }

    // does not take in account possibility of validation set in the result ;)
    private static TrepanResults retrieveValuesFromFidelityAndAccuracy(long finalNumberOfInnerNodes, File fidelity, File accuracyFile) {
        Double trepanTrainAcc = 0.0;
        Double trepanTestAcc = 0.0;
        Double trainFidelity = 0.0;
        Double testFidelity = 0.0;
        Long node = -100l;

        Double networkTrainAcc = 0.0;
        Double networkTestAcc = 0.0;


        try (BufferedReader br = new BufferedReader(new FileReader(fidelity))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split("\\s+");
                if(line.trim().length() > 0 && !splitted[0].trim().equals("nodes")){
                    Long currentNode = Long.valueOf(splitted[0]);
                    if(Math.abs(finalNumberOfInnerNodes - currentNode) < Math.abs(finalNumberOfInnerNodes - node)){
                        node = currentNode;
                        trainFidelity = Double.valueOf(splitted[1]);
                        trepanTrainAcc = Double.valueOf(splitted[2]);
                        testFidelity = Double.valueOf(splitted[3]);
                        trepanTestAcc = Double.valueOf(splitted[4]);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(accuracyFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(line.contains("Test")){
                    String[] splitted = line.split("\\s+");
                    networkTestAcc = Double.valueOf(splitted[splitted.length-1]);
                }else if(line.contains("Training")){
                    String[] splitted = line.split("\\s+");
                    networkTrainAcc = Double.valueOf(splitted[splitted.length-1]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new TrepanResults(finalNumberOfInnerNodes,trepanTrainAcc,trepanTestAcc,trainFidelity,testFidelity,networkTrainAcc,networkTestAcc);
    }

    private static long getNumberOfNodes(File tree) {
        Long numberOfInnerNodes = 0l;
        try (BufferedReader br = new BufferedReader(new FileReader(tree))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(describesInnerNode(line)){
                    numberOfInnerNodes++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numberOfInnerNodes;
    }

    private static boolean describesInnerNode(String line) {
        return line.contains("shape=box");
    }

}
