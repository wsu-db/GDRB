package edu.wsu.eecs.gfc.core;

import java.util.*;

/**
 * DAG and topological sort for graphs.
 * <p>
 * Peng Lin penglin03@gmail.com
 */
public class DirectedAcyclicGraph<VT, ET> {

    private Graph<VT, ET> g;

    private List<Node<VT>> topologicalOrder;

    private Map<Node<VT>, Integer> depthMap;

    private Map<Node<VT>, Integer> heightMap;

    private int height;

    private DirectedAcyclicGraph(Graph<VT, ET> g,
                                 List<Node<VT>> topologicalOrder,
                                 Map<Node<VT>, Integer> depthMap,
                                 Map<Node<VT>, Integer> heightMap,
                                 int height) {
        this.g = g;
        this.topologicalOrder = topologicalOrder;
        this.depthMap = depthMap;
        this.heightMap = heightMap;
        this.height = height;
    }

    private DirectedAcyclicGraph(Graph<VT, ET> g) {
        this.g = g;
        this.topologicalOrder = topologicalSort(g);
        this.depthMap = null;
        this.heightMap = null;
        this.height = -1;
    }

    private DirectedAcyclicGraph<VT, ET> createDepthMap() {
        int maxDepth = -1;
        Map<Node<VT>, Integer> depthMap = new HashMap<>();
        for (int i = topologicalOrder.size() - 1; i >= 0; i--) {
            Node<VT> child = topologicalOrder.get(i);
            if (g.outDegree(child) == 0) {
                depthMap.put(child, 0);
            } else {
                int maxParentDepth = -1;
                for (Node<VT> parent : g.nodesFrom(child)) {
                    if (maxParentDepth < depthMap.get(parent)) {
                        maxParentDepth = depthMap.get(parent);
                    }
                }
                depthMap.put(child, maxParentDepth + 1);
            }
            if (maxDepth < depthMap.get(child)) {
                maxDepth = depthMap.get(child);
            }
        }
        this.height = maxDepth + 1;
        this.depthMap = depthMap;
        return this;
    }

    private DirectedAcyclicGraph<VT, ET> createHeightMap() {
        int maxHeight = -1;
        Map<Node<VT>, Integer> heightMap = new HashMap<>();
        for (int i = 0; i < topologicalOrder.size(); i++) {
            Node<VT> parent = topologicalOrder.get(i);
            if (g.inDegree(parent) == 0) {
                heightMap.put(parent, 0);
            } else {
                int maxChildrenHeight = -1;
                for (Node<VT> child : g.nodesTo(parent)) {
                    if (maxChildrenHeight < heightMap.get(child)) {
                        maxChildrenHeight = heightMap.get(child);
                    }
                }
                heightMap.put(parent, maxChildrenHeight + 1);
            }
            if (maxHeight < heightMap.get(parent)) {
                maxHeight = heightMap.get(parent);
            }
        }
        this.height = maxHeight + 1;
        this.heightMap = heightMap;
        return this;
    }

    public static <VT, ET> DirectedAcyclicGraph<VT, ET> createFromGraph(Graph<VT, ET> g, boolean bDepth, boolean bHeight) {
        DirectedAcyclicGraph<VT, ET> dag = new DirectedAcyclicGraph<>(g);
        if (!dag.isDAG()) {
            throw new RuntimeException("The graph is not a directed acyclic graph (DAG).");
        }

        if (bDepth) {
            dag.createDepthMap();
        }

        if (bHeight) {
            dag.createHeightMap();
        }

        return dag;
    }

    public boolean isDAG() {
        return topologicalOrder.size() == g.numOfNodes();
    }

    public List<Node<VT>> getTopologicalOrder() {
        return topologicalOrder;
    }

    public Graph<VT, ET> getGraph() {
        return g;
    }

    public int getHeight() {
        return height;
    }

    public int getNodeDepth(Node<VT> v) {
        return depthMap.get(v);
    }

    public int getNodeDepth(Object id) {
        return depthMap.get(g.getNode(id));
    }

    public int getNodeHeight(Node<VT> v) {
        return heightMap.get(v);
    }

    public int getNodeHeight(Object id) {
        return heightMap.get(g.getNode(id));
    }

    /**
     * Topological sort.
     *
     * @param g    the graph to sort
     * @param <VT> the node type
     * @param <ET> the edge type
     * @return null if the graph is not a DirectedAcyclicGraph; otherwise, return the sorted list of nodes.
     */
    public static <VT, ET> List<Node<VT>> topologicalSort(Graph<VT, ET> g) {
        List<Node<VT>> order = new ArrayList<>();
        Deque<Node<VT>> queue = new ArrayDeque<>();
        Graph<VT, ET> gCopy = g.shallowCopy();
        for (Node<VT> n : gCopy.nodeIter()) {
            if (gCopy.inDegree(n) == 0) {
                queue.add(n);
            }
        }
        while (!queue.isEmpty()) {
            Node<VT> n = queue.removeFirst();
            order.add(n);
            gCopy.removeNode(n);
            for (Node<VT> m : g.nodesFrom(n)) {
                if (gCopy.inDegree(m) == 0) {
                    queue.add(m);
                }
            }
        }
        return gCopy.numOfEdges() > 0 ? new ArrayList<>() : order;
    }
}
