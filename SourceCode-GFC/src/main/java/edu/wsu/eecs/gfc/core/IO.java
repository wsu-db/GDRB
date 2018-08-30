package edu.wsu.eecs.gfc.core;

import java.io.*;
import java.util.*;

/**
 * Loaders, savers, and other stuffs with IO.
 * <p>
 * @author Peng Lin penglin03@gmail.com
 */
public class IO {

    public static Map<Integer, Relation<Integer, Integer>> readRelationFile(String relationFile, Map<String, Integer> lutLabelStr2Int) {
        Map<Integer, Relation<Integer, Integer>> relations = new LinkedHashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(relationFile));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    String[] tokens = line.split(",");
                    Integer id = Integer.parseInt(tokens[0]);
                    Relation<Integer, Integer> r = Relation.createRelation(
                            lutLabelStr2Int.get(tokens[8]),
                            lutLabelStr2Int.get(tokens[9]),
                            lutLabelStr2Int.get(tokens[10])
                    );
                    relations.put(id, r);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return relations;
    }

    public static void loadGroundTruth(String filename, List<String[]> gtExamples) {
        if (!gtExamples.isEmpty()) {
            throw new RuntimeException("Non-empty lists");
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    String[] tokens = line.split(",");
                    gtExamples.add(tokens);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Graph<Integer, Integer> loadFromFile(String vertexFile, String edgeFile) {
        Graph<Integer, Integer> graph = Graph.createEmptyGraph();
        try {
            BufferedReader br;
            String line;

            br = new BufferedReader(new FileReader(vertexFile));
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\t");
                Integer id = Integer.parseInt(tokens[0]);
                Integer label = Integer.parseInt(tokens[1]);
                Node<Integer> v = Node.createLabeledNode(id, label);
                graph.addNode(v);
            }
            br.close();

            br = new BufferedReader(new FileReader(edgeFile));
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\t");
                Integer srcId = Integer.parseInt(tokens[0]);
                Integer dstId = Integer.parseInt(tokens[1]);
                Integer edgeLabel = Integer.parseInt(tokens[2]);
                graph.createEdge(srcId, dstId, edgeLabel);
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return graph;
    }

    public static Map<Integer, String> loadLUT(String lutFile) {
        Map<Integer, String> lut = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(lutFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\t");
                Integer key = Integer.parseInt(tokens[0]);
                if (lut.containsKey(key)) {
                    throw new RuntimeException("Duplicate keys in lookup tables.");
                }
                String value = tokens[1].intern();
                lut.put(key, value);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lut;
    }

    public static List<Relation<String, String>> loadRelationList(String relationFile) throws IOException {
        List<Relation<String, String>> relationList = new ArrayList<>();
        BufferedReader br;
        String line;
        br = new BufferedReader(new FileReader(relationFile));
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                String[] tokens = line.split("\t");
                String srcLabel = tokens[0].intern();
                String dstLabel = tokens[1].intern();
                String edgeLabel = tokens[2].intern();
                Relation<String, String> r = Relation.createRelation(srcLabel, dstLabel, edgeLabel);
                relationList.add(r);
            }
        }
        br.close();
        return relationList;
    }

    public static List<Relation<String, String>> loadRelations(String inputDir) throws IOException {
        List<Relation<String, String>> relationList = new ArrayList<>();
        BufferedReader br;
        String line;
        br = new BufferedReader(new FileReader(new File(inputDir, "gfc_input_relations.tsv")));
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                String[] tokens = line.split("\t");
                String srcLabel = tokens[0].intern();
                String dstLabel = tokens[1].intern();
                String edgeLabel = tokens[2].intern();
                Relation<String, String> r = Relation.createRelation(srcLabel, dstLabel, edgeLabel);
                relationList.add(r);
            }
        }
        br.close();
        return relationList;
    }

    public static Graph<String, String> loadStringGraph(String vertexFile, String edgeFile) throws IOException {
        Graph<String, String> graph = Graph.createEmptyGraph();
        BufferedReader br;
        String line;

        br = new BufferedReader(new FileReader(vertexFile));
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            String id = tokens[0].intern();
            String label = tokens[1].intern();
            Node<String> v = Node.createLabeledNode(id, label);
            graph.addNode(v);
        }
        br.close();

        br = new BufferedReader(new FileReader(edgeFile));
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            String srcId = tokens[0].intern();
            String dstId = tokens[1].intern();
            String edgeLabel = tokens[2].intern();
            graph.createEdge(srcId, dstId, edgeLabel);
        }
        br.close();

        return graph;
    }

    public static void saveStringGraph(Graph<String, String> graph, String vertexFile, String edgeFile) {
        try {
            File file = new File(vertexFile);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for (Node<String> v : graph.nodeIter()) {
                bw.write(v.toString());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File file = new File(edgeFile);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for (Edge e : graph.edgeIter()) {
                bw.write(e.srcId() + "\t" + e.dstId() + "\t" + e.label());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Graph<String, String> loadStringGraph(String inputDir) throws IOException {
        Graph<String, String> graph = Graph.createEmptyGraph();
        BufferedReader br;
        String line;
        br = new BufferedReader(new FileReader(new File(inputDir, "gfc_str_nodes.tsv")));
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            String id = tokens[0].intern();
            String label = tokens[1].intern();
            graph.createNode(id, label);
        }
        br.close();

        br = new BufferedReader(new FileReader(new File(inputDir, "gfc_str_edges.tsv")));
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            String srcId = tokens[0].intern();
            String dstId = tokens[1].intern();
            String edgeLabel = tokens[2].intern();
            graph.createEdge(srcId, dstId, edgeLabel);
        }
        br.close();

        return graph;
    }

    public static Map<String, Set<String>> loadOntology(String inputDir) throws IOException {
        Map<String, Set<String>> subclassMap = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(new File(inputDir, "gfc_str_ontology.tsv")));
        String line;
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            String child = tokens[0];
            subclassMap.putIfAbsent(child, new HashSet<>());
            for (int i = 1; i < tokens.length; i++) {
                if (!tokens[i].equals(child)) {
                    subclassMap.get(child).add(tokens[i]);
                }
            }
        }
        br.close();

        return subclassMap;
    }

    public static DirectedAcyclicGraph<String, String> loadDAGOntology(String inputDir) throws IOException {
        Graph<String, String> g = Graph.createEmptyGraph();
        BufferedReader br = new BufferedReader(new FileReader(new File(inputDir, "gfc_str_ontology.tsv")));
        String line;
        String edgeLabel = "subClassOf";
        while ((line = br.readLine()) != null) {
            String tokens[] = line.split("\t");
            String child = tokens[0];
            String parent = tokens[3];
            if (!g.hasNodeId(child)) {
                g.createNode(child, child);
            }
            if (!g.hasNodeId(parent)) {
                g.createNode(parent, parent);
            }
            if (!g.hasEdge(child, parent)) {
                g.createEdge(child, parent, edgeLabel);
            }
        }
        br.close();

        return DirectedAcyclicGraph.createFromGraph(g, true, true);
    }
}
