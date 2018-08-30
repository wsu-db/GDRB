package edu.wsu.eecs.gfc.core;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * An OGFC (Ontological Graph Fact Checking) rule is in the form of P(x, y) => r(x, y),
 * which is a graph operator that matches the graph query P with the data graph G incrementally.
 * As it updates the match set incrementally, there is one special edge exy to start with,
 * and x and y are node labels of exy.
 * A match set of a rule is initialized with a graph database G,
 * a relation r, and a set of edges that are initially covered by the pattern P.
 * For more details, see our paper:
 * Discovering Graph Patterns for Fact Checking in Knowledge Graphs (DASFAA 2018)
 * <p>
 * @author Peng Lin penglin03@gmail.com
 */
public class OGFCRule<VT, ET> {

    private Graph<VT, ET> P;

    private GraphDatabase<VT, ET> G;

    private Map<Node<VT>, Set<Node<VT>>> matchSet;

    private Edge<VT, ET> exy;

    public double supp = -1;

    public double conf = -1;

    public double gTest = -1;

    public double pCov = -1;

    private OGFCRule(Graph<VT, ET> P, GraphDatabase<VT, ET> G, Map<Node<VT>, Set<Node<VT>>> matchSet, Edge<VT, ET> exy) {
        this.P = P;
        this.G = G;
        this.matchSet = matchSet;
        this.exy = exy;
    }

    public Graph<VT, ET> P() {
        return P;
    }

    public GraphDatabase<VT, ET> G() {
        return G;
    }

    public Map<Node<VT>, Set<Node<VT>>> matchSet() {
        return matchSet;
    }

    public Node<VT> x() {
        return exy.srcNode();
    }

    public Node<VT> y() {
        return exy.dstNode();
    }

    public Edge<VT, ET> exy() {
        return exy;
    }

    private void removeUnmatchedPairs() {
        boolean isChanged = true;
        while (isChanged) {
            isChanged = false;
            for (Node<VT> u : matchSet.keySet()) {
                for (Edge<VT, ET> eu : P.edgesFrom(u)) {
                    Set<Node<VT>> removeSet = new HashSet<>();
                    for (Node<VT> v : matchSet.get(u)) {
                        if (Collections.disjoint(matchSet.get(eu.dstNode()), G.getGraph().nodesFrom(v))) {
                            isChanged = true;
                            removeSet.add(v);
                        }
                    }
                    matchSet.get(u).removeAll(removeSet);
                    if (matchSet.get(u).isEmpty()) {
                        for (Set<Node<VT>> mSet : matchSet.values()) {
                            mSet.clear();
                        }
                        return;
                    }
                }

                for (Edge<VT, ET> euu : P.edgesTo(u)) {
                    Set<Node<VT>> removeSet = new HashSet<>();
                    for (Node<VT> v : matchSet.get(u)) {
                        if (Collections.disjoint(matchSet.get(euu.srcNode()), G.getGraph().nodesTo(v))) {
                            isChanged = true;
                            removeSet.add(v);
                        }
                    }
                    matchSet.get(u).removeAll(removeSet);
                    if (matchSet.get(u).isEmpty()) {
                        for (Set<Node<VT>> mSet : matchSet.values()) {
                            mSet.clear();
                        }
                        return;
                    }
                }
            }
        }
    }

    public static <VT, ET> boolean simLabel(VT l1, VT l2, GraphDatabase<VT, ET> bigGraph) {
        return bigGraph.getSimLabels(l1).contains(l2);
    }

    public static <VT, ET> boolean simRelation(Relation<VT, ET> r1, Relation<VT, ET> r2, GraphDatabase<VT, ET> bigGraph) {
        return bigGraph.getSimLabels(r1.srcLabel()).contains(r2.srcLabel())
                && bigGraph.getSimLabels(r1.dstLabel()).contains(r2.dstLabel());
    }

    /**
     * Initialize an OGFC rule.
     *
     * @param G                is the big data graph.
     * @param rxy              is the relation to OGFC_stream.
     * @param positiveExamples is the edges that r(x, y) initially covers.
     * @return an OGFC rule.
     */
    @NotNull
    public static <VT, ET> OGFCRule<VT, ET> createInit(GraphDatabase<VT, ET> G, Relation<VT, ET> rxy, List<Edge<VT, ET>> positiveExamples) {
        Node<VT> x = Node.createLabeledNode(0, rxy.srcLabel());
        Node<VT> y = Node.createLabeledNode(1, rxy.dstLabel());
        Edge<VT, ET> exy = Edge.createLabeledEdge(x, y, rxy.edgeLabel());
        Graph<VT, ET> P = Graph.createEmptyGraph();
        P.addNode(x);
        P.addNode(y);
        Map<Node<VT>, Set<Node<VT>>> matchSet = new HashMap<>();
        matchSet.put(x, new HashSet<>());
        matchSet.put(y, new HashSet<>());
        for (Edge<VT, ET> e : positiveExamples) {
            matchSet.get(x).add(e.srcNode());
            matchSet.get(y).add(e.dstNode());
        }
        return new OGFCRule<>(P, G, matchSet, exy);
    }

    /**
     * First shallow copy the OGFCRule and then extend it by a new pattern edge f.
     *
     * @param phi is the original OGFC rule to extend.
     * @param f   is the "frontier" edge to extend the rule P.
     * @return a new rule that extends the original edge by a new edge.
     */
    @NotNull
    public static <VT, ET> OGFCRule<VT, ET> extendEdge(OGFCRule<VT, ET> phi, Edge<VT, ET> f) {
        if ((!phi.P.hasNodeId(f.srcId())) && (!phi.P.hasNodeId(f.dstId()))) {
            throw new RuntimeException("Invalid frontier edge.");
        }
        Relation<VT, ET> r = Relation.fromEdge(f);
        Graph<VT, ET> Q = phi.P.shallowCopy();
        int id = phi.P.numOfNodes();
        Map<Node<VT>, Set<Node<VT>>> matchSet = new HashMap<>();
        for (Node<VT> u : phi.matchSet.keySet()) {
            matchSet.put(u, new HashSet<>(phi.matchSet.get(u)));
        }
        if (f.srcId() == null && f.dstId() == null) {
            throw new RuntimeException("Invalid frontier edge.");
        } else if (f.srcId() == null) {
            Node<VT> u = Q.getNode(f.dstId());
            Node<VT> v = Node.createLabeledNode(id, f.srcLabel());
            matchSet.put(v, new HashSet<>());
            for (VT lv : phi.G.getSimLabels(v.label())) {
                matchSet.get(v).addAll(phi.G.getNodes(lv));
            }
            for (Node<VT> w : phi.P.nodeIter()) {
                if (simLabel(w.label(), v.label(), phi.G)) {
                    matchSet.get(v).removeAll(phi.matchSet.get(w));
                }
            }
            Q.addNode(v);
            Edge<VT, ET> e = Edge.createLabeledEdge(v, u, f.label());
            Q.addEdge(e);
        } else if (f.dstId() == null) {
            Node<VT> u = Q.getNode(f.srcId());
            Node<VT> v = Node.createLabeledNode(id, f.dstLabel());
            matchSet.put(v, new HashSet<>());
            for (VT lv : phi.G.getSimLabels(v.label())) {
                matchSet.get(v).addAll(phi.G.getNodes(lv));
            }
            for (Node<VT> w : phi.P.nodeIter()) {
                if (simLabel(w.label(), v.label(), phi.G)) {
                    matchSet.get(v).removeAll(phi.matchSet.get(w));
                }
            }
            Q.addNode(v);
            Edge<VT, ET> e = Edge.createLabeledEdge(u, v, f.label());
            Q.addEdge(e);
        } else {
            Q.createEdge(f.srcId(), f.dstId(), f.label());
            Node<VT> s = Q.getNode(f.srcId());
            Node<VT> t = Q.getNode(f.dstId());
            Set<Node<VT>> sSet = new HashSet<>();
            Set<Node<VT>> tSet = new HashSet<>();
            for (Node<VT> v : matchSet.get(s)) {
                for (Node<VT> w : matchSet.get(t)) {
                    Edge<VT, ET> e = phi.G.getGraph().getEdge(v, w);
                    if (e != null) {
                        sSet.add(e.srcNode());
                        tSet.add(e.dstNode());
                    }
                }
            }
            matchSet.get(s).retainAll(sSet);
            matchSet.get(t).retainAll(tSet);
        }

        OGFCRule<VT, ET> phi2 = new OGFCRule<>(Q, phi.G, matchSet, phi.exy);
        phi2.removeUnmatchedPairs();
        return phi2;
    }

    public Set<Edge<VT, ET>> searchExtensionEdges() {
        Set<Edge<VT, ET>> fs = new HashSet<>();
        Relation<VT, ET> rxy = Relation.fromEdge(exy);

        // Generate out-pattern frontiers.
        for (Node<VT> u : matchSet.keySet()) {
            for (VT uSim : G.getSimLabels(u.label())) {
                for (Relation<VT, ET> r : G.getOutRelations(uSim)) {
                    if (!simRelation(r, rxy, G) && !Collections.disjoint(G.getSrcNodes(r), matchSet.get(u))) {
                        Edge<VT, ET> f = Edge.createLabeledEdge(Node.createLabeledNode(u.id(), r.srcLabel()),
                                Node.createLabeledNode(null, r.dstLabel()), r.edgeLabel());
                        fs.add(f);
                    }
                }
                for (Relation<VT, ET> r : G.getInRelations(uSim)) {
                    if (!simRelation(r, rxy, G) && !Collections.disjoint(G.getDstNodes(r), matchSet.get(u))) {
                        Edge<VT, ET> f = Edge.createLabeledEdge(Node.createLabeledNode(null, r.srcLabel()),
                                Node.createLabeledNode(u.id(), r.dstLabel()), r.edgeLabel());
                        fs.add(f);
                    }
                }
            }
        }

        // Generate in-pattern frontiers.
        for (Node<VT> u : matchSet.keySet()) {
            for (Node<VT> uu : matchSet.keySet()) {
                if (uu != u && !P.hasEdge(u, uu) && !(u == x() && uu == y())) {
                    Set<Relation<VT, ET>> relationSet = new HashSet<>();
                    for (Node<VT> v : matchSet.get(u)) {
                        for (Node<VT> vv : matchSet.get(uu)) {
                            if (G.getGraph().hasEdge(v, vv)) {
                                Relation<VT, ET> r = Relation.fromEdge(G.getGraph().getEdge(v, vv));
                                if (!simRelation(r, rxy, G) && !relationSet.contains(r)) {
                                    relationSet.add(r);
                                    Edge<VT, ET> f = Edge.createLabeledEdge(Node.createLabeledNode(u.id(), r.srcLabel()),
                                            Node.createLabeledNode(uu.id(), r.dstLabel()), r.edgeLabel());
                                    fs.add(f);

                                }
                            }
                        }
                    }
                }
            }
        }

        return fs;
    }
}
