package edu.wsu.eecs.gfc.exps;

import com.google.common.base.Stopwatch;
import edu.wsu.eecs.gfc.core.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * The caller to test GFC mining.
 * @author Peng Lin penglin03@gmail.com
 */
public class TestGFC {

    private static final int GLOBAL_HOPS = 2;

    public static void main(String[] args) throws Exception {
        String inputDir = args[0];
        String outputDir = args[1];
        new File(outputDir).mkdirs();

        double minSupp = Double.parseDouble(args[2]);
        double minConf = Double.parseDouble(args[3]);
        int maxSize = Integer.parseInt(args[4]);
        int topK = Integer.parseInt(args[5]);

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
            System.out.println("Testing for r(x, y) = " + r);

            List<Relation<String, String>> inputRelations = new ArrayList<>();
            inputRelations.add(r);
            FactSampler<String, String> sampler = new FactSampler<>(bigGraph, new ArrayList<>(inputRelations));

            if (sampler.getDataTest().get(true).size() == 0) {
                System.out.println("Not enough true testing data. Skip....");
                continue;
            }
            if (sampler.getDataTest().get(false).size() == 0) {
                System.out.println("Not enough false testing data. Skip....");
                continue;
            }

            bigGraph.buildSimLabelsMap(0);

            Stopwatch w = Stopwatch.createStarted();
            List<OGFCRule<String, String>> patterns = miner.OGFC_stream(r, sampler.getDataTrain().get(true), sampler.getDataTrain().get(false));
            w.stop();

            System.out.println("Discovered number of patterns: |P| = " + patterns.size() + ", Time = " + w.elapsed(TimeUnit.SECONDS));
            System.out.println("FactChecker: OFact_R: "
                    + FactChecker.predictByHits(patterns, sampler.getDataTest()));
            System.out.println("FactChecker: OFact    "
                    + FactChecker.predictByLogisticRegression(patterns, r, sampler.getDataTrain(), sampler.getDataTest(), outputDir, "lr"));

            System.out.println("Restore the sampled facts....");
            sampler.restore();
        }
        System.out.println("-------------------DONE-----------------");
    }
}
