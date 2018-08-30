package edu.wsu.eecs.gfc.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility functions
 * @author Peng Lin penglin03@gmail.com
 */
public class Utility {

    public static <K, V> Map<V, K> getInverseMap(Map<K, V> map) {
        if (map == null) {
            return null;
        }
        Map<V, K> invMap = new HashMap<>();
        for (K key : map.keySet()) {
            V value = map.get(key);
            if (!invMap.containsKey(value)) {
                invMap.put(value, key);
            } else {
                throw new RuntimeException("Values are not unique");
            }
        }
        return invMap;
    }

    public static <VT, ET> Map<VT, Map<Integer, Set<VT>>> indexOntology(DirectedAcyclicGraph<VT, ET> dag, int hops) {
        Map<VT, Map<Integer, Set<VT>>> ontoIndex = new HashMap<>();
        for (Node<VT> node : dag.getGraph().nodeIter()) {
            ontoIndex.put(node.label(), new HashMap<>());
            ontoIndex.get(node.label()).put(0, new HashSet<>());
            ontoIndex.get(node.label()).get(0).add(node.label());
            Map<Integer, Set<Node<VT>>> outNeighborMap = dag.getGraph().nodesFrom(node, hops);
            Map<Integer, Set<Node<VT>>> inNeighborMap = dag.getGraph().nodesTo(node, hops);
            for (int i = 1; i <= hops; i++) {
                ontoIndex.get(node.label()).putIfAbsent(i, new HashSet<>());
                for (Node<VT> m : outNeighborMap.get(i)) {
                    ontoIndex.get(node.label()).get(i).add(m.label());
                }
                for (Node<VT> m : inNeighborMap.get(i)) {
                    ontoIndex.get(node.label()).get(i).add(m.label());
                }
            }
        }

        return ontoIndex;
    }
}
