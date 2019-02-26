package org.mb.tedd.main;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.algorithm.refinement.DependencyRefiner;
import org.mb.tedd.graph.DependencyGraphManager;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.graph.dot.exportgraph.GraphExporter;
import org.mb.tedd.utils.ExecutionTime;
import org.mb.tedd.utils.Properties;

import java.util.ArrayList;
import java.util.List;

public class BaselineCompleteGraph {

    private final static Logger logger = Logger.getLogger(BaselineCompleteGraph.class.getName());

    public static void main(String[] args){

        config();

        Preconditions.checkArgument(Properties.BASELINE, "Baseline flag must be true.");

        long start = System.currentTimeMillis();
        DependencyGraphManager<String> dependencyGraphManager = new DependencyGraphManager<>();
        List<GraphNode<String>> nodes = new ArrayList<>();
        for (int i = 0; i < Properties.tests_order.length; i++) {
            nodes.add(new GraphNode<>(Properties.tests_order[i], i));
        }
        for (int i = 1; i < nodes.size(); i++) {
            addDependenciesTowards(nodes.subList(0, i), nodes.get(i), dependencyGraphManager);
        }
        long timeElapsed = System.currentTimeMillis() - start;
        logger.info("Time to compute the initial dependency graph: " + new ExecutionTime().computeExecutionTime(timeElapsed));

        Graph<GraphNode<String>, GraphEdge> dependencyGraph = dependencyGraphManager.getDependencyGraph();
        GraphExporter<String> graphExporter = new GraphExporter<>(dependencyGraph);
        graphExporter.export("dependency-graph-initial-baseline");

        logger.info("Number of dependencies in complete graph: " + dependencyGraphManager.getDependencyGraph().edgeSet().size());

        if(!Properties.ONLY_COMPUTE_GRAPH_BUILD_TIME){
            DependencyRefiner dependencyRefiner = new DependencyRefiner();
            try {

                dependencyRefiner.refine(dependencyGraph);
                graphExporter = new GraphExporter<>(dependencyGraph);
                graphExporter.export("dependency-graph-final-baseline");

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }


            // somehow refine hangs
            System.exit(0);
        }

    }

    private static void addDependenciesTowards(List<GraphNode<String>> targetNodes,
                                               GraphNode<String> sourceNode,
                                               DependencyGraphManager<String> dependencyGraphManager){
        for (GraphNode<String> targetNode : targetNodes) {
            dependencyGraphManager.addDependency(sourceNode, targetNode,
                    new GraphEdge(new ArrayList<>(), sourceNode, targetNode));
        }
    }

    private static void config(){
        instantiateProperties();
    }

    private static void instantiateProperties(){
        Properties.getInstance().createPropertiesFile();
    }
}
