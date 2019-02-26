package org.mb.tedd.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class DependencyGraphManager<T> {

    private DirectedAcyclicGraph<GraphNode<T>, GraphEdge> dependencyGraph;

    public DependencyGraphManager(Graph<GraphNode<T>, GraphEdge> dependencyGraph){
        this.dependencyGraph = (DirectedAcyclicGraph<GraphNode<T>, GraphEdge>) dependencyGraph;
    }

    public DependencyGraphManager(){
        this.dependencyGraph = new DirectedAcyclicGraph<>(GraphEdge.class);
    }

    public void addDependency(GraphNode<T> graphNodeSource, GraphNode<T> graphNodeTarget, GraphEdge graphEdge){
        this.dependencyGraph.addVertex(graphNodeSource);
        this.dependencyGraph.addVertex(graphNodeTarget);
        try {
            this.dependencyGraph.addEdge(graphNodeSource, graphNodeTarget, graphEdge);
        } catch (IllegalArgumentException ex){
            throw new RuntimeException("Cycle found exception: " + ex);
        }
    }

    public Graph<GraphNode<T>, GraphEdge> getDependencyGraph() {
        return this.dependencyGraph;
    }

    @SuppressWarnings("unchecked")
    public Graph<GraphNode<T>, GraphEdge> duplicate() {
        return (Graph<GraphNode<T>, GraphEdge>) this.dependencyGraph.clone();
    }
}
