package org.mb.tedd.algorithm.refinement.edgeselection;


import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;


/**
 * This implements the break cycles as described in the FSE paper.
 * 
 */
public class RandomStrategy<T> extends RefinementStrategy<T> {

	private Random random;
	private final static Logger logger = Logger.getLogger(RandomStrategy.class.getName());
	// This keep the list of edges to test when we come across a cycle.
	private Queue<GraphEdge> localBuffer;
	private Set<GraphEdge> commonPreconditions;

	public RandomStrategy() {
		Long seed = Long.getLong("seed");
		if (seed == null) {
			seed = System.currentTimeMillis();
			logger.debug("No seed specified. Using randomly generated seed " + seed);
		} else {
			logger.debug("Using provided seed " + seed);
		}
		random = new Random(seed);
		this.localBuffer = new LinkedList<>();
		this.commonPreconditions = new HashSet<>();
	}

	// We need to ignore the ignored deps !
	private Optional<Graph<GraphNode<T>, GraphEdge>> createCycle(final Graph<GraphNode<T>, GraphEdge> dependencyGraph, GraphEdge edge) {

		Graph<GraphNode<T>, GraphEdge> copy = new DirectedAcyclicGraph<>(GraphEdge.class);

		for (GraphNode<T> graphNode : dependencyGraph.vertexSet()) {
			GraphNode<T> graphNodeCopy = new GraphNode<>(graphNode.getTestCase(), graphNode.getNumOrder());
			copy.addVertex(graphNodeCopy);
		}
		for (GraphEdge ge : dependencyGraph.edgeSet()) {

			if (ge.isIgnored()) {
				logger.debug("Ignoring " + ge);
				continue;
			}

			GraphNode<T> toVertex = null;
			GraphNode<T> fromVertex = null;

			for (GraphNode<T> graphNode : copy.vertexSet()) {
				if (graphNode.getTestCase().equals(dependencyGraph.getEdgeSource(ge).getTestCase())) {
					fromVertex = graphNode;
					continue;
				}
				if (graphNode.getTestCase().equals(dependencyGraph.getEdgeTarget(ge).getTestCase())) {
					toVertex = graphNode;
					continue;
				}

				if (fromVertex != null && toVertex != null) {
					break;
				}
			}

			try {
				copy.addEdge(fromVertex, toVertex, ge.clone());
			} catch (IllegalArgumentException e1) {
				throw new RuntimeException(e1);
			}
		}

		GraphNode<T> src = null;
		GraphNode<T> tgt = null;
		for (GraphNode<T> graphNode : copy.vertexSet()) {
			if (graphNode.getTestCase().equals(dependencyGraph.getEdgeSource(edge).getTestCase())) {
				src = graphNode;
				continue;
			}
			if (graphNode.getTestCase().equals(dependencyGraph.getEdgeTarget(edge).getTestCase())) {
				tgt = graphNode;
				continue;
			}

			if (src != null && tgt != null) {
				break;
			}
		}
		GraphEdge copyEdge = copy.getEdge(src, tgt);

		logger.debug("Testing " + copyEdge + " for cycle");

		copy.removeEdge(copyEdge);
		try {
			copy.addEdge(tgt, src, new GraphEdge(copyEdge.getDependentValues(), tgt, src));
			return Optional.of(copy);
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}

	}

	private Optional<GraphEdge> fromLocalBuffer(final Graph<GraphNode<T>, GraphEdge> dependencyGraph) {
		while (!localBuffer.isEmpty()) {
			// Remove from local buffer
			GraphEdge e = localBuffer.poll();

			if(e == null)
				throw new RuntimeException("Queue is empty");

			logger.debug("\n\n\n Selecting Dependency EDGE from the Local Buffer: " + e);
			// Configure the ignore tags
			for (GraphEdge cp : commonPreconditions) {
				if (cp.equals(e)) {
					cp.setIgnored(false);
				} else {
					logger.debug("IGNORING  " + cp);
					// Ignoring the others
					cp.setIgnored(true);
				}
			}

			// Check the edge
			if (this.createCycle(dependencyGraph, e) != null) {
				return Optional.of(e);
			} else {
				logger.debug("Cycle inside cycle for " + e + " Local Buffer. SKIP");
			}
		}
		return Optional.empty();
	}

	private void fillLocalBuffer(final Graph<GraphNode<T>, GraphEdge> dependencyGraph, GraphEdge e) {
		/*
		 * Test source has multiple preconditions. Collect them and process them !
		 */
		final Comparator<GraphEdge> comparator = new Comparator<GraphEdge>() {
			@Override
			public int compare(GraphEdge o1, GraphEdge o2) {
				return dependencyGraph.getEdgeTarget(o1).getNumOrder() - dependencyGraph.getEdgeTarget(o2).getNumOrder();
			}
		};

		List<GraphEdge> orderedEdges = new ArrayList<>(dependencyGraph.outgoingEdgesOf(dependencyGraph.getEdgeSource(e)));
		orderedEdges.sort(comparator);
		// Enqueue Deps for the next round
		Collections.reverse(orderedEdges);
		// Not sure if addAll preserves the order
		for (GraphEdge de : orderedEdges) {
			localBuffer.add(de);
			commonPreconditions.add(de);
			logger.debug("Adding " + de + " to Local Buffer ");
		}
	}

	/**
	 * This must ensure that when we return an edge this will not create a
	 * cycle. This includes also the case with 'ignored' deps.
	 */
	@Override
	public Optional<GraphEdge> selectEdge(final Graph<GraphNode<T>, GraphEdge> dependencyGraph) {

		Optional<GraphEdge> d = this.fromLocalBuffer(dependencyGraph);
		if (d.isPresent())
			return d;

		// Reset ignore attribute
		if (!this.commonPreconditions.isEmpty()) {
			for (GraphEdge cp : commonPreconditions) {
				cp.setIgnored(false);
			}
			commonPreconditions.clear();
		}

		// Randomize the remaining edges
		List<GraphEdge> edges = new LinkedList<>();
		edges.addAll(dependencyGraph.edgeSet());
		Collections.shuffle(edges, random);

		logger.debug("Remaining Edges " + edges.size());
		for (GraphEdge e : edges) {

			logger.debug("Select " + e);
			// Skip Dependencies that are already manifest
			if (e.isManifest()) {
				logger.debug(" Dep is already manifest");
				continue;
			}

			// TODO Comment on what's the meaning of this one !
			if (dependencyGraph.outDegreeOf(dependencyGraph.getEdgeSource(e)) > 1) {
				this.fillLocalBuffer(dependencyGraph, e);
				// At this point we might return the first in line
				d = this.fromLocalBuffer(dependencyGraph);
				if (d.isPresent()) {
					logger.debug("Returning " + d.get() + " from local buffer");
					return d;
				}

			} else {

				// TODO Better compute the common preconditions here, even
				// before checking for cycles

				// Check if by reverting the edge we introduce a cycle:
				// 1 - Create a full duplicate of the graph: if THIS already has cycle, something was wrong !

				// 2 - Find in the duplicate the edge corresponding to the targetDataDependency and remove it from the graph
				if (this.introduceCycle(dependencyGraph, dependencyGraph.getEdgeSource(e),
						dependencyGraph.getEdgeTarget(e))) {
					logger.debug("Found a cycle with " + e + " select the best dep to break the cycle");

				} else {
					logger.debug("Inverting Dependency EDGE " + e);

					if (dependencyGraph.outDegreeOf(dependencyGraph.getEdgeSource(e)) > 1) {
						this.fillLocalBuffer(dependencyGraph, e);

						d = this.fromLocalBuffer(dependencyGraph);
						if (d.isPresent())
							return d;

					} else {
						return Optional.of(e);
					}

				}
			}
		}

		// Here we did not find anything
		logger.debug("Stopping the search");
		return Optional.empty();
	}

}
