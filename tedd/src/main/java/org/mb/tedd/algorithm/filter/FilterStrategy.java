package org.mb.tedd.algorithm.filter;

import org.jgrapht.Graph;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;

public interface FilterStrategy<T> {

    Graph<GraphNode<T>, GraphEdge> filterDependencies(Graph<GraphNode<T>, GraphEdge> dependencyGraph);
}
