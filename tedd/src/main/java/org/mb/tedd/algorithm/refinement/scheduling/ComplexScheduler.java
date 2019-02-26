package org.mb.tedd.algorithm.refinement.scheduling;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * When we compute the schedule for the next test execution, which breaks a
 * target data dependency, we need to be sure to keep intact all the remaining
 * data dependencies. Otherwise, the target test (affected by the data
 * dependency under test) might fail because of other precondition wrongly set.
 * 
 * This is not necessary if the data dependency are precise (down to single
 * static field); however, in some configurations dependencies are aggregated
 * per test-case testB depends on testA.
 *
 * Note that different options exist:
 * return all the tests (this might be beneficial to expose hidden/missing DD but costs a lot)
 * return all the tests up to the source,target of the target DD
 * return all the tests up to the target DD
 * return only the target DD (isolation)
 * etc..
 *
 */
public class ComplexScheduler<T> implements RefinementScheduler<T> {

	private boolean doubleCheck = false;
	private final static Logger logger = Logger.getLogger(ComplexScheduler.class.getName());

	// It was List<GraphNode<T>>. I changed it here because in OriginalOrderScheduler was wrong
    // so that it is consistent with the interface. I didn't actually check if here Set is correct or not.
    // If you plan to use this scheduler in the future make sure that Set is the proper way of storing the
    // schedule.
	@Override
	public Set<GraphNode<T>> computeSchedule(Graph<GraphNode<T>, GraphEdge> graph,
										   GraphEdge curEdge) {

		// 1. Find the wcc that containts curEdge. curEgde is NOT inverted yet ?
		// 1.1 if RUN_ALL take all the nodes - TODO this is not implemented
		// yet... Question, if this is active which wcc comes first, ideally we
		// should repeat the invocations as in REFERENCE_ORDER.

		// 2. Compute the Topological sort

		// 3. Create the schedule

		// Compute Weakly connected component(s) wccs
		ConnectivityInspector<GraphNode<T>, GraphEdge> condect = new ConnectivityInspector<>(graph);
		List<Graph<GraphNode<T>, GraphEdge>> allSubgraphs = new LinkedList<>();

		List<Set<GraphNode<T>>> wccs = condect.connectedSets();

		for (Set<GraphNode<T>> nodesInWcc : wccs) {

			// Rebuild the actual wcc as self-standing graph, only if it contains curEdge
			if (!nodesInWcc.contains(graph.getEdgeSource(curEdge))
					&& !nodesInWcc.contains(graph.getEdgeTarget(curEdge))) {
				continue;
			}

			Graph<GraphNode<T>, GraphEdge> wcc = new DirectedAcyclicGraph<>(GraphEdge.class);
			// Bookkeeping edges from the original graph
			Set<GraphEdge> relEdges = new HashSet<>();

			for (GraphNode<T> node : nodesInWcc) {
				// Clone the nodes ? Not necessarily
				wcc.addVertex(node);
				relEdges.addAll(graph.edgesOf(node));
			}
			for (GraphEdge e : relEdges) {
				try {
					if (!e.isIgnored()) {
						if (e.equals(curEdge) && !doubleCheck) {
							/*
							 * We invert the edge only if we are not
							 * double-checking the result !
							 */

							// Add this only if the edge is not
							logger.debug("Inverting " + curEdge);
							GraphEdge wccEdge = new GraphEdge(e.getDependentValues(), graph.getEdgeTarget(e), graph.getEdgeSource(e));
							wccEdge.setIgnored(e.isIgnored());
							wccEdge.setIntroducesCycle(e.isIntroducesCycle());
							wccEdge.setManifest(e.isManifest());
							wcc.addEdge(graph.getEdgeTarget(e), graph.getEdgeSource(e), wccEdge);
						} else {
							wcc.addEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e), e.clone());
						}

					} else {
						logger.debug("Ignoring " + e);
					}
				} catch (IllegalArgumentException e1) {
					// This should never happen ...
					throw new RuntimeException("Edge creates cycle ! " + curEdge);
				}
			}
			allSubgraphs.add(wcc);
		}

		// For the moment, since RUN_ALL is not implemented there should be one
		// and only one wcc

		if (allSubgraphs.size() != 1) {
			logger.debug("ERROR wrong allSubgraph count ! " + allSubgraphs.size());
		}

		// MERGE ALL THE SUBGRAPH INTO A SINGLE OBJECT AND FILTER IGNORED DEPS
		Graph<GraphNode<T>, GraphEdge> filteredInteresting = new DirectedAcyclicGraph<>(GraphEdge.class);
		for (Graph<GraphNode<T>, GraphEdge> subGraph : allSubgraphs) {
			for (GraphNode<T> graphNode : subGraph.vertexSet()) {
				filteredInteresting.addVertex(graphNode);
			}
		}
		for (Graph<GraphNode<T>, GraphEdge> subGraph : allSubgraphs) {
			for (GraphEdge graphEdge : subGraph.edgeSet()) {
				if (graphEdge.isIgnored()) {
					logger.debug("IGNORING DEP: " + graphEdge);
				} else {
					// This might create a new dep
					filteredInteresting.addEdge(subGraph.getEdgeSource(graphEdge), subGraph.getEdgeTarget(graphEdge), graphEdge.clone());
				}
			}
		}
		// Remove unconnected components - IS THIS SAFE ?!
		List<GraphNode<T>> toRemove = new ArrayList<>();
		for (GraphNode<T> graphNode : filteredInteresting.vertexSet()) {
			if (filteredInteresting.outDegreeOf(graphNode) == 0 && filteredInteresting.inDegreeOf(graphNode) == 0) {
				logger.debug("Graph node " + graphNode + " is isolated");
				toRemove.add(graphNode);
			}
		}

		// Include only the nodes needed to run (source->target)' ! So basically
		// all the reachable nodes from source

		GraphNode<T> reachFrom;
		if (!doubleCheck) {
			reachFrom = filteredInteresting.getEdgeTarget(curEdge);
		} else {
			reachFrom = filteredInteresting.getEdgeSource(curEdge);
		}


		DepthFirstIterator<GraphNode<T>, GraphEdge> reacheableFromSource = new DepthFirstIterator<>(
				filteredInteresting, reachFrom);

		Set<GraphNode<T>> reac = new HashSet<>();
		while (reacheableFromSource.hasNext()) {
			reac.add(reacheableFromSource.next());
		}

		logger.debug("Reachability set from " + reachFrom + " is " + reac);

		for (GraphNode<T> graphNode : filteredInteresting.vertexSet()) {
			if (!reac.contains(graphNode)) {
				toRemove.add(graphNode);
			}
		}

		for (GraphNode<T> del : toRemove) {
			filteredInteresting.removeVertex(del);
		}

		// In case of tie, we break it using test num order.
		TopologicalOrderIterator<GraphNode<T>, GraphEdge> it = new TopologicalOrderIterator<>(filteredInteresting,
				new Comparator<GraphNode<T>>() {
					@Override
					public int compare(GraphNode<T> o1, GraphNode<T> o2) {
						return o2.getNumOrder() - o1.getNumOrder();
					}
				});

		// Build the scheduling for the execution
		List<GraphNode<T>> scheduleList = new ArrayList<>();
		boolean atRequired = false;
		while (it.hasNext()) {
			GraphNode<T> s = it.next();

			if (atRequired) {
				scheduleList.add(s);
			} else if (s.equals(graph.getEdgeTarget(curEdge)) && !doubleCheck) {
				scheduleList.add(s);
				atRequired = true;
			} else if (s.equals(graph.getEdgeSource(curEdge)) && doubleCheck) {
				scheduleList.add(s);
				atRequired = true;
			}
		}

		Collections.reverse(scheduleList);
        Set<GraphNode<T>> scheduleSet = new LinkedHashSet<>(scheduleList);

		logger.debug("Schedule is " + scheduleSet);

		// TODO Here we can predict Execution time based on the schedule

		if (scheduleSet.isEmpty())
			throw new RuntimeException("empty schedule!");
		else {
			return scheduleSet;
		}

	}

	@Override
	public Set<GraphNode<T>> computeOriginalSchedule(Graph<GraphNode<T>, GraphEdge> graph, GraphEdge curEdge) throws InvalidScheduleException {
		throw new UnsupportedOperationException("Not implemented yet!");
	}
}
