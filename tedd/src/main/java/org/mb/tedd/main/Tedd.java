package org.mb.tedd.main;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.algorithm.extraction.string.StringAnalysisDependencyGraphExtractor;
import org.mb.tedd.algorithm.filter.FilterStrategy;
import org.mb.tedd.algorithm.filter.FilterType;
import org.mb.tedd.algorithm.filter.common_values.CommonValuesStrategy;
import org.mb.tedd.algorithm.filter.nlp.NlpFilterStrategy;
import org.mb.tedd.algorithm.refinement.DependencyRefiner;
import org.mb.tedd.algorithm.refinement.RecoverMissedDependencies;
import org.mb.tedd.graph.DependencyGraphManager;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.graph.dot.exportgraph.GraphExporter;
import org.mb.tedd.parsing.TestCaseFinder;
import org.mb.tedd.utils.ExecutionTime;
import org.mb.tedd.utils.Properties;
import spoon.reflect.declaration.CtClass;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Tedd {

    private final static Logger logger = Logger.getLogger(Tedd.class.getName());

    /**
     * @implNote it computes the dependency graph and filters the dependencies on it with the chosen filter type
     */
    public static void main(String[] args) {

        config();

        Preconditions.checkArgument(Properties.FILTER_DEPENDENCIES,
                "Filter dependencies flag must be true.");

        DependencyGraphManager<String> dependencyGraphManager = new DependencyGraphManager<>();
        DependencyRefiner dependencyRefiner;

        long start = System.currentTimeMillis();

        if(Properties.BASELINE){

            logger.info("Computing complete graph since baseline flag is true");
            List<GraphNode<String>> nodes = new ArrayList<>();
            for (int i = 0; i < Properties.tests_order.length; i++) {
                nodes.add(new GraphNode<>(Properties.tests_order[i], i));
            }
            for (int i = 1; i < nodes.size(); i++) {
                addDependenciesTowards(nodes.subList(0, i), nodes.get(i), dependencyGraphManager);
            }

            dependencyRefiner = new DependencyRefiner(true);

        } else {

            logger.info("Computing graph with string analysis");
            TestCaseFinder testCaseFinder = new TestCaseFinder();
            List<CtClass<?>> testClasses = testCaseFinder.getTestCaseParsedRepresentation(
                    new File(Properties.getInstance().getProperty("tests_path")));
            testClasses.forEach(testClass -> logger.info("Test class: " + testClass.getSimpleName()));

            StringAnalysisDependencyGraphExtractor stringAnalysisDependencyGraphExtractor
                    = new StringAnalysisDependencyGraphExtractor(testClasses);
            dependencyGraphManager = stringAnalysisDependencyGraphExtractor.computeDependencies();

            dependencyRefiner = new DependencyRefiner();

        }

        long timeElapsed = System.currentTimeMillis() - start;
        logger.info("Time to compute the initial dependency graph: "
                + new ExecutionTime().computeExecutionTime(timeElapsed));

        GraphExporter<String> graphExporter = new GraphExporter<>(dependencyGraphManager.getDependencyGraph());
        graphExporter.export("dependency-graph-initial-compute-filter-refine");

        start = System.currentTimeMillis();
        FilterStrategy<String> filterStrategy = getFilterStrategy();
        Graph<GraphNode<String>, GraphEdge> graphFiltered = filterStrategy.filterDependencies(dependencyGraphManager.getDependencyGraph());

        timeElapsed = System.currentTimeMillis() - start;
        logger.info("Time to filter dependency graph: "
                + new ExecutionTime().computeExecutionTime(timeElapsed));

        graphExporter = new GraphExporter<>(graphFiltered);
        graphExporter.export("dependency-graph-filtered-compute-filter-refine");

        logger.info("Dependency graph after constant string analysis. Number of dependencies: "
                + dependencyGraphManager.getDependencyGraph().edgeSet().size());
        logger.info("Dependency graph after filtering dependencies. Number of dependencies: "
                + graphFiltered.edgeSet().size());
        logger.info("Difference: "
                + (dependencyGraphManager.getDependencyGraph().edgeSet().size() - graphFiltered.edgeSet().size()));

        if(!Properties.ONLY_COMPUTE_GRAPH_BUILD_TIME){

            try {

                dependencyRefiner.refine(graphFiltered);

                graphExporter = new GraphExporter<>(graphFiltered);
                graphExporter.export("dependency-graph-final-compute-filter-refine");

                if(Properties.RECOVER_MISSED_DEPENDENCIES){

                    long startTime = System.currentTimeMillis();
                    RecoverMissedDependencies recoverMissedDependencies
                            = new RecoverMissedDependencies(dependencyRefiner.getExecutionTimes(), graphFiltered);
                    graphFiltered = recoverMissedDependencies.computeMissedDependencies();

                    graphExporter = new GraphExporter<>(graphFiltered);
                    graphExporter.export("dependency-graph-initial-recover-missed");

                    int iterationId = dependencyRefiner.getIterationId();
                    dependencyRefiner = new DependencyRefiner(recoverMissedDependencies.getExecutionTimes(),
                            false, iterationId);
                    dependencyRefiner.refine(graphFiltered);
                    logger.info("Time spent in recovering missed dependencies: "
                            + new ExecutionTime()
                            .computeExecutionTime(Arrays.asList((System.currentTimeMillis() - startTime))));

                    graphExporter = new GraphExporter<>(graphFiltered);
                    graphExporter.export("dependency-graph-final-recover-missed");

                }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }



            // somehow refine hangs
            System.exit(0);
        }
    }

    private static void config() {
        instantiateProperties();
    }

    private static void instantiateProperties() {
        Properties.getInstance().createPropertiesFile();
    }

    private static FilterStrategy<String> getFilterStrategy() {
        String filterType = Properties.FILTER_TYPE;
        for (String typeName: FilterType.getValues()){
            if(typeName.equals(FilterType.Type.NLP.getTypeName()) && filterType.equals(typeName)){
                return new NlpFilterStrategy<>();
            } else if(typeName.equals(FilterType.Type.COMMON_VALUES.getTypeName()) && filterType.equals(typeName)){
                return new CommonValuesStrategy<>();
            }
        }
        throw new IllegalArgumentException("Unknown filter type " + filterType + ". See " + FilterType.class + " for reference.");
    }

    private static void addDependenciesTowards(List<GraphNode<String>> targetNodes,
                                               GraphNode<String> sourceNode,
                                               DependencyGraphManager<String> dependencyGraphManager){
        for (GraphNode<String> targetNode : targetNodes) {
            dependencyGraphManager.addDependency(sourceNode, targetNode,
                    new GraphEdge(new ArrayList<>(), sourceNode, targetNode));
        }
    }
}
