package edu.wsu.eecs.gfc.core;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Graph - represent simple directed labeled graphs.
 * - Every node can have at most one self loop.
 * - Every pair of nodes (u, v) can have at most one edge from u to v, and at most one edge from v to u.
 * - Otherwise, there are no parallel edges.
 * - For undirected graphs, the graph can be constructed by randomly assigning the direction of edges.
 * To enumerate edges from a node, every time enumerate edges both from and to the node.
 * <p>
 * Note:
 * - Node IDs should be immutable, since they are used as keys of underlying hash maps of the graph.
 *
 * @author Peng lin penglin03@gmail.com
 */
public class Graph<VT, ET> {

    private int numOfNodes;

    private int numOfEdges;

    private Iterable<Node<VT>> nodeIter;

    private Iterable<Edge<VT, ET>> edgeIter;

    private Map<Object, Node<VT>> nodeIndex;

    private Map<Node<VT>, Map<Node<VT>, Edge<VT, ET>>> edgeIndex_o;

    private Map<Node<VT>, Map<Node<VT>, Edge<VT, ET>>> edgeIndex_i;

    private Graph() {
        numOfNodes = 0;
        numOfEdges = 0;
        nodeIndex = new HashMap<>();
        edgeIndex_o = new HashMap<>();
        edgeIndex_i = new HashMap<>();
        nodeIter = () -> nodeIndex.values().stream().iterator();
        edgeIter = () -> edgeIndex_o.values().stream().flatMap(e -> e.values().stream()).iterator();
    }

    @NotNull
    public static <VT, ET> Graph<VT, ET> createEmptyGraph() {
        return new Graph<>();
    }

    public int numOfNodes() {
        return numOfNodes;
    }

    public int numOfEdges() {
        return numOfEdges;
    }

    public Iterable<Node<VT>> nodeIter() {
        return nodeIter;
    }

    public Iterable<Edge<VT, ET>> edgeIter() {
        return edgeIter;
    }

    public Collection<Node<VT>> getNodeCollection() {
        return nodeIndex.values();
    }

    public boolean hasNodeId(Object id) {
        return nodeIndex.containsKey(id);
    }

    public Node<VT> getNode(Object id) {
        return nodeIndex.get(id);
    }

    public boolean hasNode(Node<VT> v) {
        return v == nodeIndex.get(v.id());
    }

    public int outDegree(Node<VT> v) {
        if (!hasNode(v)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(v).size();
    }

    public int outDegree(Object id) {
        if (!hasNodeId(id)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(getNode(id)).size();
    }

    public int inDegree(Node<VT> v) {
        if (!hasNode(v)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_i.get(v).size();
    }

    public int inDegree(Object id) {
        if (!hasNodeId(id)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_i.get(getNode(id)).size();
    }

    public int degree(Node<VT> v) {
        if (!hasNode(v)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(v).size() + edgeIndex_i.get(v).size();
    }

    public int degree(Object id) {
        if (!hasNodeId(id)) {
            throw new NoSuchElementException();
        }
        Node<VT> v = getNode(id);
        return edgeIndex_o.get(v).size() + edgeIndex_i.get(v).size();
    }

    public Set<Node<VT>> nodesFrom(Node<VT> v) {
        if (!hasNode(v)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(v).keySet();
    }

    public Set<Node<VT>> nodesFrom(Object id) {
        if (!hasNodeId(id)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(getNode(id)).keySet();
    }

    public Set<Node<VT>> nodesTo(Node<VT> v) {
        if (!hasNode(v)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_i.get(v).keySet();
    }

    public Set<Node<VT>> nodesTo(Object id) {
        if (!hasNodeId(id)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_i.get(getNode(id)).keySet();
    }

    public Collection<Edge<VT, ET>> edgesFrom(Node<VT> v) {
        if (!hasNode(v)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(v).values();
    }

    public Collection<Edge<VT, ET>> edgesFrom(Object id) {
        if (!hasNodeId(id)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(getNode(id)).values();
    }

    public Collection<Edge<VT, ET>> edgesTo(Node<VT> v) {
        if (!hasNode(v)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_i.get(v).values();
    }

    public Collection<Edge<VT, ET>> edgesTo(Object id) {
        if (!hasNodeId(id)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_i.get(getNode(id)).values();
    }

    public Node<VT> createNode(Object id, VT label) {
        if (hasNodeId(id)) {
            return null;
        }
        Node<VT> v = Node.createLabeledNode(id, label);
        nodeIndex.put(id, v);
        edgeIndex_o.put(v, new HashMap<>());
        edgeIndex_i.put(v, new HashMap<>());
        numOfNodes++;
        return v;
    }

    public boolean addNode(Node<VT> v) {
        if (hasNodeId(v.id())) {
            return false;
        }
        nodeIndex.put(v.id(), v);
        edgeIndex_o.put(v, new HashMap<>());
        edgeIndex_i.put(v, new HashMap<>());
        numOfNodes++;
        return true;
    }

    public boolean removeNode(Node<VT> v) {
        if (!hasNode(v)) {
            return false;
        }
        int degree = degree(v);
        for (Node<VT> w : nodesFrom(v)) {
            edgeIndex_i.get(w).remove(v);
        }
        for (Node<VT> w : nodesTo(v)) {
            edgeIndex_o.get(w).remove(v);
        }
        edgeIndex_o.remove(v);
        edgeIndex_i.remove(v);
        nodeIndex.remove(v.id());
        numOfNodes--;
        numOfEdges -= degree;
        return true;
    }

    public boolean removeNode(Object id) {
        if (!hasNodeId(id)) {
            return false;
        }
        Node<VT> v = getNode(id);
        int degree = degree(v);
        for (Node<VT> w : nodesFrom(v)) {
            edgeIndex_i.get(w).remove(v);
        }
        for (Node<VT> w : nodesTo(v)) {
            edgeIndex_o.get(w).remove(v);
        }
        edgeIndex_o.remove(v);
        edgeIndex_i.remove(v);
        nodeIndex.remove(v.id());
        numOfNodes--;
        numOfEdges -= degree;
        return true;
    }

    public Edge<VT, ET> getEdge(Node<VT> src, Node<VT> dst) {
        if (!hasNode(src)) {
            throw new NoSuchElementException();
        }
        if (!hasNode(dst)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(src).get(dst);
    }

    public Edge<VT, ET> getEdge(Object srcId, Object dstId) {
        if (!hasNodeId(srcId)) {
            throw new NoSuchElementException();
        }
        if (!hasNodeId(dstId)) {
            throw new NoSuchElementException();
        }
        return edgeIndex_o.get(getNode(srcId)).get(getNode(dstId));
    }

    public boolean hasEdge(Node<VT> src, Node<VT> dst) {
        if (!hasNode(src) || !hasNode(dst)) {
            return false;
        }
        return edgeIndex_o.get(src).containsKey(dst);
    }

    public boolean hasEdge(Object srcId, Object dstId) {
        if (!hasNodeId(srcId) || !hasNodeId(dstId)) {
            return false;
        }
        return edgeIndex_o.get(getNode(srcId)).containsKey(getNode(dstId));
    }

    public boolean hasEdge(Edge<VT, ET> e) {
        if (e == null) {
            return false;
        }
        if (!hasNode(e.srcNode())) {
            return false;
        }
        if (!hasNode(e.dstNode())) {
            return false;
        }
        return e == edgeIndex_o.get(e.srcNode()).get(e.dstNode());
    }

    public Edge<VT, ET> createEdge(Object srcId, Object dstId, ET label) {
        if (!hasNodeId(srcId)) {
            return null;
        }
        if (!hasNodeId(dstId)) {
            return null;
        }
        if (edgeIndex_o.get(getNode(srcId)).get(getNode(dstId)) != null) {
            return null;
        }
        Node<VT> src = getNode(srcId);
        Node<VT> dst = getNode(dstId);
        Edge<VT, ET> e = Edge.createLabeledEdge(src, dst, label);
        edgeIndex_o.get(src).put(dst, e);
        edgeIndex_i.get(dst).put(src, e);
        numOfEdges++;
        return e;
    }

    public boolean addEdge(Edge<VT, ET> e) {
        if (e == null) {
            throw new NullPointerException();
        }
        if (!hasNode(e.srcNode())) {
            return false;
        }
        if (!hasNode(e.dstNode())) {
            return false;
        }
        if (edgeIndex_o.get(e.srcNode()).get(e.dstNode()) != null) {
            return false;
        }
        edgeIndex_o.get(e.srcNode()).put(e.dstNode(), e);
        edgeIndex_i.get(e.dstNode()).put(e.srcNode(), e);
        numOfEdges++;
        return true;
    }

    public Edge<VT, ET> removeEdge(Node<VT> src, Node<VT> dst) {
        if (!hasEdge(src, dst)) {
            return null;
        }
        Edge<VT, ET> e = getEdge(src, dst);
        edgeIndex_o.get(src).remove(dst);
        edgeIndex_i.get(dst).remove(src);
        numOfEdges--;
        return e;
    }

    public Edge<VT, ET> removeEdge(Object srcId, Object dstId) {
        if (!hasNodeId(srcId)) {
            throw new NoSuchElementException();
        }
        if (!hasNodeId(dstId)) {
            throw new NoSuchElementException();
        }
        Edge<VT, ET> e = edgeIndex_o.get(getNode(srcId)).get(getNode(dstId));
        edgeIndex_o.get(e.srcNode()).remove(e.dstNode());
        edgeIndex_i.get(e.dstNode()).remove(e.srcNode());
        numOfEdges--;
        return e;
    }

    public boolean removeEdge(Edge<VT, ET> e) {
        if (!hasEdge(e)) {
            return false;
        }
        edgeIndex_o.get(e.srcNode()).remove(e.dstNode());
        edgeIndex_i.get(e.dstNode()).remove(e.srcNode());
        numOfEdges--;
        return true;
    }

    /**
     * Shallow copy this graph to a new graph. Note that there is no copy of each node or edge object, and it is
     * only to re-index the nodes and edges in the new graph.
     *
     * @return a new graph that has the nodes and edges in this graph.
     */
    public Graph<VT, ET> shallowCopy() {
        Graph<VT, ET> g = Graph.createEmptyGraph();
        for (Node<VT> v : nodeIter) {
            g.addNode(v);
        }
        for (Edge<VT, ET> e : edgeIter) {
            g.addEdge(e);
        }
        return g;
    }

    public void clear() {
        this.numOfNodes = 0;
        this.numOfEdges = 0;
        this.nodeIndex.clear();
        this.edgeIndex_o.clear();
        this.edgeIndex_i.clear();
    }

    public boolean isEmpty() {
        return numOfNodes == 0;
    }

    public String toGraphString() {
        StringBuilder sb = new StringBuilder();
        sb.append(toSizeString()).append("\n");
        sb.append("# Nodes:\n");
        for (Node<VT> node : nodeIter) {
            sb.append(node).append("\n");
        }
        sb.append("# Edges:\n");
        for (Edge<VT, ET> e : edgeIter) {
            sb.append(e.srcId()).append("\t").append(e.dstId()).append("\t").append(e.label()).append("\n");
        }
        return sb.toString();
    }

    public String toSizeString() {
        return "# |V| = " + numOfNodes + ", |E| = " + numOfEdges;
    }

    public Set<Node<VT>> oneHopNeighbors(Node<VT> center) {
        Set<Node<VT>> nbor = new HashSet<>(nodesFrom(center));
        nbor.addAll(nodesTo(center));
        return nbor;
    }

    public Map<Integer, Set<Node<VT>>> multipleHopNeighbors(Node<VT> center, int radius) {
        Map<Integer, Set<Node<VT>>> nborMap = new HashMap<>();
        for (int i = 0; i <= radius; i++) {
            nborMap.put(i, new HashSet<>());
        }
        nborMap.get(0).add(center);

        Deque<Node<VT>> queue = new ArrayDeque<>();
        Set<Node<VT>> visitedNodes = new HashSet<>();
        queue.addLast(center);
        visitedNodes.add(center);
        for (int i = 1; i <= radius; i++) {
            if (queue.isEmpty()) {
                break;
            }
            int queueSize = queue.size();
            for (int j = 0; j < queueSize; j++) {
                Node<VT> v = queue.removeFirst();
                for (Node<VT> w : oneHopNeighbors(v)) {
                    if (!visitedNodes.contains(w)) {
                        visitedNodes.add(w);
                        queue.addLast(w);
                        nborMap.get(i).add(w);
                    }
                }
            }
        }
        return nborMap;
    }

    // TODO: to delete.
    public Map<Integer, Set<Node<VT>>> nodesFrom(Node<VT> center, int radius) {
        if (radius <= 0) {
            throw new RuntimeException("d should be at least 1.");
        }
        Map<Integer, Set<Node<VT>>> nodeMap = new HashMap<>();
        for (int i = 1; i <= radius; i++) {
            nodeMap.put(i, new HashSet<>());
        }
        Deque<Node<VT>> queue = new ArrayDeque<>();
        Set<Node<VT>> visitedNodes = new HashSet<>();
        queue.addLast(center);
        visitedNodes.add(center);
        for (int i = 1; i <= radius; i++) {
            if (queue.isEmpty()) {
                break;
            }
            int toDequeue = queue.size();
            for (int j = 0; j < toDequeue; j++) {
                Node<VT> n = queue.removeFirst();
                for (Node<VT> m : nodesFrom(n)) {
                    if (!visitedNodes.contains(m)) {
                        visitedNodes.add(m);
                        queue.addLast(m);
                        nodeMap.get(i).add(m);
                    }
                }
            }
        }
        return nodeMap;
    }

    // TODO: to delete.
    public Map<Integer, Set<Node<VT>>> nodesFrom(Object centerId, int radius) {
        return nodesFrom(getNode(centerId), radius);
    }

    // TODO: to delete.
    public Map<Integer, Set<Node<VT>>> nodesTo(Node<VT> center, int radius) {
        if (radius <= 0) {
            throw new RuntimeException("d should be at least 1.");
        }
        Map<Integer, Set<Node<VT>>> neighborMap = new HashMap<>();
        for (int i = 1; i <= radius; i++) {
            neighborMap.put(i, new HashSet<>());
        }
        Deque<Node<VT>> queue = new ArrayDeque<>();
        Set<Node<VT>> visitedNodes = new HashSet<>();
        queue.addLast(center);
        visitedNodes.add(center);
        for (int i = 1; i <= radius; i++) {
            if (queue.isEmpty()) {
                break;
            }
            int toDequeue = queue.size();
            for (int j = 0; j < toDequeue; j++) {
                Node<VT> n = queue.removeFirst();
                for (Node<VT> m : nodesTo(n)) {
                    if (!visitedNodes.contains(m)) {
                        visitedNodes.add(m);
                        queue.addLast(m);
                        neighborMap.get(i).add(m);
                    }
                }
            }
        }
        return neighborMap;
    }

    // TODO: to delete.
    public Map<Integer, Set<Node<VT>>> nodesTo(Object centerId, int radius) {
        return nodesTo(getNode(centerId), radius);
    }
}
