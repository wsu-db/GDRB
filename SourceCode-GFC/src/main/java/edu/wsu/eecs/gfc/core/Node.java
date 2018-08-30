package edu.wsu.eecs.gfc.core;

import org.jetbrains.annotations.NotNull;

/**
 * A descriptor for graph nodes.
 * <p>
 * A node has a unique ID, which can be any name or number for the node.
 * A node carries a label, which has the type VT, specified by the user.
 * The ID cannot be changed and the label can be reassigned.
 *
 * @author Peng Lin penglin03@gmail.com
 */
public class Node<VT> {

    private final Object id;

    private VT label;

    private Node(Object id, VT label) {
        this.id = id;
        this.label = label;
    }

    @NotNull
    public static <VT> Node<VT> createLabeledNode(Object id, VT label) {
        return new Node<>(id, label);
    }

    @NotNull
    public static <VT> Node<VT> createUnlabeledNode(Object id) {
        return new Node<>(id, null);
    }

    public Object id() {
        return id;
    }

    public VT label() {
        return label;
    }

    public void setLabel(VT label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return id + "\t" + label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node<?> node = (Node<?>) o;

        if (id != null ? !id.equals(node.id) : node.id != null) return false;
        return label != null ? label.equals(node.label) : node.label == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
