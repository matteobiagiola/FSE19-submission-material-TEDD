package org.mb.tedd.graph.dot.exportgraph;

import org.jgrapht.io.ComponentNameProvider;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.graph.dot.NodeId;


public class NodeNameProvider<T> implements ComponentNameProvider<GraphNode<T>> {

    @Override
    public String getName(GraphNode<T> graphNode) {
        return NodeId.getId(graphNode);
    }
}
