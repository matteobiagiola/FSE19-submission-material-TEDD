package org.mb.tedd.algorithm.refinement.edgeselection;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.graph.DependencyGraphManager;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;

import java.util.Optional;

public abstract class RefinementStrategy<T> {

    private final static Logger logger = Logger.getLogger(RefinementStrategy.class.getName());

    /**
     * Check if the provided graph contains a cycle when edge is inverted. The
     * input graph is duplicated - might take some time/memory - and the
     * provided edge must belong to the input graph.
     *
     */
    protected boolean introduceCycle(Graph<GraphNode<T>, GraphEdge> dependencyGraph, //
                                     GraphNode<T> graphSourceNode, GraphNode<T> graphTargetNode) {

        if(dependencyGraph.getEdge(graphSourceNode, graphTargetNode) == null){
            throw new IllegalArgumentException("Graph does not contain an edge between "
                    + graphSourceNode + " and " + graphTargetNode);
        }

        Graph<GraphNode<T>, GraphEdge> copy = new DependencyGraphManager<>(dependencyGraph).duplicate();

        GraphNode<T> src = null;
        GraphNode<T> tgt = null;
        for (GraphNode<T> graphNode : copy.vertexSet()) {
            if (graphNode.getTestCase().equals(graphSourceNode.getTestCase())) {
                src = graphNode;
                continue;
            }
            if (graphNode.getTestCase().equals(graphTargetNode.getTestCase())) {
                tgt = graphNode;
                continue;
            }
            if (src != null && tgt != null) {
                break;
            }
        }

        GraphEdge eCopy = copy.getEdge(src, tgt);
        copy.removeEdge(eCopy);

        // A cycle will result in an exception
        try {
            copy.addEdge(tgt, src, new GraphEdge(eCopy.getDependentValues(), tgt, src));
            return false;
        } catch (IllegalArgumentException ex) {
            logger.debug("Inverting edge " + dependencyGraph.getEdge(graphSourceNode, graphTargetNode)
                    + " would result in a cycle");
            return true;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * This must ensure that the returned edge will not create a cycle when
     * inverted !
     *
     * Returns either the selected edge or null when no more edges can be
     * selected
     *
     * @param dependencyGraph
     * @return
     */
    public abstract Optional<GraphEdge> selectEdge(Graph<GraphNode<T>, GraphEdge> dependencyGraph);
}
