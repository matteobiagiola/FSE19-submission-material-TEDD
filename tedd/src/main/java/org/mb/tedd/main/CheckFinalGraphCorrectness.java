package org.mb.tedd.main;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.algorithm.execution.TestCaseExecutor;
import org.mb.tedd.algorithm.execution.TestResult;
import org.mb.tedd.algorithm.refinement.scheduling.InvalidScheduleException;
import org.mb.tedd.algorithm.refinement.scheduling.OriginalOrderScheduler;
import org.mb.tedd.algorithm.refinement.scheduling.RefinementScheduler;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.graph.GraphUtils;
import org.mb.tedd.graph.dot.importgraph.GraphImporter;
import org.mb.tedd.utils.Properties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class CheckFinalGraphCorrectness {

    private final static Logger logger = Logger.getLogger(CheckFinalGraphCorrectness.class.getName());

    /**
     * @implNote It checks if the final graph obtained with the runtime validation is correct
     */
    public static void main(String[] args) {

        config();

        Preconditions.checkArgument(Properties.CHECK_FINAL_GRAPH,
                "The check_final_graph property must be true.");
        Preconditions.checkArgument(new File(Properties.FINAL_GRAPH_PATH).exists(), "Graph "
                + Properties.FINAL_GRAPH_PATH + " does not exist.");

        TestCaseExecutor<String> testCaseExecutor = new TestCaseExecutor<>();
        Set<Set<GraphNode<String>>> schedules = new LinkedHashSet<>();
        List<Long> executionTimes = new ArrayList<>();
        List<Integer> scheduleLengths = new ArrayList<>();
        int falseNegatives = 0;
        int dependenciesRemovedBecauseOfCycle = 0;
        long maxExecutionTime = 0;
        Set<GraphNode<String>> maxTimeSchedule = new LinkedHashSet<>();
        long starTime = 0;
        long endTime = 0;

        int totalNumberOfSchedulesExecuted = 0;

        logger.info("Checking final graph: " + Properties.FINAL_GRAPH_PATH);

        GraphImporter graphImporter = new GraphImporter();
        Graph<GraphNode<String>, GraphEdge> graph = graphImporter.importGraph(Properties.FINAL_GRAPH_PATH);

        try {

            Set<GraphNode<String>> independentNodes = graph.vertexSet().stream()
                    .filter(graphNode -> graph.outDegreeOf(graphNode) == 0 && graph.inDegreeOf(graphNode) == 0)
                    .collect(Collectors.toSet());
            logger.info("Independent nodes: " + independentNodes);

            logger.info("Validating independent nodes...");
            for (GraphNode<String> independentNode : independentNodes) {
                Set<GraphNode<String>> schedule = new LinkedHashSet<>();
                schedule.add(independentNode);
                scheduleLengths.add(schedule.size());
                schedules.add(schedule);
            }

            totalNumberOfSchedulesExecuted = totalNumberOfSchedulesExecuted + schedules.size();

            for (Set<GraphNode<String>> schedule : schedules) {
                starTime = System.currentTimeMillis();
                Map<GraphNode<String>, TestResult> results = executeTests(schedule, testCaseExecutor);
                endTime = System.currentTimeMillis() - starTime;
                logger.info("Execution of schedule " + schedule + " took " + ((double) endTime / 1000) + " s");
                executionTimes.add(endTime);
                Optional<GraphNode<String>> testCaseThatFailedOptional = getFailureTestCase(results);
                if(testCaseThatFailedOptional.isPresent()){
                    String errorMessage = "Validation check failed!!! Test case "
                            + testCaseThatFailedOptional.get() + " failed on schedule " + schedule;
                    if(Properties.EXECUTE_WHOLE_TEST_SUITE){
                        throw new RuntimeException(errorMessage);
                    } else {
                        logger.warn(errorMessage);
                        falseNegatives++;
                    }
                }
            }

            schedules.clear();

            Set<GraphNode<String>> nodesOrder = GraphUtils.mapTestsOrderToNodesOrder(Arrays.asList(Properties.tests_order));
            RefinementScheduler<String> originalOrderScheduler = new OriginalOrderScheduler<>(nodesOrder);

            for (GraphEdge graphEdge : graph.edgeSet()) {
                try {
                    Set<GraphNode<String>> schedule = originalOrderScheduler.computeOriginalSchedule(graph, graphEdge);
                    scheduleLengths.add(schedule.size());
                    logger.info("Schedule with dep " + graphEdge + ": " + schedule);
                    schedules.add(schedule);
                } catch (InvalidScheduleException e) {
                    e.printStackTrace();
                    dependenciesRemovedBecauseOfCycle++;
                }
            }

            logger.info("Validating all the other paths...");

            totalNumberOfSchedulesExecuted = totalNumberOfSchedulesExecuted + schedules.size();

            for (Set<GraphNode<String>> schedule : schedules) {
                starTime = System.currentTimeMillis();
                Map<GraphNode<String>, TestResult> results = executeTests(schedule, testCaseExecutor);
                endTime = System.currentTimeMillis() - starTime;
                executionTimes.add(endTime);
                logger.info("Execution of schedule " + schedule + " took " + ((double) endTime / 1000) + " s");
                if(endTime > maxExecutionTime){
                    maxExecutionTime = endTime;
                    maxTimeSchedule = new LinkedHashSet<>(schedule);
                }
                Optional<GraphNode<String>> testCaseThatFailedOptional = getFailureTestCase(results);
                if(testCaseThatFailedOptional.isPresent()){
                    String errorMessage = "Validation check failed!!! Test case "
                            + testCaseThatFailedOptional.get() + " failed on schedule " + schedule;
                    if(Properties.EXECUTE_WHOLE_TEST_SUITE){
                        throw new RuntimeException(errorMessage);
                    } else {
                        logger.warn(errorMessage);
                        falseNegatives++;
                    }
                }
            }

            logger.info("=====================================");

            logger.info("Total number of schedules executed: " + totalNumberOfSchedulesExecuted + " (independent nodes " +
                    independentNodes.size() + "). Deps: " + graph.edgeSet().size());

            if(Properties.EXECUTE_WHOLE_TEST_SUITE){
                logger.info("Executing test suite in its default order...");
                starTime = System.currentTimeMillis();
                Map<GraphNode<String>, TestResult> results = executeTests(nodesOrder, testCaseExecutor);
                endTime = System.currentTimeMillis() - starTime;
                Optional<GraphNode<String>> testCaseThatFailedOptional = getFailureTestCase(results);
                if(testCaseThatFailedOptional.isPresent())
                    throw new IllegalStateException("Test suite is flaky!!! Test case "
                            + testCaseThatFailedOptional.get() + " failed on schedule " + nodesOrder);

                long runtimeOriginalTestSuiteRunOrder = endTime;
                double worstCaseSpeedUp = ((double) runtimeOriginalTestSuiteRunOrder / maxExecutionTime);
                logger.info("Execution of test suite in its default order took "
                        + ((double) runtimeOriginalTestSuiteRunOrder / 1000) + " s");
                logger.info("Schedule with the highest runtime of "
                        + ((double) maxExecutionTime / 1000) + " s: " + maxTimeSchedule);
                logger.info("Worst case speed up factor: " + worstCaseSpeedUp);

                double averageRuntimeSchedulesParallelized = ((double) executionTimes.stream().mapToLong(Long::new).sum()
                        / (double) executionTimes.size());
                double averageSpeedUp = ((double) runtimeOriginalTestSuiteRunOrder
                        / averageRuntimeSchedulesParallelized);
                logger.info("Average speed up factor: " + averageSpeedUp);

                double averageScheduleLengths = ((double) scheduleLengths.stream()
                        .mapToInt(Integer::new).sum() / scheduleLengths.size());
                logger.info("Average runtime schedules parallelized: "
                        + (averageRuntimeSchedulesParallelized / 1000) + " s");
                logger.info("Average schedule lengths: " + averageScheduleLengths);
            }

            logger.info("False negatives: " + falseNegatives);
            logger.info("Dependencies removed because of cycle: " + dependenciesRemovedBecauseOfCycle);
            logger.info("=====================================");

        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }

        // Somehow it hangs
        System.exit(0);

    }

    private static Optional<GraphNode<String>> getFailureTestCase(Map<GraphNode<String>, TestResult> results){
        for (GraphNode<String> graphNode : results.keySet()) {
            if(results.get(graphNode).equals(TestResult.FAIL))
                return Optional.of(graphNode);
        }
        return Optional.empty();
    }

    private static Map<GraphNode<String>, TestResult> executeTests(Set<GraphNode<String>> schedule,
                                                                   TestCaseExecutor<String> testCaseExecutor)
                                                                    throws InterruptedException,
                                                                           ExecutionException,
                                                                           IOException {
        return new HashMap<>(testCaseExecutor.executeTestsRemoteJUnitCore(schedule));
    }

    private static void config() {
        instantiateProperties();
    }

    private static void instantiateProperties() {
        Properties.getInstance().createPropertiesFile();
    }
}
