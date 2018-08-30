package edu.wsu.eecs.gfc.exps;

import com.google.common.base.Stopwatch;
import edu.wsu.eecs.gfc.core.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The caller to test GFC mining.
 * @author Peng Lin penglin03@gmail.com
 */
public class TestOGFC {

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

            for (int j = 0; j <= GLOBAL_HOPS; j++) {
                System.out.println("Grouping similar relations....");
                Set<Relation<String, String>> similarRelations = new HashSet<>();
                bigGraph.buildSimLabelsMap(j);

                for (String dstLabel : ontoIndex.get(r.dstLabel()).get(j)) {
                    for (Relation<String, String> rIn : bigGraph.getInRelations(dstLabel)) {
                        if (bigGraph.getSimLabels(r.srcLabel()).contains(rIn.srcLabel())) {
                            similarRelations.add(rIn);
                        }
                    }
                }
                for (String srcLabel : ontoIndex.get(r.srcLabel()).get(j)) {
                    for (Relation<String, String> rOut : bigGraph.getOutRelations(srcLabel)) {
                        if (bigGraph.getSimLabels(r.dstLabel()).contains(rOut.dstLabel())) {
                            similarRelations.add(rOut);
                        }
                    }
                }

                // Preprocessing: filter out those relations with too may candidate patterns.
                List<Relation<String, String>> tmpList = new ArrayList<>();
                for (Relation<String, String> rSim : similarRelations) {
                    if (bigGraph.getEdges(rSim).size() >= 20
                            && bigGraph.getEdges(rSim).size() <= 250
                            && bigGraph.getEdges(rSim.srcLabel()).size() <= 5000
                            && bigGraph.getEdges(rSim.srcLabel()).size() <= 5000) {
                        tmpList.add(rSim);
                    }
                }

                System.out.println(Arrays.toString(tmpList.toArray()));

                tmpList.sort(Comparator.comparingInt(
                        er -> bigGraph.getNodes(er.srcLabel()).size()
                                * bigGraph.getNodes(er.dstLabel()).size()));

                // By default, pick one similar relation for each level.
                // This is to align the number of similar relations by distance.
                // Otherwise, there may be skewed number of relations from different distances.
                if (!tmpList.isEmpty()) {
                    inputRelations.addAll(tmpList.subList(0, 1));
                }
            }

            if (inputRelations.size() != GLOBAL_HOPS + 1) {
                System.out.println("No enough relations up to d-hop neighbors. Skip....");
                continue;
            }

            System.out.println("Relation List: " + Arrays.toString(inputRelations.toArray()));

            FactSampler<String, String> sampler = new FactSampler<>(bigGraph, new ArrayList<>(inputRelations));

            if (sampler.getDataTest().get(true).size() == 0) {
                System.out.println("Not enough true testing data. Skip....");
                continue;
            }

            if (sampler.getDataTest().get(false).size() == 0) {
                System.out.println("Not enough false testing data. Skip....");
                continue;
            }

            for (int i = 0; i <= GLOBAL_HOPS; i++) {
                bigGraph.buildSimLabelsMap(i);

                Stopwatch w = Stopwatch.createStarted();
                List<OGFCRule<String, String>> patterns = miner.OGFC_stream(r, sampler.getDataTrain().get(true), sampler.getDataTrain().get(false));
                w.stop();

                System.out.println("Hop = " + i + ", r = " + r);
                System.out.println("Discovered number of patterns: |P| = " + patterns.size() + ", Time = " + w.elapsed(TimeUnit.SECONDS));
                System.out.println("FactChecker: OFact_R: "
                        + FactChecker.predictByHits(patterns, sampler.getDataTest()));
                System.out.println("FactChecker: OFact    "
                        + FactChecker.predictByLogisticRegression(patterns, r, sampler.getDataTrain(), sampler.getDataTest(), outputDir, "lr"));
            }

            System.out.println("Restore the sampled facts....");
            sampler.restore();
        }
        System.out.println("-------------------DONE-----------------");
    }
}
