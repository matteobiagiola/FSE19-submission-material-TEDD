package org.mb.tedd.algorithm.refinement.scheduling;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * When we compute the schedule for the next test execution, which breaks a
 * target data dependency, we need to be sure to keep intact all the remaining
 * data dependencies. Otherwise, the target test (affected by the data
 * dependency under test) might fail because of other precondition wrongly set.
 * 
 * This is not necessary if the data dependency is precise (down to single
 * static field); however, in some configurations dependencies are aggregated
 * per test-case testB depends on testA.
 *
 * Note that different options exist: return all the tests (this might be
 * beneficial to expose hidden/missing DD but costs a lot) return all the tests
 * up to the source,target of the target DD return all the tests up to the
 * target DD return only the target DD (isolation) etc..
 * 
 * TODO NOTE THAT WE DO NOT CARE ABOUT IGNORED DEPS AT THE MOMENT.
 *
 */
public class OriginalOrderScheduler<T> implements RefinementScheduler<T> {

	private List<GraphNode<T>> originalOrder;
	private final static Logger logger = Logger.getLogger(OriginalOrderScheduler.class.getName());

	public OriginalOrderScheduler(Set<GraphNode<T>> originalOrder) {
		this.originalOrder = new ArrayList<>();
		this.originalOrder.addAll(originalOrder);
	}

	private Graph<GraphNode<T>, GraphEdge> findWccContainingTargetDataDependency(Graph<GraphNode<T>, GraphEdge> graph, GraphEdge targetDataDependency)
			throws InvalidScheduleException {
		// Compute Weakly connected component(s) wccs
		ConnectivityInspector<GraphNode<T>, GraphEdge> condect = new ConnectivityInspector<>(graph);

		List<Set<GraphNode<T>>> wccs = condect.connectedSets();

		int count = 0;
		for (Set<GraphNode<T>> nodesInWcc : wccs) {

			if (!nodesInWcc.contains(graph.getEdgeSource(targetDataDependency))
					&& !nodesInWcc.contains(graph.getEdgeTarget(targetDataDependency))) {
				continue;
			}
			count++;
		}
		if (count != 1) {
			throw new RuntimeException(
					"ERROR: Wrong number of WCC that contain " + targetDataDependency + ". Count is " + count);
		}

		for (Set<GraphNode<T>> nodesInWcc : wccs) {

			if (!nodesInWcc.contains(graph.getEdgeSource(targetDataDependency))
					&& !nodesInWcc.contains(graph.getEdgeTarget(targetDataDependency))) {
				continue;
			}

			logger.debug("Found WCC that contains " + targetDataDependency + ":\n" + nodesInWcc);

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

                    GraphEdge wccEdge;
					// Maintain all the attributes of the original graph !
					// WHAT IF TARGET DEP IS IGNORED ?!!
					if (e.equals(targetDataDependency)) {
						logger.debug("Inverting " + targetDataDependency);
						/*
						 * We invert the edge only if we are not double-checking
						 * the result !
						 */
						wccEdge = new GraphEdge(e.getDependentValues(), graph.getEdgeTarget(e), graph.getEdgeSource(e));
						wcc.addEdge(graph.getEdgeTarget(e), graph.getEdgeSource(e), wccEdge);
					} else {
                        wccEdge = new GraphEdge(e.getDependentValues(), graph.getEdgeSource(e), graph.getEdgeTarget(e));
						wcc.addEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e), wccEdge);
					}

                    wccEdge.setIgnored(e.isIgnored());
                    wccEdge.setIntroducesCycle(e.isIntroducesCycle());
                    wccEdge.setManifest(e.isManifest());

				} catch (IllegalArgumentException e1) {
					// This should never happen ...
					throw new InvalidScheduleException("An Edge creates cycle ! " + targetDataDependency);
				}
			}
			return wcc;
		}
		throw new RuntimeException("Cannot find WCC containing " + targetDataDependency);

	}

	@Override
	public Set<GraphNode<T>> computeSchedule(Graph<GraphNode<T>, GraphEdge> dataDependencyGraph,
											  GraphEdge targetDataDependency) throws InvalidScheduleException {

		// 1. Find the wcc that contains targetDataDependency, since only the
		// WCCs make sense here (basically this scheduler does not support the
		// RUNALL option). By definition only 1 WCC shall contain the
		// targetDataDependency. Node that we IGNORE, i.e., do not report
		// ignored dependencies in the

		GraphNode<T> dependentTest = dataDependencyGraph.getEdgeSource(targetDataDependency);
		GraphNode<T> preconditionSetterTest = dataDependencyGraph.getEdgeTarget(targetDataDependency);

		logger.info("Dependent test: " + dependentTest);
		logger.info("Precondition setter test: " + preconditionSetterTest);

		// Nodes should be the same objects, edges are different...
		Graph<GraphNode<T>, GraphEdge> wcc
				= this.findWccContainingTargetDataDependency(dataDependencyGraph, targetDataDependency);

		// 2. Compute the precondition of dependentTest minus the
		// preconditionSetterTest
		// wcc);
//		BreadthFirstIterator<GraphNode<T>, GraphEdge> it = new BreadthFirstIterator<>(dataDependencyGraph, preconditionSetterTest);

		// Creates a new breadth-first iterator for the specified graph.
		// Iteration will start at the specified start vertex and will be limited
		// to the connected component that includes that vertex.
		// If the specified start vertex is null, iteration will start at an arbitrary
		// vertex and will not be limited, that is, will be able to traverse all the graph.
		BreadthFirstIterator<GraphNode<T>, GraphEdge> it
				= new BreadthFirstIterator<>(wcc, dependentTest);
		// Fast forward to dependentTest node
		List<GraphNode<T>> preconditions = new ArrayList<>();
		while (it.hasNext()) {
			preconditions.add(it.next());
		}
        logger.debug("Breadth first iterator from " + dependentTest + " to test dependency "
                + targetDataDependency);

		logger.debug("Preconditions of " + dependentTest + " when dependency is inverted are: "
				+ preconditions);
		// Build the schedule for the execution: it contains only the nodes in
		// WCC that are preconditions or dependentTest AND nodes are ordered
		// according
		// to originalOrder
		//
		// We obtain this by cloning originalOrder and removing all the nodes
		// that are not in the preconditions
		Set<GraphNode<T>> schedule = new LinkedHashSet<>(); // LinkedHashSet is ordered
		logger.debug("Original Order " + originalOrder);

		for (GraphNode<T> graphNode : originalOrder) {
			if (preconditions.contains(graphNode)) {
				schedule.add(graphNode);
			}
		}

		// TODO Here we can predict Execution time based on the schedule

		if (schedule.isEmpty())
			throw new RuntimeException("Empty schedule!");
		else {
			return schedule;
		}

	}

	@Override
	public Set<GraphNode<T>> computeOriginalSchedule(Graph<GraphNode<T>, GraphEdge> dataDependencyGraph,
											 GraphEdge targetDataDependency) throws InvalidScheduleException {

		// 1. Find the wcc that contains targetDataDependency, since only the
		// WCCs make sense here (basically this scheduler does not support the
		// RUNALL option). By definition only 1 WCC shall contain the
		// targetDataDependency. Node that we IGNORE, i.e., do not report
		// ignored dependencies in the

		GraphNode<T> dependentTest = dataDependencyGraph.getEdgeSource(targetDataDependency);
		GraphNode<T> preconditionSetterTest = dataDependencyGraph.getEdgeTarget(targetDataDependency);

		logger.info("Dependent test: " + dependentTest);
		logger.info("Precondition setter test: " + preconditionSetterTest);

		// Nodes should be the same objects, edges are different...
		Graph<GraphNode<T>, GraphEdge> wcc
				= this.findWccContainingTargetDataDependency(dataDependencyGraph, targetDataDependency);

		// 2. Compute the precondition of dependentTest minus the
		// preconditionSetterTest
		// wcc);
//		BreadthFirstIterator<GraphNode<T>, GraphEdge> it = new BreadthFirstIterator<>(dataDependencyGraph, preconditionSetterTest);

		// Creates a new breadth-first iterator for the specified graph.
		// Iteration will start at the specified start vertex and will be limited
		// to the connected component that includes that vertex.
		// If the specified start vertex is null, iteration will start at an arbitrary
		// vertex and will not be limited, that is, will be able to traverse all the graph.
		BreadthFirstIterator<GraphNode<T>, GraphEdge> it
				= new BreadthFirstIterator<>(wcc, preconditionSetterTest);
		// Fast forward to preconditionSetterTest node
		List<GraphNode<T>> preconditions = new ArrayList<>();
		while (it.hasNext()) {
			preconditions.add(it.next());
		}
		logger.debug("Breadth first iterator from " + preconditionSetterTest + " to test dependency "
				+ targetDataDependency);

		logger.debug("Preconditions of " + preconditionSetterTest + " when dependency is inverted are: "
				+ preconditions);
		// Build the schedule for the execution: it contains only the nodes in
		// WCC that are preconditions or dependentTest AND nodes are ordered
		// according
		// to originalOrder
		//
		// We obtain this by cloning originalOrder and removing all the nodes
		// that are not in the preconditions
		Set<GraphNode<T>> schedule = new LinkedHashSet<>(); // LinkedHashSet is ordered
		logger.debug("Original Order " + originalOrder);

		for (GraphNode<T> graphNode : originalOrder) {
			if (preconditions.contains(graphNode)) {
				schedule.add(graphNode);
			}
		}
		// Add dependentTest
		schedule.add(dependentTest);

		// TODO Here we can predict Execution time based on the schedule

		if (schedule.isEmpty())
			throw new RuntimeException("Empty schedule!");
		else {
			return schedule;
		}

	}
}
