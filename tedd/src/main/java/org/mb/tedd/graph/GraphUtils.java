package org.mb.tedd.graph;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.algorithm.extraction.string.StringConstantValue;
import org.mb.tedd.algorithm.extraction.string.StringValue;
import org.mb.tedd.statement.LiteralParameter;
import org.mb.tedd.utils.input.PromptUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GraphUtils {

    public static String missingDepPrefix = "missing_added_";

    private final static Logger logger = Logger.getLogger(GraphUtils.class.getName());

    public GraphUtils(){

    }

    public static Set<GraphNode<String>> mapTestsOrderToNodesOrder(List<String> testsOrder){
        Set<GraphNode<String>> nodesOrder = new LinkedHashSet<>();
        for (int i = 0; i < testsOrder.size(); i++) {
            nodesOrder.add(new GraphNode<>(testsOrder.get(i), i));
        }
        return nodesOrder;
    }

    /**
     * @implNote modifies graph by adding an edge provided by the user
     * */
    public static void addEdgeToGraphManually(Graph<GraphNode<String>, GraphEdge> graph){
        PromptUser promptUser = new PromptUser();
        GraphNode<String> graphSourceNode = promptUser.getSourceNode();
        GraphNode<String> graphTargetNode = promptUser.getTargetNode();
        StringValue stringValue = new StringConstantValue(new LiteralParameter(missingDepPrefix + "manually"));
        boolean sure = promptUser.promptAreYouSure("Are you sure you want to add " + graphSourceNode
                + " -> " + graphTargetNode + "? If yes it will be added as manifest.");
        GraphEdge graphEdge = new GraphEdge(Arrays.asList(stringValue), graphSourceNode, graphTargetNode);
        if(sure)
            graphEdge.setManifest(true);
        if(!graph.addEdge(graphSourceNode, graphTargetNode, graphEdge)){
            logger.warn("Graph already contains " + graphEdge);
        }
    }

    /**
     * @implNote modifies graph by adding an edge provided by the user
     * @param candidatePreconditions set of test cases among which there is the graphTargetNode of
     *                               a missing dependency
     * @param graphSourceNode of a missing dependency is provided automatically while graphTargetNode has to be
     *                    chosen manually by the user among those tests in @param
     *                    candidatePreconditions
     * @return graphEdge added
     * */
    public static GraphEdge addEdgeToGraphManually(Graph<GraphNode<String>, GraphEdge> graph,
                                              Set<GraphNode<String>> candidatePreconditions,
                                              GraphNode<String> graphSourceNode){
        PromptUser promptUser = new PromptUser();
        GraphNode<String> graphTargetNode = promptUser.chooseInSet(candidatePreconditions);
        boolean sure = promptUser.promptAreYouSure("Are you sure you want to add " + graphSourceNode
                + " -> " + graphTargetNode + "? If yes it will be added as manifest.");
        StringValue stringValue = new StringConstantValue(new LiteralParameter(missingDepPrefix + "manually"));
        GraphEdge graphEdge = new GraphEdge(Arrays.asList(stringValue), graphSourceNode, graphTargetNode);
        if(sure)
            graphEdge.setManifest(true);
        if(!graph.addEdge(graphSourceNode, graphTargetNode, graphEdge)){
            logger.warn("Graph already contains " + graphEdge);
        }
        return graphEdge;
    }

    /**
     * @implNote modifies graph by automatically adding an edge.
     * @param graph dependency graph
     * @param graphSourceNode of a missing dependency
     * @param graphTargetNode of a missing dependency
     * @param isManifest if true the method adds the dependency as manifest
     * @return graphEdge added if @param graph does not contain the edge, empty otherwise
     * */
    @SuppressWarnings("unchecked")
    public static Optional<GraphEdge> addEdgeToGraphAutomatically(Graph<GraphNode<String>, GraphEdge> graph,
                                                                  GraphNode<String> graphSourceNode,
                                                                  GraphNode<String> graphTargetNode,
                                                                  boolean isManifest){
        StringValue stringValue = new StringConstantValue(new LiteralParameter(missingDepPrefix + "automatically"));
        GraphEdge graphEdge = new GraphEdge(Arrays.asList(stringValue), graphSourceNode, graphTargetNode);
        if(!graph.addEdge(graphSourceNode, graphTargetNode, graphEdge)){
            logger.warn("Graph already contains " + graphEdge);
            return Optional.empty();
        }
        if(isManifest)
            graphEdge.setManifest(true);
        return Optional.of(graphEdge);
    }

    /**
     * @implNote modifies @param graph
     * @return graphEdgeInverted
     * */
    public static GraphEdge invertEdge(Graph<GraphNode<String>, GraphEdge> graph, GraphEdge edgeToInvert){
        GraphNode<String> graphNodeSource = graph.getEdgeSource(edgeToInvert);
        GraphNode<String> graphNodeTarget = graph.getEdgeTarget(edgeToInvert);
        GraphEdge graphEdgeInverted = new GraphEdge(new ArrayList<>(edgeToInvert.getDependentValues()), graphNodeTarget, graphNodeSource);
        if(!graph.removeEdge(edgeToInvert)){
            throw new IllegalArgumentException("Edge " + edgeToInvert + " is not in the graph.");
        }
        if(!graph.addEdge(graphNodeTarget, graphNodeSource, graphEdgeInverted)){
            throw new IllegalArgumentException("Edge " + edgeToInvert + " is still in the graph.");
        }
        return graphEdgeInverted;
    }
}
