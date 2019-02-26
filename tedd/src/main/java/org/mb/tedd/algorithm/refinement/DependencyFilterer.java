package org.mb.tedd.algorithm.refinement;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.graph.GraphUtils;
import org.mb.tedd.utils.Properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyFilterer {

    private List<GraphNode<String>> originalOrder;
    private final static Logger logger = Logger.getLogger(DependencyFilterer.class.getName());

    public DependencyFilterer(){
        List<String> testsOrder = Arrays.asList(Properties.tests_order);
        this.originalOrder = new ArrayList<>(GraphUtils.mapTestsOrderToNodesOrder(testsOrder));
    }

    /**
     * @implNote works if there are manifest dependencies in the dependency graph;
     * it modifies the input graph by removing or marking edges; it does not take dependency values into account
     * @param removeUninterestingDependencies if true it removes dependency from graph
     * @return set of uninteresting dependencies (empty set if there are no uninteresting dependencies)
     * */
    public Set<GraphEdge> filterUninterestingDependencies(Graph<GraphNode<String>, GraphEdge> dependencyGraph,
                                                                               boolean removeUninterestingDependencies){
        Set<GraphEdge> uninterestingDependencies = new LinkedHashSet<>();
        for (int i = this.originalOrder.size() - 1; i > 0; i--) {
            GraphNode<String> nodeUnderAnalysis = this.originalOrder.get(i);
            Set<GraphEdge> nonManifestEdges = dependencyGraph.edgesOf(nodeUnderAnalysis).stream()
                    .filter(graphEdge -> !graphEdge.isManifest()).collect(Collectors.toSet());
            for (GraphEdge nonManifestEdge: nonManifestEdges){
                GraphNode<String> targetNode = dependencyGraph.getEdgeTarget(nonManifestEdge);
                AllDirectedPaths<GraphNode<String>, GraphEdge> allPaths = new AllDirectedPaths<>(dependencyGraph);
                List<GraphPath<GraphNode<String>, GraphEdge>> paths = allPaths
                        .getAllPaths(nodeUnderAnalysis, targetNode, true, dependencyGraph.edgeSet().size());
                boolean manifestPathExists = paths.stream().anyMatch(graphPath -> {
                    if(graphPath.getLength() == 0 || graphPath.getLength() == 1)
                        return false;
                    List<GraphEdge> manifestEdges = graphPath.getEdgeList().stream()
                            .filter(GraphEdge::isManifest).collect(Collectors.toList());
                    if(manifestEdges.size() == graphPath.getEdgeList().size()){
                        logger.debug("There exists a path of manifest deps between " + nodeUnderAnalysis + " and "
                                + targetNode + ". The path is: " + manifestEdges + ".");
                        return true;
                    }
                    return false;
                });
                if(manifestPathExists){
                    if(removeUninterestingDependencies){
                        logger.info("Removing " + nonManifestEdge + " since it is uninteresting!");
                        dependencyGraph.removeEdge(nonManifestEdge);
                    } else {
                        logger.info("Marking " + nonManifestEdge + " as uninteresting!");
                        nonManifestEdge.setInteresting(false);
                    }
                    uninterestingDependencies.add(nonManifestEdge);
                }
            }
        }
        return uninterestingDependencies;
    }
}
