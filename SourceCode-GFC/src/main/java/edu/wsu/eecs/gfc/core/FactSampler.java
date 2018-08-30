package edu.wsu.eecs.gfc.core;

import java.util.*;

/**
 * Sample a set of positive and negative facts from the graph.
 * Note: the positive testing data (edges) will be removed from the graph.
 * <p>
 * @author Peng Lin penglin03@gmail.com
 */
public class FactSampler<VT, ET> {
    private GraphDatabase<VT, ET> bigGraph;
    private List<Relation<VT, ET>> relationList;

    private Map<Boolean, List<Edge<VT, ET>>> dataTrain;
    private Map<Boolean, List<Edge<VT, ET>>> dataTest;

    private static final Random DEFAULT_SEED = new Random(357);
    private static final double DEFAULT_PERCENTAGE_OF_TRAIN = 0.8;
    private static final double DEFAULT_NEG_POS_RATIO = 4;

    public FactSampler(GraphDatabase<VT, ET> bigGraph, List<Relation<VT, ET>> relationList) {
        this.bigGraph = bigGraph;
        this.relationList = relationList;
        dataTrain = new HashMap<>();
        dataTest = new HashMap<>();
        extract();
    }

    public GraphDatabase<VT, ET> getBigGraph() {
        return bigGraph;
    }

    public List<Relation<VT, ET>> getRelationList() {
        return relationList;
    }

    public Relation<VT, ET> getMajorRelation() {
        return relationList.get(0);
    }

    public Map<Boolean, List<Edge<VT, ET>>> getDataTest() {
        return dataTest;
    }

    public Map<Boolean, List<Edge<VT, ET>>> getDataTrain() {
        return dataTrain;
    }

    private void extract() {
        dataTrain.put(true, new ArrayList<>());
        dataTrain.put(false, new ArrayList<>());
        dataTest.put(true, new ArrayList<>());
        dataTest.put(false, new ArrayList<>());

        for (Relation<VT, ET> r : relationList) {
            int nPos = bigGraph.getEdges(r).size();
            int nNeg = (int) (nPos * DEFAULT_NEG_POS_RATIO);

            List<Edge<VT, ET>> posExamples = new ArrayList<>(bigGraph.getEdges(r));
            Set<Integer> posTestIndex = new HashSet<>();
            for (int i : DEFAULT_SEED.ints(0, posExamples.size()).distinct()
                    .limit((int) (posExamples.size() * (1 - DEFAULT_PERCENTAGE_OF_TRAIN))).toArray()) {
                posTestIndex.add(i);
            }

            for (int i = 0; i < posExamples.size(); i++) {
                if (!posTestIndex.contains(i)) {
                    dataTrain.get(true).add(posExamples.get(i));
                } else {
                    dataTest.get(true).add(posExamples.get(i));
                }
            }

            List<Edge<VT, ET>> negExamples = new ArrayList<>(nNeg);
            Set<Node<VT>> xSet = new HashSet<>(bigGraph.getNodes(r.srcLabel()));
            Set<Node<VT>> ySet = new HashSet<>(bigGraph.getNodes(r.dstLabel()));
            if (xSet.size() > bigGraph.getSrcNodes(r).size() && ySet.size() > bigGraph.getDstNodes(r).size()) {
                // Partial Closed World Assumption (PCWA)
                System.out.println("Sampling the examples by PCWA....");
                xSet.removeAll(bigGraph.getSrcNodes(r));
                ySet.removeAll(bigGraph.getDstNodes(r));
                List<Node<VT>> xList = new ArrayList<>(xSet);
                List<Node<VT>> yList = new ArrayList<>(ySet);
                Collections.shuffle(xList, DEFAULT_SEED);
                Collections.shuffle(yList, DEFAULT_SEED);
                for (int i = 0; i < nNeg; i++) {
                    Node<VT> vx = xList.get(i % xList.size());
                    Node<VT> vy = yList.get(i % yList.size());
                    negExamples.add(Edge.createUnlabeledEdge(vx, vy));
                }
            } else {
                // Closed World Assumption (CWA)
                System.out.println("Sampling the examples by CWA....");
                List<Node<VT>> xList = new ArrayList<>(xSet);
                List<Node<VT>> yList = new ArrayList<>(ySet);
                Collections.shuffle(xList, DEFAULT_SEED);
                Collections.shuffle(yList, DEFAULT_SEED);
                for (int i = 0; i < 2 * nNeg; i++) {
                    if (negExamples.size() > nNeg) {
                        break;
                    }
                    Node<VT> vx = xList.get(i % xList.size());
                    for (int j = 0; j < 2 * nNeg; j++) {
                        if (negExamples.size() > nNeg) {
                            break;
                        }
                        Node<VT> vy = yList.get(i % yList.size());
                        Edge<VT, ET> exy = bigGraph.getGraph().getEdge(vx, vy);
                        if (exy == null || !exy.label().equals(r.edgeLabel())) {
                            negExamples.add(Edge.createUnlabeledEdge(vx, vy));
                        }
                    }
                }
            }

            if (negExamples.size() > nNeg) {
                negExamples = new ArrayList<>(negExamples.subList(0, nNeg));
            }
            int negCut = (int) (negExamples.size() * DEFAULT_PERCENTAGE_OF_TRAIN);

            if (negExamples.size() != 0) {
                dataTrain.get(false).addAll(new ArrayList<>(negExamples.subList(0, negCut)));
                dataTest.get(false).addAll(new ArrayList<>(negExamples.subList(negCut + 1, negExamples.size())));
            }
        }

        for (Relation<VT, ET> r : relationList) {
            for (Edge<VT, ET> e : dataTest.get(true)) {
                bigGraph.getGraph().removeEdge(e);
                bigGraph.getEdges(r.edgeLabel()).remove(e);
                bigGraph.getEdges(r).remove(e);
            }
        }
    }

    public void restore() {
        for (Relation<VT, ET> r : relationList) {
            for (Edge<VT, ET> e : dataTest.get(true)) {
                bigGraph.getGraph().addEdge(e);
                bigGraph.getEdges(r.edgeLabel()).add(e);
                bigGraph.getEdges(r).add(e);
            }
        }
    }
}
