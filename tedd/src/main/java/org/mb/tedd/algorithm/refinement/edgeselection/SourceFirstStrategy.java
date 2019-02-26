package org.mb.tedd.algorithm.refinement.edgeselection;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.graph.utils.ComparatorNodesDecreasing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class SourceFirstStrategy<T> extends RefinementStrategy<T> {

    private Iterator<GraphEdge> dependenciesIterator;
    private final static Logger logger = Logger.getLogger(SourceFirstStrategy.class.getName());

    public SourceFirstStrategy(){
        this.dependenciesIterator = null;
    }

    /**
     * Build an iterator over the dependencies at first use and then use that
     * dependencyIterator after that. This works since the approach is deterministic.
     */
    @Override
    public Optional<GraphEdge> selectEdge(final Graph<GraphNode<T>, GraphEdge> dependencyGraph) {

        this.dependenciesIterator = this.buildDependenciesIterator(dependencyGraph);

        while (this.dependenciesIterator.hasNext()) {
            GraphEdge e = this.dependenciesIterator.next();

            logger.debug("Checking if it is possible to select edge " + e);

            // We must check every time because manifest dependencies might introduce problems !
            if (this.isTestable(dependencyGraph, dependencyGraph.getEdgeSource(e), dependencyGraph.getEdgeTarget(e)) &&
                    !this.introduceCycle(dependencyGraph, dependencyGraph.getEdgeSource(e), dependencyGraph.getEdgeTarget(e))) {
                return Optional.of(e);
            }
        }

        // Here we did not find anything
        logger.info("No more edges available. Stopping refinement.");
        return Optional.empty();
    }

    private Iterator<GraphEdge> buildDependenciesIterator(Graph<GraphNode<T>, GraphEdge> dependencyGraph){
        try {
            logger.debug("Building dependencies iterator");
            List<GraphEdge> dependenciesToTest = new ArrayList<>();
            ConnectivityInspector<GraphNode<T>, GraphEdge> condect = new ConnectivityInspector<>(dependencyGraph);

            // wccs stands for weakly connected components: http://mathworld.wolfram.com/WeaklyConnectedComponent.html
            List<Set<GraphNode<T>>> wccs = condect.connectedSets();

            logger.debug("Found " + wccs.size() + " WCCs ");
            // Sort by MAX ID in wccs. Bigger ID first
            wccs.sort(new Comparator<Set<GraphNode<T>>>() {

                private int findMax(Set<GraphNode<T>> set) {
                    int max = -1;
                    for (GraphNode node : set)
                        if (node.getNumOrder() >= max)
                            max = node.getNumOrder();
                    return max;
                }

                @Override
                public int compare(Set<GraphNode<T>> o1, Set<GraphNode<T>> o2) {
                    int max1 = findMax(o1);
                    int max2 = findMax(o2);
                    // Note 2 before 1 !
                    return max2 - max1;
                }
            });

            for (Set<GraphNode<T>> wccNodes : wccs) {

                if (wccNodes.size() == 1) {
                    logger.debug("Skipping WCC " + wccs.indexOf(wccNodes) + " with "
                            + wccNodes.size() + " nodes");
                    continue;
                }

                logger.debug("Processing WCC " + wccs.indexOf(wccNodes) + " with "
                        + wccNodes.size() + " nodes");
                logger.debug("WCC Contains: " + wccNodes);

                List<GraphNode<T>> orderedNodes = new ArrayList<>(wccNodes);

                // order nodes in decreasing num order
                orderedNodes.sort(new ComparatorNodesDecreasing<>());

                for (GraphNode<T> graphNode : orderedNodes) {
                    logger.debug("Processing Graph node: " + graphNode);

                    // Get all the deps that include this node as source and
                    // sort them from the biggest to the smallest graph node order
                    List<GraphEdge> outgoingEdges = new ArrayList<>(dependencyGraph.outgoingEdgesOf(graphNode));

                    logger.debug("Node " + graphNode + " has outgoing edges " + outgoingEdges);

                    if (outgoingEdges.isEmpty()) {
                        logger.debug("Node " + graphNode + " has no outgoing edges. Skip it.");
                    }

                    outgoingEdges.sort((edge1, edge2)
                            -> dependencyGraph.getEdgeTarget(edge2).getNumOrder()
                            - dependencyGraph.getEdgeTarget(edge1).getNumOrder());

                    // Add them to the list of deps to test
                    dependenciesToTest.addAll(outgoingEdges);
                }
            }

            logger.debug("Dependencies to test: " + dependenciesToTest.size());
            return dependenciesToTest.iterator();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Source is the test WHICH requires Target to be executed
     *
     * Target not in PRE( Source - Target ) - not a formal definition
     *
     * Basically if we compute all the possible paths from Source to Target, the
     * count should be 1 at the time we compute it !
     *
     * @param dependencyGraph
     * @param graphSourceNode
     * @param graphTargetNode
     */
    private boolean isTestable(Graph<GraphNode<T>, GraphEdge> dependencyGraph,
                               GraphNode<T> graphSourceNode, GraphNode<T> graphTargetNode) {

        if (dependencyGraph.getEdge(graphSourceNode, graphTargetNode).isManifest()) {
            logger.debug("Dependency " + graphSourceNode + " -> " + graphTargetNode + " is already manifest.");
            return false;
        }

        // Find the WCC that contains source and target
        // Compute Weakly connected component(s) wccs
        ConnectivityInspector<GraphNode<T>, GraphEdge> condect = new ConnectivityInspector<>(dependencyGraph);

        List<Set<GraphNode<T>>> wccs = condect.connectedSets();

        int count = 0;
        for (Set<GraphNode<T>> nodesInWcc : wccs) {
            if (!nodesInWcc.contains(graphSourceNode) && !nodesInWcc.contains(graphTargetNode)) {
                continue;
            }
            count++;
        }
        if (count != 1) {
            throw new RuntimeException("ERROR: Wrong number of WCC that contain "
                    + dependencyGraph.getEdge(graphSourceNode, graphTargetNode) + ". Count is " + count);
        }

        for (Set<GraphNode<T>> nodesInWcc : wccs) {

            if (!nodesInWcc.contains(graphSourceNode) && !nodesInWcc.contains(graphTargetNode)) {
                continue;
            }

            // Build a WCC graph
            Graph<GraphNode<T>, GraphEdge> wcc = new DirectedAcyclicGraph<>(GraphEdge.class);
            Set<GraphEdge> relEdges = new HashSet<>();
            for (GraphNode<T> node : nodesInWcc) {
                wcc.addVertex(node);
                relEdges.addAll(dependencyGraph.edgesOf(node));
            }
            for (GraphEdge e : relEdges) {
                try {
                    GraphEdge wccEdge = e.clone();
                    wcc.addEdge(dependencyGraph.getEdgeSource(e), dependencyGraph.getEdgeTarget(e), wccEdge);
                } catch (IllegalArgumentException ex) {
                    // This should never happen
                    logger.error("ERROR !!");
                    throw new RuntimeException(ex);
                }
            }

            logger.debug("AllPaths algorithm start for " + wcc.vertexSet().size() + " -- " + wcc.edgeSet().size());

            long start = System.currentTimeMillis();

            AllDirectedPaths<GraphNode<T>, GraphEdge> allPaths = new AllDirectedPaths<>(wcc);

            int paths = allPaths.getAllPaths(graphSourceNode, graphTargetNode, true, wcc.edgeSet().size()).size();

            long end = System.currentTimeMillis();
            logger.debug("AllPaths algorithm took " + ((end - start) / 1000) + "s");
            if (paths > 1) {
                logger.debug("Multiple paths between " + graphSourceNode + " and "
                        + graphTargetNode + " dependency is not testable (it would create a cycle if inverted)");
                 // I guess because otherwise it creates a cycle. If the selected edge is e that connects S with T
                 // and there is another path between S and T through another node than inverting e would result in a cycle
                 return false;
            } else {
                logger.debug("Single path between " + graphSourceNode + " and "
                        + graphTargetNode + " dependency is testable");
                return true;
            }
        }
        logger.debug("No more WCC to test");
        return false;
    }

}
