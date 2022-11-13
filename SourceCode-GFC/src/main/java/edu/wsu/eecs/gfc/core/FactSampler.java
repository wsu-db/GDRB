package edu.wsu.eecs.gfc.core;
import java.io.*;
import java.util.*;


/**
 * Sample a set of positive and negative facts from the graph.
 * Note: the positive testing data (edges) will be removed from the graph.
 * <p>
 * @author Peng Lin penglin03@gmail.com
 */
public class FactSampler{
    private GraphDatabase<String, String> bigGraph;
    private List<Relation<String, String>> relationList;

    private Map<Boolean, List<Edge<String, String>>> dataTrain;
    private Map<Boolean, List<Edge<String, String>>> dataTest;

    public FactSampler(String inputDir) throws IOException {

        dataTrain = new HashMap<>();
        dataTest = new HashMap<>();

    }

    public GraphDatabase<String, String> getBigGraph() {
        return bigGraph;
    }

    public List<Relation<String, String>> getRelationList() {
        return relationList;
    }

    public Relation<String, String> getMajorRelation() {
        return relationList.get(0);
    }

    public Map<Boolean, List<Edge<String, String>>> getDataTest() {
        return dataTest;
    }

    public Map<Boolean, List<Edge<String, String>>> getDataTrain() {
        return dataTrain;
    }

    public void extract_training_asserions(String inputDir,Relation<String,String> r) throws IOException {
        dataTrain.put(true, new ArrayList<>());
        dataTrain.put(false, new ArrayList<>());

        BufferedReader br;
        String line;
        br = new BufferedReader(new FileReader(new File(inputDir, "facts_train.csv")));
        List<Edge<String, String>> posExamples = new ArrayList<>();
        List<Edge<String, String>> negExamples = new ArrayList<>();
        while ((line = br.readLine()) != null) {    
            String[] tokens = line.split(",");
            String snID = tokens[0].intern();
            String snLabel = tokens[1].intern();
            String dnID = tokens[3].intern();
            String dnLabel = tokens[4].intern();
            String label = tokens[2].intern();
            String eclass = tokens[5].intern();
            if(snLabel==r.srcLabel() && dnLabel==r.dstLabel() && label==r.edgeLabel()){
                Node<String> srcNode = (Node<String>) Node.createLabeledNode(snID,snLabel);
                Node<String> dstNode = (Node<String>) Node.createLabeledNode(dnID,dnLabel); 
                Edge<String, String> assertion = (Edge<String, String>) Edge.createLabeledEdge(srcNode,dstNode,label);
                if(eclass == "1"){
                    posExamples.add(assertion);
                }
                else{
                    negExamples.add(assertion);
                }
            }
        }
        br.close();

        for (int i = 0; i < posExamples.size(); i++) {
            dataTrain.get(true).add(posExamples.get(i));
        }

        for (int i = 0; i < negExamples.size(); i++) {
            dataTrain.get(false).add(negExamples.get(i));
        }

    }

    public void extract_testing_asserions(String inputDir,Relation<String,String> r) throws IOException {
        dataTest.put(true, new ArrayList<>());
        dataTest.put(false, new ArrayList<>());

        BufferedReader br;
        String line;
        br = new BufferedReader(new FileReader(new File(inputDir, "facts_test.csv")));
        List<Edge<String, String>> posExamples = new ArrayList<>();
        List<Edge<String, String>> negExamples = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split(",");
            String snID = tokens[0].intern();
            String snLabel = tokens[1].intern();
            String dnID = tokens[3].intern();
            String dnLabel = tokens[4].intern();
            String label = tokens[2].intern();
            String eclass = tokens[5].intern();
            if(snLabel==r.srcLabel() && dnLabel==r.dstLabel() && label==r.edgeLabel()){
                Node<String> srcNode = Node.createLabeledNode(snID,snLabel);
                Node<String> dstNode = Node.createLabeledNode(dnID,dnLabel); 
                Edge<String, String> assertion = Edge.createLabeledEdge(srcNode,dstNode,label);

                if(eclass == "1"){
                    posExamples.add(assertion);
                }
                else{
                    negExamples.add(assertion);
                }
            }
        }
        br.close();

        for (int i = 0; i < posExamples.size(); i++) {
            dataTest.get(true).add(posExamples.get(i));
        }

        for (int i = 0; i < negExamples.size(); i++) {
            dataTest.get(false).add(negExamples.get(i));
        }
    }

    // public void restore() {
    //     for (Relation<VT, ET> r : relationList) {
    //         for (Edge<VT, ET> e : dataTest.get(true)) {
    //             bigGraph.getGraph().addEdge(e);
    //             bigGraph.getEdges(r.edgeLabel()).add(e);
    //             bigGraph.getEdges(r).add(e);
    //         }
    //     }
    // }
}
