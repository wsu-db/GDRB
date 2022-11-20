package edu.wsu.eecs.gfc.core;

import com.google.common.base.Stopwatch;
import java.io.*;
// import java.nio.file.Files;
// import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
// import com.google.gson.Gson; 

public class TrainGFC {
    private static final int GLOBAL_HOPS = 2;

    public static List<String> trainingAssertions = new ArrayList<>();

    public static void train(String inputDir,String outputDir,double minSupp,double minConf,int maxSize,int topK) throws Exception {

        new File(outputDir).mkdirs();
        new File("Trained_Models").mkdirs();
        new File("Patterns").mkdirs();

        System.out.println("Configurations:"
                + "\nInputDir = " + inputDir
                + "\nOutputDir = " + outputDir
                + "\nminSupp = " + minSupp
                + "\nminConf = " + minConf
                + "\nmaxSize = " + maxSize
                + "\ntop-K = " + topK);

        System.out.println("Loading the data graph....");
        Graph<String, String> graph = IO.loadStringGraph(inputDir);
        System.out.println("Graph: " + graph.toSizeString());

        System.out.println("Loading the ontology....");
        DirectedAcyclicGraph<String, String> onto = IO.loadDAGOntology(inputDir);
        System.out.println("Indexing the ontology....");
        Map<String, Map<Integer, Set<String>>> ontoIndex = Utility.indexOntology(onto, GLOBAL_HOPS);

        System.out.println("Indexing the data graph....");
        GraphDatabase<String, String> bigGraph = GraphDatabase.buildFromGraph(graph, ontoIndex);
        System.out.println("BigGraph: " + bigGraph.toSizeString());

        System.out.println("Loading the input relations....");
        List<Relation<String, String>> relationList = IO.loadRelations(inputDir);

        RuleMiner<String, String> miner = RuleMiner.createInit(bigGraph, minSupp, minConf, maxSize, topK);

        for (Relation<String, String> r : relationList) {
            System.out.println("----------------------------------------");
            System.out.println("Training for r(x, y) = " + r);
            // String rName = r.srcLabel() + "_" + r.edgeLabel() + "_" + r.dstLabel();

            FactSampler sampler = new FactSampler(inputDir);
            sampler.extract_training_asserions(inputDir,r);

            bigGraph.buildSimLabelsMap(0);

            Stopwatch w = Stopwatch.createStarted();
            List<OGFCRule<String, String>> patterns = miner.OGFC_stream(r, sampler.getDataTrain().get(true), sampler.getDataTrain().get(false));
            w.stop();

            // Gson gson = new Gson();
            // Writer writer = Files.newBufferedWriter(Paths.get("./Patterns/"+rName+".json"));
            // gson.toJson(patterns, writer);
            // writer.close();

            // System.out.println(sampler.getDataTrain().get(true));
            System.out.println("Discovered number of patterns: |P| = " + patterns.size() + ", Time = " + w.elapsed(TimeUnit.SECONDS));

            System.out.println("\nTraining: "
                    + FactChecker.Train_LRModel(patterns, r, sampler.getDataTrain(), outputDir));

        }
        System.out.println("-------------------DONE-----------------");
    }

    public static List<String> addTrainingData(String sub, String pred, String obj, String truthVal){

        String assertion = sub +"\t" + pred +"\t" + obj +"\t" + truthVal ;
        trainingAssertions.add(assertion);
        return trainingAssertions;
    }
    
}
