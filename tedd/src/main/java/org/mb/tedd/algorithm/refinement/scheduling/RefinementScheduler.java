package org.mb.tedd.algorithm.refinement.scheduling;

import org.jgrapht.Graph;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;

import java.util.Set;


public interface RefinementScheduler<T> {

	/**
	 * Given the dependency graph and a target dependency to test in it, returns
	 * a schedule for the next test execution.
	 *
	 * Shall this consider also the test which set the preconditions ? Why ?!
	 * TODO Make this configurable ?
	 * 
	 * @param graph
	 * @param curEdge
	 * @throws InvalidScheduleException
	 */
	Set<GraphNode<T>> computeSchedule(Graph<GraphNode<T>, GraphEdge> graph, GraphEdge curEdge) throws InvalidScheduleException;

	/**
	 * Given the dependency graph and a target dependency to test in it, returns
	 * a schedule for the next test execution. Differently from the above method, this
	 * method computes the schedule when the dependency @param curEdge is not inverted.
	 * This method is useful when checking for missing dependencies and to compute paths
	 * that respect all the dependencies in the validated (all deps are manifest) dependency graph.
	 *
	 * @param graph
	 * @param curEdge
	 * @throws InvalidScheduleException
	 */
	Set<GraphNode<T>> computeOriginalSchedule(Graph<GraphNode<T>, GraphEdge> graph, GraphEdge curEdge) throws InvalidScheduleException;

}