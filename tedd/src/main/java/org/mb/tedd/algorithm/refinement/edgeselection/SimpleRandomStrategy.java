package org.mb.tedd.algorithm.refinement.edgeselection;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;


/**
 * Randomly select an edge which does not create a cycle when inverted.
 *
 */
public class SimpleRandomStrategy<T> extends RefinementStrategy<T> {

    private Random random;
    private final static Logger logger = Logger.getLogger(SimpleRandomStrategy.class.getName());

    public SimpleRandomStrategy() {
        Long seed = Long.getLong("seed");
        if (seed == null) {
            seed = System.currentTimeMillis();
            logger.debug("No seed specified. Using randomly generated seed " + seed);
        } else {
            logger.debug("Using provided seed " + seed);
        }
        random = new Random(seed);
    }

    @Override
    public Optional<GraphEdge> selectEdge(final Graph<GraphNode<T>, GraphEdge> dependencyGraph) {

        // Randomize the edges using the provided seed
        List<GraphEdge> edges = new LinkedList<>();
        edges.addAll(dependencyGraph.edgeSet());
        Collections.shuffle(edges, random);

        for (GraphEdge e : edges) {

            // Skip Dependency Edges that are already manifest
            if (e.isManifest()) {
                continue;
            }
            // Check
            if (!this.introduceCycle(dependencyGraph, dependencyGraph.getEdgeSource(e), dependencyGraph.getEdgeTarget(e))) {
                return Optional.of(e);
            }
        }

        // Here we did not find anything
        logger.debug("No more edges available. Stopping refinement");
        return Optional.empty();
    }

}
