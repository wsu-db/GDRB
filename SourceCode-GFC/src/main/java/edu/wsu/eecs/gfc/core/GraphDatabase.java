package edu.wsu.eecs.gfc.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A very simple in-memory graph database,
 * which provides primitive variant index for nodes, edges, labels, and relations.
 */
public class GraphDatabase<VT, ET> {

    private final Graph<VT, ET> graph;

    private Map<VT, Set<Node<VT>>> indexOfNodeLabels;

    private Map<ET, Set<Edge<VT, ET>>> indexOfEdgeLabels;

    private Map<Relation<VT, ET>, Set<Edge<VT, ET>>> indexOfRelations_e;

    private Map<Relation<VT, ET>, Map<Node<VT>, Set<Node<VT>>>> relationSrcDsts;

    private Map<Relation<VT, ET>, Map<Node<VT>, Set<Node<VT>>>> relationDstSrcs;

    private Map<VT, Set<Relation<VT, ET>>> indexOfsrcLabels;

    private Map<VT, Set<Relation<VT, ET>>> indexOfdstLabels;

    private Map<VT, Map<Integer, Set<VT>>> ontoIndex;

    private Map<VT, Set<VT>> labelSimLabels;

    public GraphDatabase(Graph<VT, ET> graph) {
        this.graph = graph;
        this.indexOfNodeLabels = null;
        this.labelSimLabels = null;
        this.indexOfEdgeLabels = null;
        this.indexOfRelations_e = null;
        this.relationSrcDsts = null;
        this.relationDstSrcs = null;
        this.indexOfsrcLabels = null;
        this.indexOfdstLabels = null;
    }

    public GraphDatabase(Graph<VT, ET> graph, Map<VT, Map<Integer, Set<VT>>> ontoIndex) {
        this.graph = graph;
        this.indexOfNodeLabels = null;
        this.labelSimLabels = null;
        this.indexOfEdgeLabels = null;
        this.indexOfRelations_e = null;
        this.relationSrcDsts = null;
        this.relationDstSrcs = null;
        this.indexOfsrcLabels = null;
        this.indexOfdstLabels = null;
        this.ontoIndex = ontoIndex;
    }

    public static <VT, ET> GraphDatabase<VT, ET> init(Graph<VT, ET> graph) {
        return new GraphDatabase<>(graph);
    }

    // TODO : remove
    public static <VT, ET> GraphDatabase<VT, ET> buildFromGraph(Graph<VT, ET> graph) {
        return new GraphDatabase<>(graph).buildAllIndices();
    }

    // TODO : remove
    public static <VT, ET> GraphDatabase<VT, ET> buildFromGraph(Graph<VT, ET> graph, Map<VT, Map<Integer, Set<VT>>> ontoIndex) {
        return new GraphDatabase<>(graph, ontoIndex).buildAllIndices();
    }

    private GraphDatabase<VT, ET> indexNodeLabels() {
        indexOfNodeLabels = new HashMap<>();
        for (Node<VT> v : graph.nodeIter()) {
            indexOfNodeLabels.putIfAbsent(v.label(), new HashSet<>());
            indexOfNodeLabels.get(v.label()).add(v);
        }
        return this;
    }

    public GraphDatabase<VT, ET> buildSimLabelsMap(int hops) {
        labelSimLabels = new HashMap<>();
        for (VT nodeLabel : ontoIndex.keySet()) {
            labelSimLabels.put(nodeLabel, new HashSet<>());
        }
        for (VT nodeLabel : ontoIndex.keySet()) {
            for (int i = 0; i <= hops; i++) {
                labelSimLabels.get(nodeLabel).addAll(ontoIndex.get(nodeLabel).get(i));
            }
        }

        return this;
    }

    private GraphDatabase<VT, ET> indexEdgeLabels() {
        indexOfEdgeLabels = new HashMap<>();
        for (Edge<VT, ET> e : graph.edgeIter()) {
            indexOfEdgeLabels.putIfAbsent(e.label(), new HashSet<>());
            indexOfEdgeLabels.get(e.label()).add(e);
        }
        return this;
    }

    private GraphDatabase<VT, ET> indexRelationsByEdges() {
        indexOfRelations_e = new HashMap<>();
        for (Edge<VT, ET> e : graph.edgeIter()) {
            Relation<VT, ET> r = Relation.fromEdge(e);
            indexOfRelations_e.putIfAbsent(r, new HashSet<>());
            indexOfRelations_e.get(r).add(e);
        }
        return this;
    }

    private GraphDatabase<VT, ET> buildRelationNodesMap() {
        if (indexOfRelations_e == null) {
            indexRelationsByEdges();
        }

        relationSrcDsts = new HashMap<>();
        relationDstSrcs = new HashMap<>();
        for (Relation<VT, ET> r : indexOfRelations_e.keySet()) {
            relationSrcDsts.putIfAbsent(r, new HashMap<>());
            relationDstSrcs.putIfAbsent(r, new HashMap<>());
            for (Edge<VT, ET> e : indexOfRelations_e.get(r)) {
                relationSrcDsts.get(r).putIfAbsent(e.srcNode(), new HashSet<>());
                relationDstSrcs.get(r).putIfAbsent(e.dstNode(), new HashSet<>());
                relationSrcDsts.get(r).get(e.srcNode()).add(e.dstNode());
                relationDstSrcs.get(r).get(e.dstNode()).add(e.srcNode());
            }
        }

        return this;
    }

    private GraphDatabase<VT, ET> buildSrcLabelRelationsMap() {
        if (indexOfRelations_e == null) {
            indexRelationsByEdges();
        }

        indexOfsrcLabels = new HashMap<>();
        for (Relation<VT, ET> r : indexOfRelations_e.keySet()) {
            indexOfsrcLabels.putIfAbsent(r.srcLabel(), new HashSet<>());
            indexOfsrcLabels.putIfAbsent(r.dstLabel(), new HashSet<>());
            indexOfsrcLabels.get(r.srcLabel()).add(r);
        }
        return this;
    }

    private GraphDatabase<VT, ET> buildDstLabelRelationsMap() {
        if (indexOfRelations_e == null) {
            indexRelationsByEdges();
        }

        indexOfdstLabels = new HashMap<>();
        for (Relation<VT, ET> r : indexOfRelations_e.keySet()) {
            indexOfdstLabels.putIfAbsent(r.srcLabel(), new HashSet<>());
            indexOfdstLabels.putIfAbsent(r.dstLabel(), new HashSet<>());
            indexOfdstLabels.get(r.dstLabel()).add(r);
        }
        return this;
    }

    private GraphDatabase<VT, ET> buildAllIndices() {
        return this.indexNodeLabels()
                .indexEdgeLabels()
                .indexRelationsByEdges()
                .buildRelationNodesMap()
                .buildSrcLabelRelationsMap()
                .buildDstLabelRelationsMap();
    }

    public Graph<VT, ET> getGraph() {
        return graph;
    }

    public Set<VT> nodeLabels() {
        return indexOfNodeLabels.keySet();
    }

    public Set<Node<VT>> getNodes(VT label) {
        if (!indexOfNodeLabels.containsKey(label)) {
            return new HashSet<>();
        } else {
            return indexOfNodeLabels.get(label);
        }
    }

    public Set<VT> getSimLabels(VT label) {
        if (!labelSimLabels.containsKey(label)) {
            return new HashSet<>();
        } else {
            return labelSimLabels.get(label);
        }
    }

    public Set<ET> edgeLabels() {
        return indexOfEdgeLabels.keySet();
    }

    public Set<Relation<VT, ET>> relationSet() {
        return indexOfRelations_e.keySet();
    }

    public Set<Edge<VT, ET>> getEdges(ET edgeLabel) {
        if (!indexOfEdgeLabels.containsKey(edgeLabel)) {
            return new HashSet<>();
        } else {
            return indexOfEdgeLabels.get(edgeLabel);
        }
    }

    public Set<Edge<VT, ET>> getEdges(Relation<VT, ET> r) {
        if (!indexOfRelations_e.containsKey(r)) {
            return new HashSet<>();
        } else {
            return indexOfRelations_e.get(r);
        }
    }

    public Set<Node<VT>> getSrcNodes(Relation<VT, ET> r) {
        if (!relationSrcDsts.containsKey(r)) {
            return new HashSet<>();
        } else {
            return relationSrcDsts.get(r).keySet();
        }
    }

    public Set<Node<VT>> getDstNodes(Relation<VT, ET> r) {
        if (!relationDstSrcs.containsKey(r)) {
            return new HashSet<>();
        } else {
            return relationDstSrcs.get(r).keySet();
        }
    }

    public Set<Node<VT>> outNeighbors(Node<VT> v, Relation<VT, ET> r) {
        if (!relationSrcDsts.get(r).containsKey(v)) {
            return new HashSet<>();
        } else {
            return relationSrcDsts.get(r).get(v);
        }
    }

    public Set<Node<VT>> inNeighbors(Node<VT> v, Relation<VT, ET> r) {
        if (!relationDstSrcs.get(r).containsKey(v)) {
            return new HashSet<>();
        } else {
            return relationDstSrcs.get(r).get(v);
        }
    }

    public Set<Relation<VT, ET>> getOutRelations(VT srcLabel) {
        if (!indexOfsrcLabels.containsKey(srcLabel)) {
            return new HashSet<>();
        } else {
            return indexOfsrcLabels.get(srcLabel);
        }
    }

    public Set<Relation<VT, ET>> getInRelations(VT dstLabel) {
        if (!indexOfdstLabels.containsKey(dstLabel)) {
            return new HashSet<>();
        } else {
            return indexOfdstLabels.get(dstLabel);
        }
    }

    public Map<VT, Map<Integer, Set<VT>>> getOntoIndex() {
        return ontoIndex;
    }

    public String toSizeString() {
        return "# |V| = " + graph.numOfNodes() + "\n" +
                "# |E| = " + graph.numOfEdges() + "\n" +
                "# |L| = " + nodeLabels().size() + "\n" +
                "# |R| = " + edgeLabels().size() + "\n" +
                "# |T| = " + relationSet().size();
    }

}