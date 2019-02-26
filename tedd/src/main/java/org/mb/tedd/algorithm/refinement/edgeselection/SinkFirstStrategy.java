package org.mb.tedd.algorithm.refinement.edgeselection;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;

import java.util.Optional;
import java.util.Set;


public class SinkFirstStrategy<T> extends RefinementStrategy<T> {

	private static final int MAX_IN_DEGREE = 100;
	private final static Logger logger = Logger.getLogger(SinkFirstStrategy.class.getName());

	@Override
	public Optional<GraphEdge> selectEdge(Graph<GraphNode<T>, GraphEdge> dependencyGraph) {

		for (int i = 1; i <= MAX_IN_DEGREE; i++) {
			for (GraphNode<T> v : dependencyGraph.vertexSet()) {
				if (dependencyGraph.outDegreeOf(v) == 0 && dependencyGraph.inDegreeOf(v) == i) {
					// we have a sink with only one incoming edge
					Set<GraphEdge> e = dependencyGraph.edgesOf(v);

					if (e.size() != i)
						throw new RuntimeException("SinkStrategy found " + e.size() + " edges instead of" + i);

					// TODO maybe clever heuristic here?
					GraphEdge ret = e.iterator().next();
					logger.debug("Found sink " + ret + " in degree of " + i);
					return Optional.of(ret);
				}
			}
		}

		// No suitable sink found. Fallback: Random
		logger.debug("Didn't find a sink. Using SimpleRandomStrategy...");
		return new SimpleRandomStrategy<T>().selectEdge(dependencyGraph);
	}

}
