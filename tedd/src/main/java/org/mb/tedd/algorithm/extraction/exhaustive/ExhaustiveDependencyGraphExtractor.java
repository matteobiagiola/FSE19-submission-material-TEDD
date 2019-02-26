package org.mb.tedd.algorithm.extraction.exhaustive;

import org.apache.log4j.Logger;
import org.mb.tedd.algorithm.extraction.DependencyGraphExtractor;
import org.mb.tedd.graph.DependencyGraphManager;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.utils.ExecutionTime;
import org.mb.tedd.utils.Properties;

import java.util.ArrayList;
import java.util.List;

public class ExhaustiveDependencyGraphExtractor implements DependencyGraphExtractor {

    private final static Logger logger = Logger.getLogger(ExhaustiveDependencyGraphExtractor.class.getName());

    @Override
    public DependencyGraphManager<String> computeDependencies() {
        long start = System.currentTimeMillis();
        DependencyGraphManager<String> dependencyGraphManager = new DependencyGraphManager<>();
        List<GraphNode<String>> nodes = new ArrayList<>();
        for (int i = 0; i < Properties.tests_order.length; i++) {
            nodes.add(new GraphNode<>(Properties.tests_order[i], i));
        }
        for (int i = 1; i < nodes.size(); i++) {
            this.addDependenciesTowards(nodes.subList(0, i), nodes.get(i), dependencyGraphManager);
        }
        long timeElapsed = System.currentTimeMillis() - start;
        logger.info("Time to compute the initial dependency graph: "
                + new ExecutionTime().computeExecutionTime(timeElapsed));
        return dependencyGraphManager;
    }

    private void addDependenciesTowards(List<GraphNode<String>> targetNodes,
                                               GraphNode<String> sourceNode,
                                               DependencyGraphManager<String> dependencyGraphManager){
        for (GraphNode<String> targetNode : targetNodes) {
            dependencyGraphManager.addDependency(sourceNode, targetNode,
                    new GraphEdge(new ArrayList<>(), sourceNode, targetNode));
        }
    }
}
