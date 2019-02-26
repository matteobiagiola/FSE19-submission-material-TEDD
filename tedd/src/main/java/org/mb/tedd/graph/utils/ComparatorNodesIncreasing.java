package org.mb.tedd.graph.utils;

import org.mb.tedd.graph.GraphNode;

import java.util.Comparator;

public class ComparatorNodesIncreasing<T> implements Comparator<GraphNode<T>> {

    @Override
    public int compare(GraphNode<T> node1, GraphNode<T> node2) {
        return node1.getNumOrder() - node2.getNumOrder();
    }
}
