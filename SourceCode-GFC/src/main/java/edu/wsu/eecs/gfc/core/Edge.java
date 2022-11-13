package edu.wsu.eecs.gfc.core;

import org.jetbrains.annotations.NotNull;

/**
 * A descriptor for graph edges.
 * <p>
 * An edge has two end nodes (srcNode and dstNode) and an edge label in type of ET.
 * The srcNode and dstNode cannot be changed and the edge label can be reassigned.
 *
 * @author Peng Lin penglin03@gmail.com
 */
public class Edge<VT, ET> {

    private final Node<VT> srcNode;

    private final Node<VT> dstNode;

    private ET label;

    private Edge(Node<VT> srcNode, Node<VT> dstNode, ET label) {
        this.srcNode = srcNode;
        this.dstNode = dstNode;
        this.label = label;
    }

    @NotNull
    public static <VT, ET> Edge<VT, ET> createLabeledEdge(Node<VT> srcNode, Node<VT> dstNode, ET label) {
        return new Edge<>(srcNode, dstNode, label);
    }

    @NotNull
    public static <VT, ET> Edge<VT, ET> createUnlabeledEdge(Node<VT> srcNode, Node<VT> dstNode) {
        return new Edge<>(srcNode, dstNode, null);
    }

    public Node<VT> srcNode() {
        return srcNode;
    }

    public Node<VT> dstNode() {
        return dstNode;
    }

    public ET label() {
        return label;
    }

    public void setLabel(ET label) {
        this.label = label;
    }

    public Object srcId() {
        return srcNode.id();
    }

    public Object dstId() {
        return dstNode.id();
    }

    public VT srcLabel() {
        return srcNode.label();
    }

    public VT dstLabel() {
        return dstNode.label();
    }

    @Override
    public String toString() {
        return srcNode + "\t" + dstNode + "\t" + label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edge<?, ?> edge = (Edge<?, ?>) o;

        if (srcNode != null ? !srcNode.equals(edge.srcNode) : edge.srcNode != null) return false;
        if (dstNode != null ? !dstNode.equals(edge.dstNode) : edge.dstNode != null) return false;
        return label != null ? label.equals(edge.label) : edge.label == null;
    }

    @Override
    public int hashCode() {
        int result = srcNode != null ? srcNode.hashCode() : 0;
        result = 31 * result + (dstNode != null ? dstNode.hashCode() : 0);
        return result;
    }
}
