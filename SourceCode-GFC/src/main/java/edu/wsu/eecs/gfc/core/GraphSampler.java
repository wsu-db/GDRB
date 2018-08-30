package edu.wsu.eecs.gfc.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Sampling graphs into different sizes for different impact factors.
 * <p>
 * @author Peng Lin penglin03@gmail.com
 */
public class GraphSampler {

    public static final String SEP = File.separator;

    public static void sampleByEdges(String inputDir, String outputDir, int maxTripleFreq, int minTripleFreq, int skip) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Graph<String, String> graph = IO.loadStringGraph(inputDir + SEP + "gfc_str_nodes.tsv", inputDir + SEP + "gfc_str_edges.tsv");
        GraphDatabase<String, String> dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("OriginalSize:\n" + dataGraph.toSizeString());

        // Remove the edges out of range.
        for (Relation<String, String> r : dataGraph.relationSet()) {
            int nr = dataGraph.getEdges(r).size();
            if (nr < minTripleFreq || nr > maxTripleFreq) {
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());


        System.out.println("Sampling relations every skip = " + skip);
        List<Relation<String, String>> relationList = new ArrayList<>(dataGraph.relationSet());
        GraphDatabase<String, String> finalDataGraph = dataGraph;
        relationList.sort(Comparator.comparingInt(r -> finalDataGraph.getEdges(r).size()));
        for (int k = 0; k < relationList.size(); k = k + skip) {
            for (int j = k + 1; j < k + skip && j < relationList.size(); j++) {
                Relation<String, String> r = relationList.get(j);
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());

        List<Node<String>> nodeList = new ArrayList<>(graph.getNodeCollection());
        nodeList.sort(Comparator.comparingInt(n -> -1 * graph.degree(n)));

        int fullSize = graph.numOfEdges();
        double[] sizeArr = {1.0, 0.8, 0.6, 0.4, 0.2};

        List<Relation<String, String>> commonRelations = new ArrayList<>();

        int index = 0;
        for (Node<String> v : nodeList) {
            if (graph.numOfEdges() <= (int) (fullSize * sizeArr[index])) {
                System.out.println("Processing Graph_" + index);
                for (Node<String> node : new HashSet<>(graph.getNodeCollection())) {
                    if (graph.degree(node) == 0) {
                        graph.removeNode(node);
                    }
                }
                dataGraph = GraphDatabase.buildFromGraph(graph);
                relationList = new ArrayList<>(dataGraph.relationSet());
                GraphDatabase<String, String> tmpDataGraph = dataGraph;
                relationList.sort(Comparator.comparingInt(r -> tmpDataGraph.getEdges(r).size()));

                System.out.println("Graph_" + index + ":\n" + dataGraph.toSizeString());

                BufferedWriter bw;

                File iOutputDir = new File(outputDir + SEP, "t" + Integer.toString(5 - index));
                if (!iOutputDir.exists()) {
                    iOutputDir.mkdirs();
                }

                System.out.println("Saving " + index + "_relations.tsv...");
                bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(5 - index) + SEP + "gfc_all_relations.tsv"));
                for (Relation<String, String> r : relationList) {
                    bw.write(r.toString() + "\t" + dataGraph.getEdges(r).size());
                    bw.newLine();
                }
                bw.close();

                if (index == 0) {
                    commonRelations = new ArrayList<>(relationList);
                } else {
                    commonRelations.retainAll(relationList);
                }

                System.out.println("Saving " + index + "_nodes.tsv....");
                bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(5 - index) + SEP + "gfc_str_nodes.tsv"));
                for (Node<String> n : graph.nodeIter()) {
                    bw.write(n.toString());
                    bw.newLine();
                }
                bw.close();

                System.out.println("Saving " + index + "_edges.tsv....");
                bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(5 - index) + SEP + "gfc_str_edges.tsv"));
                for (Edge<String, String> e : graph.edgeIter()) {
                    bw.write(e.srcId() + "\t" + e.dstId() + "\t" + e.label());
                    bw.newLine();
                }
                bw.close();

                index++;
                if (index >= sizeArr.length) {
                    break;
                }
            }

            graph.removeNode(v);
        }

        System.out.println("Saving common_relations.tsv....");
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + SEP + "common_relations.tsv"));
        for (Relation<String, String> r : commonRelations) {
            bw.write(r.toString());
            bw.newLine();
        }
        bw.close();

        System.out.println("Saving default input relations....");
        for (int gIndex = 0; gIndex < 5; gIndex++) {
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(5 - gIndex) + SEP + "gfc_input_relations.tsv"));
            for (int k = 0; k < 10 && k < commonRelations.size(); k++) {
                bw.write(commonRelations.get(k).toString());
                bw.newLine();
            }
            bw.close();
        }
    }

    public static void sampleByEdges2(String inputDir, String outputDir, int maxTripleFreq, int minTripleFreq, int skip) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Graph<String, String> graph = IO.loadStringGraph(inputDir + SEP + "gfc_str_nodes.tsv", inputDir + SEP + "gfc_str_edges.tsv");
        GraphDatabase<String, String> dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("OriginalSize:\n" + dataGraph.toSizeString());

        // Remove the edges out of range.
        for (Relation<String, String> r : dataGraph.relationSet()) {
            int nr = dataGraph.getEdges(r).size();
            if (nr < minTripleFreq || nr > maxTripleFreq) {
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());


        System.out.println("Sampling relations every skip = " + skip);
        List<Relation<String, String>> relationList = new ArrayList<>(dataGraph.relationSet());
        GraphDatabase<String, String> finalDataGraph = dataGraph;
        relationList.sort(Comparator.comparingInt(r -> finalDataGraph.getEdges(r).size()));
        for (int k = 0; k < relationList.size(); k = k + skip) {
            for (int j = k + 1; j < k + skip && j < relationList.size(); j++) {
                Relation<String, String> r = relationList.get(j);
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());

        List<Relation<String, String>> commonRelations = new ArrayList<>();


        for (int index = 5; index >= 1; index--) {
            System.out.println("Saving graph: " + index);
            relationList = new ArrayList<>(dataGraph.relationSet());
            GraphDatabase<String, String> tmpDataGraph = dataGraph;
            relationList.sort(Comparator.comparingInt(r -> tmpDataGraph.getEdges(r).size()));

            System.out.println("Graph_" + index + ":\n" + dataGraph.toSizeString());
            File iOutputDir = new File(outputDir + SEP, "t" + index);
            if (!iOutputDir.exists()) {
                iOutputDir.mkdirs();
            }

            BufferedWriter bw;

            System.out.println("Saving " + index + "_relations.tsv...");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_all_relations.tsv"));
            for (Relation<String, String> r : relationList) {
                bw.write(r.toString() + "\t" + dataGraph.getEdges(r).size());
                bw.newLine();
            }
            bw.close();

            if (index == 5) {
                commonRelations = new ArrayList<>(relationList);
            } else {
                commonRelations.retainAll(relationList);
            }

            System.out.println("Saving " + index + "_nodes.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_str_nodes.tsv"));
            for (Node<String> n : graph.nodeIter()) {
                bw.write(n.toString());
                bw.newLine();
            }
            bw.close();

            System.out.println("Saving " + index + "_edges.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_str_edges.tsv"));
            for (Edge<String, String> e : graph.edgeIter()) {
                bw.write(e.srcId() + "\t" + e.dstId() + "\t" + e.label());
                bw.newLine();
            }
            bw.close();

            if (index == 1) {
                break;
            } else {
                for (Relation<String, String> r : dataGraph.relationSet()) {
                    int n = dataGraph.getEdges(r).size() / index;
                    int cnt = 0;
                    for (Edge<String, String> e : dataGraph.getEdges(r)) {
                        if (cnt >= n) {
                            break;
                        } else {
                            graph.removeEdge(e);
                            cnt++;
                        }
                    }
                }
                for (Node<String> n : new HashSet<>(graph.getNodeCollection())) {
                    if (graph.degree(n) == 0) {
                        graph.removeNode(n);
                    }
                }
                dataGraph = GraphDatabase.buildFromGraph(graph);
            }
        }

        System.out.println("Saving common_relations.tsv....");
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + SEP + "common_relations.tsv"));
        for (Relation<String, String> r : commonRelations) {
            bw.write(r.toString());
            bw.newLine();
        }
        bw.close();

        System.out.println("Saving default input relations....");
        for (int gIndex = 0; gIndex < 5; gIndex++) {
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(5 - gIndex) + SEP + "gfc_input_relations.tsv"));
            for (int k = 0; k < 10 && k < commonRelations.size(); k++) {
                bw.write(commonRelations.get(k).toString());
                bw.newLine();
            }
            bw.close();
        }
    }

    public static void sampleByRelations(String inputDir, String outputDir, int maxTripleFreq, int minTripleFreq, int step) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Graph<String, String> graph = IO.loadStringGraph(inputDir + SEP + "gfc_str_nodes.tsv", inputDir + SEP + "gfc_str_edges.tsv");
        GraphDatabase<String, String> dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("OriginalSize:\n" + dataGraph.toSizeString());

        // Remove the edges out of range.
        for (Relation<String, String> r : dataGraph.relationSet()) {
            int nr = dataGraph.getEdges(r).size();
            if (nr < minTripleFreq || nr > maxTripleFreq) {
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }

        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());

        List<Relation<String, String>> totalRelationList = new ArrayList<>(dataGraph.relationSet());
        GraphDatabase<String, String> tmpDataGraph1 = dataGraph;
        totalRelationList.sort(Comparator.comparingInt(r -> tmpDataGraph1.getEdges(r).size()));

        Graph<String, String> outGraph = Graph.createEmptyGraph();
        List<Relation<String, String>> commonRelations = new ArrayList<>();

        for (int gIndex = 0; gIndex < 5; gIndex++) {
            for (int rIndex = gIndex; rIndex < 5 * step && rIndex < totalRelationList.size(); rIndex = rIndex + 5) {
                Relation<String, String> r = totalRelationList.get(rIndex);
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    outGraph.addNode(e.srcNode());
                    outGraph.addNode(e.dstNode());
                    outGraph.addEdge(e);
                }
            }

            BufferedWriter bw;

            GraphDatabase<String, String> outDataGraph = GraphDatabase.buildFromGraph(outGraph);
            System.out.println("Saving " + gIndex + "_relations.tsv...");
            List<Relation<String, String>> relationList = new ArrayList<>(outDataGraph.relationSet());
            relationList.sort(Comparator.comparingInt(r -> outDataGraph.getEdges(r).size()));

            if (gIndex == 0) {
                commonRelations = new ArrayList<>(relationList);
            } else {
                commonRelations.retainAll(relationList);
            }

            System.out.println("Graph_" + gIndex + ":\n" + outDataGraph.toSizeString());

            File iOutputDir = new File(outputDir + SEP, "t" + Integer.toString(gIndex + 1));
            if (!iOutputDir.exists()) {
                iOutputDir.mkdirs();
            }

            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(gIndex + 1) + SEP + "gfc_all_relations.tsv"));
            for (Relation<String, String> r : relationList) {
                bw.write(r.toString() + "\t" + outDataGraph.getEdges(r).size());
                bw.newLine();
            }
            bw.close();

            System.out.println("Saving " + gIndex + "/gfc_str_nodes.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(gIndex + 1) + SEP + "gfc_str_nodes.tsv"));
            for (Node<String> n : outGraph.nodeIter()) {
                bw.write(n.toString());
                bw.newLine();
            }
            bw.close();

            System.out.println("Saving " + gIndex + "/gfc_str_edges.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(gIndex + 1) + SEP + "gfc_str_edges.tsv"));
            for (Edge<String, String> e : outGraph.edgeIter()) {
                bw.write(e.srcId() + "\t" + e.dstId() + "\t" + e.label());
                bw.newLine();
            }
            bw.close();
        }

        System.out.println("Saving common_relations.tsv....");
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + SEP + "common_relations.tsv"));
        for (Relation<String, String> r : commonRelations) {
            bw.write(r.toString());
            bw.newLine();
        }
        bw.close();

        System.out.println("Saving default input relations....");
        for (int gIndex = 0; gIndex < 5; gIndex++) {
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(gIndex + 1) + SEP + "gfc_input_relations.tsv"));
            for (int k = 0; k < 10 && k < commonRelations.size(); k++) {
                bw.write(commonRelations.get(k).toString());
                bw.newLine();
            }
            bw.close();
        }
    }

    public static void sampleByDegree(String inputDir, String outputDir, int maxTripleFreq, int minTripleFreq, int skip, int offset) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Graph<String, String> graph = IO.loadStringGraph(inputDir + SEP + "gfc_str_nodes.tsv", inputDir + SEP + "gfc_str_edges.tsv");
        GraphDatabase<String, String> dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("OriginalSize:\n" + dataGraph.toSizeString());

        // Remove the edges out of range.
        for (Relation<String, String> r : dataGraph.relationSet()) {
            int nr = dataGraph.getEdges(r).size();
            if (nr < minTripleFreq || nr > maxTripleFreq) {
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());


        // Sample relations every skip.
        System.out.println("Sampling relations every skip = " + skip);
        List<Relation<String, String>> relationList = new ArrayList<>(dataGraph.relationSet());
        GraphDatabase<String, String> finalDataGraph = dataGraph;
        relationList.sort(Comparator.comparingInt(r -> finalDataGraph.getEdges(r).size()));
        for (int k = 0; k < relationList.size(); k = k + skip) {
            for (int j = k + 1; j < k + skip && j < relationList.size(); j++) {
                Relation<String, String> r = relationList.get(j);
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        // Remove 0-degree nodes.
        for (Node<String> n : new ArrayList<>(graph.getNodeCollection())) {
            if (graph.degree(n) == 0) {
                graph.removeNode(n);
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());

        List<Relation<String, String>> commonRelations = new ArrayList<>();

        int theNumberOfNodes = graph.numOfNodes();

        for (int index = 5; index >= 1; index--) {
            System.out.println("Saving graph: " + index);
            relationList = new ArrayList<>(dataGraph.relationSet());
            GraphDatabase<String, String> tmpDataGraph = dataGraph;
            relationList.sort(Comparator.comparingInt(r -> tmpDataGraph.getEdges(r).size()));

            System.out.println("Graph_" + index + ":\n" + dataGraph.toSizeString());

            System.out.println("Average Degree = " + (2 * graph.numOfEdges() / (double) theNumberOfNodes));

            File iOutputDir = new File(outputDir + SEP, "t" + index);
            if (!iOutputDir.exists()) {
                iOutputDir.mkdirs();
            }

            BufferedWriter bw;

            System.out.println("Saving " + index + "_relations.tsv...");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_all_relations.tsv"));
            for (Relation<String, String> r : relationList) {
                bw.write(r.toString() + "\t" + dataGraph.getEdges(r).size());
                bw.newLine();
            }
            bw.close();

            if (index == 5) {
                commonRelations = new ArrayList<>(relationList);
            } else {
                commonRelations.retainAll(relationList);
            }

            System.out.println("Saving " + index + "_nodes.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_str_nodes.tsv"));
            for (Node<String> n : graph.nodeIter()) {
                bw.write(n.toString());
                bw.newLine();
            }
            bw.close();

            System.out.println("Saving " + index + "_edges.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_str_edges.tsv"));
            for (Edge<String, String> e : graph.edgeIter()) {
                bw.write(e.srcId() + "\t" + e.dstId() + "\t" + e.label());
                bw.newLine();
            }
            bw.close();

            if (index == 1) {
                break;
            } else {
                List<Node<String>> nodeList = new ArrayList<>(graph.getNodeCollection());
                nodeList.sort(Comparator.comparingInt(n -> -1 * graph.degree(n)));

                Map<Node<String>, Integer> degreeMap = new LinkedHashMap<>();
                for (Node<String> n : nodeList) {
                    degreeMap.put(n, graph.degree(n) / (index + offset));
                }

                for (Node<String> n : degreeMap.keySet()) {
                    for (Edge<String, String> eOut : new ArrayList<>(graph.edgesFrom(n))) {
                        int counter = degreeMap.get(n);
                        if (counter == 0) {
                            break;
                        } else {
                            int tmpCounter = degreeMap.get(eOut.dstNode());
                            if (tmpCounter > 0) {
                                degreeMap.put(eOut.dstNode(), tmpCounter - 1);
                                degreeMap.put(n, counter - 1);
                                graph.removeEdge(eOut);
                            }
                        }
                    }
                    for (Edge<String, String> eIn : new ArrayList<>(graph.edgesTo(n))) {
                        int counter = degreeMap.get(n);
                        if (counter == 0) {
                            break;
                        } else {
                            int tmpCounter = degreeMap.get(eIn.srcNode());
                            if (tmpCounter > 0) {
                                degreeMap.put(eIn.srcNode(), tmpCounter - 1);
                                degreeMap.put(n, counter - 1);
                                graph.removeEdge(eIn);
                            }
                        }
                    }
                }

                // Just cleanup isolated nodes for convenience. The actual number of nodes includes the isolated nodes.
                // Thus, the |V| is the |V| of graph t5.
                for (Node<String> n : new ArrayList<>(graph.getNodeCollection())) {
                    if (graph.degree(n) == 0) {
                        graph.removeNode(n);
                    }
                }

                dataGraph = GraphDatabase.buildFromGraph(graph);
            }
        }

        System.out.println("Saving common_relations.tsv....");
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + SEP + "common_relations.tsv"));
        for (Relation<String, String> r : commonRelations) {
            bw.write(r.toString());
            bw.newLine();
        }
        bw.close();

        System.out.println("Saving default input relations....");
        for (int gIndex = 0; gIndex < 5; gIndex++) {
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(5 - gIndex) + SEP + "gfc_input_relations.tsv"));
            for (int k = 0; k < 10 && k < commonRelations.size(); k++) {
                bw.write(commonRelations.get(k).toString());
                bw.newLine();
            }
            bw.close();
        }
    }

    public static void sampleByDegreeDiv2(String inputDir, String outputDir, int maxTripleFreq, int minTripleFreq, int skip, int offset) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Graph<String, String> graph = IO.loadStringGraph(inputDir + SEP + "gfc_str_nodes.tsv", inputDir + SEP + "gfc_str_edges.tsv");
        GraphDatabase<String, String> dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("OriginalSize:\n" + dataGraph.toSizeString());

        List<Relation<String, String>> inputRelations = IO.loadRelationList(inputDir + SEP + "gfc_input_relations.tsv");

        // Remove the edges out of range.
        for (Relation<String, String> r : dataGraph.relationSet()) {
            int nr = dataGraph.getEdges(r).size();
            if (nr < minTripleFreq || nr > maxTripleFreq) {
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());


        // Sample relations every skip.
        System.out.println("Sampling relations every skip = " + skip);
        List<Relation<String, String>> relationList = new ArrayList<>(dataGraph.relationSet());
        GraphDatabase<String, String> finalDataGraph = dataGraph;
        relationList.sort(Comparator.comparingInt(r -> finalDataGraph.getEdges(r).size()));
        for (int k = 0; k < relationList.size(); k = k + skip) {
            for (int j = k + 1; j < k + skip && j < relationList.size(); j++) {
                Relation<String, String> r = relationList.get(j);
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        // Remove 0-degree nodes.
        for (Node<String> n : new ArrayList<>(graph.getNodeCollection())) {
            if (graph.degree(n) == 0) {
                graph.removeNode(n);
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());

        List<Relation<String, String>> commonRelations = new ArrayList<>();

        for (int k = inputRelations.size() - 1; k >= 0; k--) {
            Relation<String, String> r = inputRelations.get(k);
            if (!dataGraph.relationSet().contains(r)) {
                System.out.println("Relation: " + r.toString() + "is out of range....");
                inputRelations.remove(r);
            }
        }

        Set<Node<String>> xySet = new HashSet<>();
        for (Relation<String, String> r : inputRelations) {
            xySet.addAll(dataGraph.getSrcNodes(r));
            xySet.addAll(dataGraph.getDstNodes(r));
        }

        int theNumberOfNodes = graph.numOfNodes();

        for (int index = 5; index >= 1; index--) {
            System.out.println("Saving graph: " + index);
            relationList = new ArrayList<>(dataGraph.relationSet());
            GraphDatabase<String, String> tmpDataGraph = dataGraph;
            relationList.sort(Comparator.comparingInt(r -> tmpDataGraph.getEdges(r).size()));

            System.out.println("Graph_" + index + ":\n" + dataGraph.toSizeString());

            System.out.println("Average Degree = " + (2 * graph.numOfEdges() / (double) theNumberOfNodes));

            File iOutputDir = new File(outputDir + SEP, "t" + index);
            if (!iOutputDir.exists()) {
                iOutputDir.mkdirs();
            }

            BufferedWriter bw;

            System.out.println("Saving " + index + "_relations.tsv...");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_all_relations.tsv"));
            for (Relation<String, String> r : relationList) {
                bw.write(r.toString() + "\t" + dataGraph.getEdges(r).size());
                bw.newLine();
            }
            bw.close();

            if (index == 5) {
                commonRelations = new ArrayList<>(relationList);
            } else {
                commonRelations.retainAll(relationList);
            }

            System.out.println("Saving " + index + "_nodes.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_str_nodes.tsv"));
            for (Node<String> n : graph.nodeIter()) {
                bw.write(n.toString());
                bw.newLine();
            }
            bw.close();

            System.out.println("Saving " + index + "_edges.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + index + SEP + "gfc_str_edges.tsv"));
            for (Edge<String, String> e : graph.edgeIter()) {
                bw.write(e.srcId() + "\t" + e.dstId() + "\t" + e.label());
                bw.newLine();
            }
            bw.close();

            if (index == 1) {
                break;
            } else {
                List<Node<String>> nodeList = new ArrayList<>(graph.getNodeCollection());

                nodeList.sort(Comparator.comparingInt(n -> -1 * graph.degree(n)));
//                nodeList.sort(Comparator.comparingInt(n -> +1 * graph.degree(n)));

                Map<Node<String>, Integer> degreeMap = new LinkedHashMap<>();
                for (Node<String> n : nodeList) {
//                    if (!xySet.contains(n)) {
                    degreeMap.put(n, graph.degree(n) / (2 + offset));
//                    } else {
//                        degreeMap.put(n, 0);
//                    }
                }

                for (Node<String> n : degreeMap.keySet()) {
                    for (Edge<String, String> eOut : new ArrayList<>(graph.edgesFrom(n))) {
                        int counter = degreeMap.get(n);
                        if (counter == 0) {
                            break;
                        } else {
                            int tmpCounter = degreeMap.get(eOut.dstNode());
                            if (!xySet.contains(eOut.dstNode())) {
                                degreeMap.put(eOut.dstNode(), tmpCounter - 1);
                                degreeMap.put(n, counter - 1);
                                graph.removeEdge(eOut);
                            } else if (xySet.contains(n)) {
                                degreeMap.put(eOut.dstNode(), tmpCounter - 1);
                                degreeMap.put(n, counter - 1);
                                graph.removeEdge(eOut);
                            }
                        }
                    }
                    for (Edge<String, String> eIn : new ArrayList<>(graph.edgesTo(n))) {
                        int counter = degreeMap.get(n);
                        if (counter == 0) {
                            break;
                        } else {
                            int tmpCounter = degreeMap.get(eIn.srcNode());
                            if (!xySet.contains(eIn.srcNode())) {
                                degreeMap.put(eIn.srcNode(), tmpCounter - 1);
                                degreeMap.put(n, counter - 1);
                                graph.removeEdge(eIn);
                            } else if (xySet.contains(n)) {
                                degreeMap.put(eIn.srcNode(), tmpCounter - 1);
                                degreeMap.put(n, counter - 1);
                                graph.removeEdge(eIn);
                            }
                        }
                    }
                }

                // Just cleanup isolated nodes for convenience. The actual number of nodes includes the isolated nodes.
                // Thus, the |V| is the |V| of graph t5.
                for (Node<String> n : new ArrayList<>(graph.getNodeCollection())) {
                    if (graph.degree(n) == 0) {
                        graph.removeNode(n);
                    }
                }

                dataGraph = GraphDatabase.buildFromGraph(graph);
            }
        }

        System.out.println("Saving common_relations.tsv....");
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + SEP + "common_relations.tsv"));
        for (Relation<String, String> r : commonRelations) {
            bw.write(r.toString());
            bw.newLine();
        }
        bw.close();

        System.out.println("Saving default input relations....");
        for (int gIndex = 0; gIndex < 5; gIndex++) {
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(5 - gIndex) + SEP + "gfc_input_relations.tsv"));
            for (Relation<String, String> r : inputRelations) {
                bw.write(r.toString());
                bw.newLine();
            }
            bw.close();
        }
    }

    public static void sampleByRadius(String inputDir, String outputDir, int maxTripleFreq, int minTripleFreq, int skip) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Graph<String, String> graph = IO.loadStringGraph(inputDir + SEP + "gfc_str_nodes.tsv", inputDir + SEP + "gfc_str_edges.tsv");
        GraphDatabase<String, String> dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("OriginalSize:\n" + dataGraph.toSizeString());

        List<Relation<String, String>> inputRelations = IO.loadRelationList(inputDir + SEP + "gfc_input_relations.tsv");

        // Remove the edges out of range.
        for (Relation<String, String> r : dataGraph.relationSet()) {
            int nr = dataGraph.getEdges(r).size();
            if (nr < minTripleFreq || nr > maxTripleFreq) {
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());

        System.out.println("Sampling relations every skip = " + skip);
        List<Relation<String, String>> relationList = new ArrayList<>(dataGraph.relationSet());
        GraphDatabase<String, String> finalDataGraph = dataGraph;
        relationList.sort(Comparator.comparingInt(r -> finalDataGraph.getEdges(r).size()));
        for (int k = 0; k < relationList.size(); k = k + skip) {
            for (int j = k + 1; j < k + skip && j < relationList.size(); j++) {
                Relation<String, String> r = relationList.get(j);
                for (Edge<String, String> e : dataGraph.getEdges(r)) {
                    graph.removeEdge(e);
                }
            }
        }
        dataGraph = GraphDatabase.buildFromGraph(graph);
        System.out.println("SampledSize:\n" + dataGraph.toSizeString());

        List<Relation<String, String>> commonRelations = new ArrayList<>();

        for (int k = inputRelations.size() - 1; k >= 0; k--) {
            Relation<String, String> r = inputRelations.get(k);
            if (!dataGraph.relationSet().contains(r)) {
                System.out.println("Relation: " + r.toString() + "is out of range....");
                inputRelations.remove(r);
            }
        }

        Graph<String, String> outGraph = Graph.createEmptyGraph();
        for (Relation<String, String> r : inputRelations) {
            for (Node<String> vx : dataGraph.getSrcNodes(r)) {
                outGraph.addNode(vx);
            }
            for (Node<String> vy : dataGraph.getDstNodes(r)) {
                outGraph.addNode(vy);
            }
            for (Edge<String, String> e : dataGraph.getEdges(r)) {
                outGraph.addEdge(e);
            }
        }
        Set<Node<String>> newBorderNodes = new HashSet<>(outGraph.getNodeCollection());

        for (int radius = 1; radius <= 3; radius++) {
            System.out.println("radius = " + radius);
            Set<Node<String>> borderNodes = new HashSet<>(newBorderNodes);
            newBorderNodes.clear();
            // Enlarge the outGraph by 1-hop
            for (Node<String> n : borderNodes) {
                for (Edge<String, String> eOut : graph.edgesFrom(n)) {
                    Node<String> dst = eOut.dstNode();
                    if (!outGraph.hasNode(dst)) {
                        newBorderNodes.add(dst);
                        outGraph.addNode(dst);
                    }
                    outGraph.addEdge(eOut);
                }
                for (Edge<String, String> eIn : graph.edgesTo(n)) {
                    Node<String> src = eIn.srcNode();
                    if (!outGraph.hasNode(src)) {
                        newBorderNodes.add(src);
                        outGraph.addNode(src);
                    }
                    outGraph.addEdge(eIn);
                }
            }

            GraphDatabase outGraphData = GraphDatabase.buildFromGraph(outGraph);

            // Dump the graph file
            System.out.println("Saving graph: " + radius);
            relationList = new ArrayList<>(inputRelations);
            GraphDatabase<String, String> tmpDataGraph = outGraphData;
            relationList.sort(Comparator.comparingInt(r -> tmpDataGraph.getEdges(r).size()));

            System.out.println("Graph_" + radius + ":\n" + outGraph.toSizeString());
            File iOutputDir = new File(outputDir + SEP, "t" + radius);
            if (!iOutputDir.exists()) {
                iOutputDir.mkdirs();
            }

            BufferedWriter bw;


            System.out.println("Saving " + radius + "_relations.tsv...");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + radius + SEP + "gfc_all_relations.tsv"));
            for (Relation<String, String> r : relationList) {
                bw.write(r.toString() + "\t" + outGraphData.getEdges(r).size());
                bw.newLine();
            }
            bw.close();

            if (radius == 1) {
                commonRelations = new ArrayList<>(relationList);
            } else {
                commonRelations.retainAll(relationList);
            }

            System.out.println("Saving " + radius + "_nodes.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + radius + SEP + "gfc_str_nodes.tsv"));
            for (Node<String> n : outGraph.nodeIter()) {
                bw.write(n.toString());
                bw.newLine();
            }
            bw.close();

            System.out.println("Saving " + radius + "_edges.tsv....");
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + radius + SEP + "gfc_str_edges.tsv"));
            for (Edge<String, String> e : outGraph.edgeIter()) {
                bw.write(e.srcId() + "\t" + e.dstId() + "\t" + e.label());
                bw.newLine();
            }
            bw.close();
        }

        System.out.println("Saving common_relations.tsv....");
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + SEP + "common_relations.tsv"));
        for (Relation<String, String> r : commonRelations) {
            bw.write(r.toString());
            bw.newLine();
        }
        bw.close();

        System.out.println("Saving default input relations....");
        for (int radius = 1; radius <= 3; radius++) {
            bw = new BufferedWriter(new FileWriter(outputDir + SEP + "t" + Integer.toString(radius) + SEP + "gfc_input_relations.tsv"));
            for (int k = 0; k < inputRelations.size(); k++) {
                bw.write(inputRelations.get(k).toString());
                bw.newLine();
            }
            bw.close();
        }
    }
}
