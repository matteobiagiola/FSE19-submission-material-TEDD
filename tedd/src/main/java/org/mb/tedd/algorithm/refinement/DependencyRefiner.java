package org.mb.tedd.algorithm.refinement;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.algorithm.execution.TestCaseExecutor;
import org.mb.tedd.algorithm.execution.TestResult;
import org.mb.tedd.algorithm.refinement.edgeselection.RandomStrategy;
import org.mb.tedd.algorithm.refinement.edgeselection.RefinementStrategies;
import org.mb.tedd.algorithm.refinement.edgeselection.RefinementStrategy;
import org.mb.tedd.algorithm.refinement.edgeselection.SimpleRandomStrategy;
import org.mb.tedd.algorithm.refinement.edgeselection.SinkFirstStrategy;
import org.mb.tedd.algorithm.refinement.edgeselection.SourceFirstStrategy;
import org.mb.tedd.algorithm.refinement.scheduling.InvalidScheduleException;
import org.mb.tedd.algorithm.refinement.scheduling.OriginalOrderScheduler;
import org.mb.tedd.algorithm.refinement.scheduling.RefinementScheduler;
import org.mb.tedd.algorithm.extraction.string.StringValue;
import org.mb.tedd.algorithm.extraction.string.ValueType;
import org.mb.tedd.graph.DependencyGraphManager;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphUtils;
import org.mb.tedd.graph.dot.exportgraph.GraphExporter;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.graph.utils.ComparatorNodesIncreasing;
import org.mb.tedd.utils.ExecutionTime;
import org.mb.tedd.utils.Properties;
import org.mb.tedd.utils.SetsUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class DependencyRefiner {

    private Map<GraphNode<String>, TestResult> expectedResults;
    private Set<GraphNode<String>> nodesOrder;
    private RefinementScheduler<String> refinementScheduler;
    private RefinementStrategy<String> refinementStrategy;
    private final static Logger logger = Logger.getLogger(DependencyRefiner.class.getName());
    private int iterationId;
    private ComparatorNodesIncreasing<String> comparatorNodesIncreasing = new ComparatorNodesIncreasing<>();
    private List<Long> executionTimes = new LinkedList<>();
    private boolean checkForMissing;

    private Multiset<GraphNode<String>> testCasesThatFailedWhileCheckingForMissing = HashMultiset.create();
    private Multiset<GraphEdge> dependenciesAddedWhileCheckingForMissing = HashMultiset.create();

    private GraphEdge previousDataDependencyUnderTest;
    private final static int NUMBER_OF_RETRIES_FLAKINESS = 3;
    private int counterFlakiness = 0;
    private List<Long> timeSpentOnFlakiness = new ArrayList<>();

    private Set<GraphEdge> uninterestingDependencies = new LinkedHashSet<>();

    private Set<GraphNode<String>> notFailingNodesWithoutOutgoingEdges
            = new LinkedHashSet<>();
    private Set<GraphEdge> falseDependencies = new LinkedHashSet<>();
    private boolean falsePositivesDetection = false;
    private Map<GraphEdge, Graph<GraphNode<String>, GraphEdge>>
            graphSnapshotsFalsePositivesRemoval = new LinkedHashMap<>();

    public DependencyRefiner(){
        List<String> testsOrder = Arrays.asList(Properties.tests_order);
        this.nodesOrder = GraphUtils.mapTestsOrderToNodesOrder(testsOrder);
        this.expectedResults = this.buildExpectedResults(this.nodesOrder);
        this.refinementScheduler = new OriginalOrderScheduler<>(this.nodesOrder);
        this.refinementStrategy = this.getRefinementStrategy();
        this.checkForMissing = !Properties.BASELINE && !Properties.GROUND_TRUTH_CHECK;
        this.iterationId = 0;
    }

    public DependencyRefiner(boolean checkForMissing){
        this();
        this.checkForMissing = checkForMissing;
    }

    public DependencyRefiner(List<Long> executionTimes, boolean checkForMissing, int iterationId){
        this();
        this.executionTimes = executionTimes;
        this.checkForMissing = checkForMissing;
        this.iterationId = iterationId;
    }

    /**
     * @param falsePositivesDetection needed for resetting all manifest dependencies and
     *                                to save a snapshot of the graph before the removal of
     *                                each false positive
     * */
    public DependencyRefiner(List<Long> executionTimes, boolean checkForMissing,
                             boolean falsePositivesDetection, int iterationId){
        this();
        this.executionTimes = executionTimes;
        this.checkForMissing = checkForMissing;
        this.falsePositivesDetection = falsePositivesDetection;
        this.iterationId = iterationId;
    }

    /**
     * @param notFailingIndependentNodes needed during fixed point computation
     *                                            after false positives are removed. It is used in
     *                                            while checking for flakiness. If the test that fails
     *                                            belongs to this set and it not the source of the dependency
     *                                            under test, then we do not update the counter
     *                                            for flakiness but we remove the dependency as false
     * */
    public DependencyRefiner(List<Long> executionTimes,
                             boolean checkForMissing,
                             int iterationId,
                             Set<GraphNode<String>> notFailingIndependentNodes) {
        this();
        this.executionTimes = executionTimes;
        this.checkForMissing = checkForMissing;
        this.iterationId = iterationId;
        this.notFailingNodesWithoutOutgoingEdges = notFailingIndependentNodes;
    }

    /**
     * @return the first test case with unexpected results, according to the default execution order
     * */
    private Optional<GraphNode<String>> getFailingTestCase(Map<GraphNode<String>, TestResult> results){
        List<GraphNode<String>> failingTestCases = new ArrayList<>();
        for (GraphNode<String> graphNode : results.keySet()) {
            if(!results.get(graphNode).equals(this.expectedResults.get(graphNode))){
                logger.debug("On node " + graphNode + " current result: " + results.get(graphNode)
                        + " != expected result: " + this.expectedResults.get(graphNode));
                // FIXME it may happen I guess -> I am not sure
//                Preconditions.checkState(graphNode.equals(dependencySourceNode)
//                                || graphNode.equals(dependencyTargetNode),
//                        "Graph node that failed " + graphNode
//                                + " is different from source and target of dependency under test "
//                                + dependencySourceNode + " -> " + dependencyTargetNode);
                failingTestCases.add(graphNode);
            }
        }
        if(failingTestCases.isEmpty())
            return Optional.empty();
        failingTestCases.sort(this.comparatorNodesIncreasing);
        return Optional.of(failingTestCases.get(0));
    }

    /*
     * Check actual results against expected results and discover if the
     * dependency is manifest, that is: changing the preconditions set by A test
     * (target) to another B (source) will cause the failure of the latter.
     *
     * NOTE: Accumulating evidences that other data deps might be there is
     * out-of-scope of this method.
     *
     * TODO Check that the original Data Dep is NOT inverted !!!
     */
    private ManifestResult<String> isManifestDependency(GraphNode<String> dependencySourceNode,
                                                        GraphNode<String> dependencyTargetNode,
                                                        Map<GraphNode<String>, TestResult> results) {

        boolean isManifest = false;
        GraphNode<String> testWithUnexpectedResult = null;

        logger.info("Checking if " + dependencySourceNode + " -> " + dependencyTargetNode + " is manifest...");

        String testWithViolatedPreconditions = dependencySourceNode.getTestCase();
        String testThatSetsThePreconditions = dependencyTargetNode.getTestCase();

        logger.debug("Test with violated preconditions: " + testWithViolatedPreconditions);
        logger.debug("Test that sets the preconditions: " + testThatSetsThePreconditions);

        // TODO Accumulate the result also for the other tests in the schedule,
        // as they might suggest
        // missed data dependencies ! Why this should be the case if we respect
        // all the preconditions ?!
        for (GraphNode<String> graphNode : results.keySet()) {
            logger.debug("GraphNode test case: " + graphNode.getTestCase());
            if (testWithViolatedPreconditions.equals(graphNode.getTestCase())) { // Source is the test out of order !
                logger.debug(testWithViolatedPreconditions + "=" + graphNode.getTestCase() + ". Current result: "
                        + results.get(graphNode) + ", expected: " + this.expectedResults.get(graphNode));
                if (!results.get(graphNode).equals(this.expectedResults.get(graphNode))) {
                    logger.info("\t * " + graphNode + " Different (SOURCE), dependency "
                            + dependencySourceNode + " -> " + dependencyTargetNode + " is manifest.");
                    isManifest = true;
                    testWithUnexpectedResult = graphNode;
                }
            } else if (testThatSetsThePreconditions.equals(graphNode.getTestCase())) {
                logger.debug(testThatSetsThePreconditions + "=" + graphNode.getTestCase() + ". Current result: "
                        + results.get(graphNode) + ", expected: " + this.expectedResults.get(graphNode));
                if (!results.get(graphNode).equals(this.expectedResults.get(graphNode))) {
                    logger.debug("\t * " + graphNode + " Different (TARGET - SKIP)");
                }
            } else {
                logger.debug(testThatSetsThePreconditions + "!=" + graphNode.getTestCase() + ". \n Current result: "
                        + results.get(graphNode) + ", expected: " + this.expectedResults.get(graphNode));
                if (!results.get(graphNode).equals(this.expectedResults.get(graphNode))) {
                    logger.debug("\t * " + graphNode + " Different (OTHER - SKIP)");
                }
            }

        }

        return new ManifestResult<>(isManifest, testWithUnexpectedResult);
    }

    /**
     * @implNote it modifies the input graph by removing edges; it does not take dependency values into account
     * Manifest dependencies can be extracted from the dependencyGraph.
     * */
    public void refine(Graph<GraphNode<String>, GraphEdge> dependencyGraph) throws InterruptedException, ExecutionException, IOException {

        this.falseDependencies = new LinkedHashSet<>(); // reset set of false dependencies every time refine is called
        if(this.falsePositivesDetection){
            // let's pretend that dependencies in the graph are not manifest and let's run the dependency
            // refinement algorithm again to spot the false positives (dependencies removed)
            for (GraphEdge graphEdge : dependencyGraph.edgeSet()) {
                graphEdge.setManifest(false);
            }
        }

        List<Integer> testCounts = new LinkedList<>();
        ExecutionTime executionTime = new ExecutionTime();
        int totalNumberOfDependencies = dependencyGraph.edgeSet().size();

        GraphExporter<String> graphExporter = new GraphExporter<>(dependencyGraph);
        TestCaseExecutor<String> testCaseExecutor = new TestCaseExecutor<>();
        Set<GraphEdge> manifestDependencies = new LinkedHashSet<>();
        Set<GraphEdge> untestableDependencies = new LinkedHashSet<>();

        // Select a data dependency to test - this must not introduce cycle
        Optional<GraphEdge> dataDependencyUnderTestOptional = this.refinementStrategy.selectEdge(dependencyGraph);

        while(dataDependencyUnderTestOptional.isPresent()){

            // This might be over-approximate because some dependencies cannot be
            // tested with our approach - they will be reported as Untestable.
            logger.info("Total iteration count: " + totalNumberOfDependencies);
            logger.info("Iteration " + this.iterationId + " time elapsed: " + executionTime.computeExecutionTime(this.executionTimes));

            GraphEdge dataDependencyUnderTest = dataDependencyUnderTestOptional.get();
            logger.info("Dependency under test " + dataDependencyUnderTest + ". Iteration id: " + this.iterationId);
            dataDependencyUnderTest.setUnderTest(true);

            // Compute a schedule to validate/test the data dependency. This
            // raises an exception if the schedule cannot be found. For example,
            // random policy violates explicitly some conditions and uses ignore
            // to plan the schedule... however, OriginalOrderSchedule prevents
            // that by design.
            // So we simply SKIP this specific edge
            Set<GraphNode<String>> schedule;

            try {
                // it inverts dependencyUnderTest (DUT) -> try to execute graphSourceNode of DUT before graphTargetNode
                // that means try to see if graphSourceNode really needs graphTargetNode to run properly
                schedule = this.refinementScheduler.computeSchedule(dependencyGraph, dataDependencyUnderTest);
                logger.info("Schedule is: " + schedule + ". Iteration id: " + this.iterationId);

                // 4. Execute the tests
                Map<GraphNode<String>, TestResult> results =
                        this.executeTests(schedule,testCaseExecutor,testCounts,this.executionTimes);

                // 5. Check if there is any difference compared with reference output
                // A dependency is manifest only if there is a mismatch between the expected
                // result and the source of the dependency
                ManifestResult<String> manifestResult = this.isManifestDependency(
                        dependencyGraph.getEdgeSource(dataDependencyUnderTest),
                        dependencyGraph.getEdgeTarget(dataDependencyUnderTest), results);

                Optional<GraphNode<String>> testCaseThatFailedOptional = this.getFailingTestCase(results);

                if (manifestResult.isManifest() || testCaseThatFailedOptional.isPresent()) {

                    if(this.checkForMissing){

                        Set<GraphNode<String>> testCasesThatFailedMoreThanOnce = new LinkedHashSet<>();
                        for (GraphNode<String> testCaseThatFailedWhileCheckingForMissing : this.testCasesThatFailedWhileCheckingForMissing) {
                            if(this.testCasesThatFailedWhileCheckingForMissing.count(testCaseThatFailedWhileCheckingForMissing) > 1){
                                logger.warn("[Flakiness] Test case " + testCaseThatFailedWhileCheckingForMissing + " failed "
                                        + this.testCasesThatFailedWhileCheckingForMissing.count(testCaseThatFailedWhileCheckingForMissing)
                                        + " times!");
                                testCasesThatFailedMoreThanOnce.add(testCaseThatFailedWhileCheckingForMissing);
                            }
                        }
                        Set<GraphEdge> dependenciesAddedMoreThanOnce = new LinkedHashSet<>();
                        for (GraphEdge dependencyAddedWhileCheckingForMissing : this.dependenciesAddedWhileCheckingForMissing) {
                            if(this.dependenciesAddedWhileCheckingForMissing.count(dependencyAddedWhileCheckingForMissing) > 1){
                                logger.warn("[Flakiness] Dependency " + dependencyAddedWhileCheckingForMissing + " added "
                                        + this.dependenciesAddedWhileCheckingForMissing.count(dependencyAddedWhileCheckingForMissing)
                                        + " times!");
                                dependenciesAddedMoreThanOnce.add(dependencyAddedWhileCheckingForMissing);
                            }
                        }

                        if(!testCasesThatFailedMoreThanOnce.isEmpty() && !dependenciesAddedMoreThanOnce.isEmpty()){
                            throw new IllegalStateException("[Flakiness] Test suite is flaky! The following test cases failed more than once: "
                                    + testCasesThatFailedMoreThanOnce + "; check the relative stack trace on the logs. The following dependencies were added more than once: "
                                    + dependenciesAddedMoreThanOnce);
                        }

                        if(manifestResult.isManifest()){
                            this.checkMissingDependencies(schedule, dependencyGraph,
                                    dataDependencyUnderTest, testCaseExecutor, testCounts,
                                    this.executionTimes);
                        } else if(testCaseThatFailedOptional.isPresent()){
                            // at this point we are sure that there is a missing dependency
                            // we just have to compute the preconditions and add them to the graph
                            this.addMissingDependencies(testCaseThatFailedOptional.get(), schedule, dataDependencyUnderTest, dependencyGraph);
                            this.testCasesThatFailedWhileCheckingForMissing.add(testCaseThatFailedOptional.get());
                        }

                    } else {
                        if(manifestResult.isManifest()){
                            logger.info("Found Manifest Dependency: " + dataDependencyUnderTest + ". Iteration id: "
                                    + this.iterationId + " \n");
                            // TODO Double check if tests are flaky
                            dataDependencyUnderTest.setManifest(true);
                        } else if(!this.notFailingNodesWithoutOutgoingEdges
                                    .contains(testCaseThatFailedOptional.get())) {

                            // check flakiness
                            if(this.previousDataDependencyUnderTest == null){
                                this.previousDataDependencyUnderTest = dataDependencyUnderTest;
                                this.counterFlakiness = 1;
                                this.timeSpentOnFlakiness.add(this.executionTimes.get(this.executionTimes.size() - 1));
                                logger.info("[Flakiness] Time spent on flakiness (previous = null): " + new ExecutionTime().computeExecutionTime(this.timeSpentOnFlakiness)
                                        + ". Counter " + this.counterFlakiness + "/" + NUMBER_OF_RETRIES_FLAKINESS);
                            } else if(this.previousDataDependencyUnderTest.equals(dataDependencyUnderTest)){
                                this.timeSpentOnFlakiness.add(this.executionTimes.get(this.executionTimes.size() - 1));
                                this.counterFlakiness++;
                                logger.info("[Flakiness] Time spent on flakiness (previous = equals): " + new ExecutionTime().computeExecutionTime(this.timeSpentOnFlakiness)
                                        + ". Counter " + this.counterFlakiness + "/" + NUMBER_OF_RETRIES_FLAKINESS);
                                if(this.counterFlakiness == NUMBER_OF_RETRIES_FLAKINESS){
                                    throw new IllegalStateException("Error during refinement: " + dataDependencyUnderTest
                                            + " is not manifest but " + testCaseThatFailedOptional.get()
                                            + " fails in schedule " + schedule);
                                }
                            } else {
                                logger.info("Reset previous data dependency for flakiness check");
                                this.previousDataDependencyUnderTest = null;
                                this.counterFlakiness = 0;
                            }

                        } else {

                            logger.info("Test case " + testCaseThatFailedOptional.get() +
                                    " failed while inverting the dependency " + dataDependencyUnderTest
                                    + " but it belongs to nodes without outgoing edges that do not fail if "
                                    + " executed independently -> " + this.notFailingNodesWithoutOutgoingEdges);

                            dataDependencyUnderTest.setManifest(false);

                            if (!dependencyGraph.removeEdge(dataDependencyUnderTest)) {
                                logger.warn("Edge " + dataDependencyUnderTest + " NOT REMOVED"
                                        + ". Iteration id: " + this.iterationId);
                            } else {
                                this.falseDependencies.add(dataDependencyUnderTest);
                                logger.info("Removing edge " + dataDependencyUnderTest
                                        + " because it does not represent a manifest dependency" + ". Iteration id: "
                                        + this.iterationId);
                            }
                        }
                    }

                } else {

                    dataDependencyUnderTest.setManifest(false);

                    if(this.falsePositivesDetection){
                        logger.info("Saving graph snapshot before removing dep "
                                + dataDependencyUnderTest);
                        Graph<GraphNode<String>, GraphEdge> graphSnapshot =
                                new DependencyGraphManager(dependencyGraph).duplicate();
                        this.graphSnapshotsFalsePositivesRemoval.put(dataDependencyUnderTest, graphSnapshot);
                    }

                    if (!dependencyGraph.removeEdge(dataDependencyUnderTest)) {
                        logger.warn("Edge " + dataDependencyUnderTest + " NOT REMOVED"
                                + ". Iteration id: " + this.iterationId);
                    } else {
                        this.falseDependencies.add(dataDependencyUnderTest);
                        logger.info("Removing edge " + dataDependencyUnderTest
                                + " because it does not represent a manifest dependency" + ". Iteration id: "
                                + this.iterationId);
                    }
                }

                dataDependencyUnderTest.setUnderTest(false);

            } catch (InvalidScheduleException ise) {
                //
                logger.info("Iteration skipped ! " + dataDependencyUnderTest + " creates a cycle !"
                        + ". Iteration id: " + this.iterationId);
                // Not sure this is needed. If the schedule was not valid was
                // because of random.
                dataDependencyUnderTest.setIntroducesCycle(true);
            }

            if(Properties.PRINT_INTERMEDIATE_GRAPHS)
                graphExporter.export("dependency-graph-" + this.iterationId);

            // Re-sample the "Updated" graph
            dataDependencyUnderTestOptional = this.refinementStrategy.selectEdge(dependencyGraph);

            this.iterationId++;
        }

        for (GraphEdge graphEdge : dependencyGraph.edgeSet()) {
            if (graphEdge.isManifest()) {
                manifestDependencies.add(graphEdge);
            } else {
                untestableDependencies.add(graphEdge);
            }
        }

        this.uninterestingDependencies = this
                .filterUninterestingDependencies(dependencyGraph, untestableDependencies);

        this.computeStats(dependencyGraph, testCounts, totalNumberOfDependencies, manifestDependencies);
    }

    private Set<GraphEdge> filterUninterestingDependencies(Graph<GraphNode<String>, GraphEdge> dependencyGraph,
                                                 Set<GraphEdge> untestableDependencies){
        DependencyFilterer dependencyFilterer = new DependencyFilterer();
        Set<GraphEdge> uninterestingDependencies = new LinkedHashSet<>();
        if (untestableDependencies.size() > 0) {
            logger.info("Untestable dependencies size: " + untestableDependencies.size() + " at iteration "
                    + this.iterationId + ". Continue refinement.");
            uninterestingDependencies = dependencyFilterer
                    .filterUninterestingDependencies(dependencyGraph, true);
            if (uninterestingDependencies.size() == untestableDependencies.size()) {
                // this should be the default case
                logger.info("Number of uninteresting dependencies removed = Number of untestable dependencies. Stop refinement at iteration "
                        + this.iterationId);
                logger.info("Number of uninteresting/implicit dependencies: " + uninterestingDependencies.size());
            } else {
                throw new RuntimeException("Refinement algorithm has a bug. At this point all the untestable dependencies " +
                        "must be uninteresting. Untestable deps size: " + untestableDependencies.size() + ", " +
                        "Uninteresting size: " + uninterestingDependencies.size());
            }
        } else {
            logger.info("No untestable dependencies.");
        }
        return uninterestingDependencies;
    }

    private void computeStats(Graph<GraphNode<String>,GraphEdge> dependencyGraph,
                              List<Integer> testCounts,
                              int totalNumberOfDependencies,
                              Set<GraphEdge> manifestDependencies){
        int totalTests = 0;
        for (int n : testCounts) {
            totalTests += n;
        }
        long totalExecTime = 0;
        for (long l : this.executionTimes) {
            totalExecTime += l;
        }
        float totalFloat = ((float) totalExecTime / 1000);

        logger.info("Finished after " + this.iterationId + " iterations");
        if (this.iterationId > 0) {
            logger.info("Executed " + totalTests + " tests in total, avg.: " + (totalTests / this.iterationId));
            if(totalFloat > 60.0){
                if(totalFloat > 3600.0)
                    logger.info("Spent " + totalFloat/3600.0 + " h executing tests, avg.: "
                            + (totalFloat / this.iterationId) + "s per refinement step");
                else
                    logger.info("Spent " + totalFloat/60.0 + " min executing tests, avg.: "
                            + (totalFloat / this.iterationId) + "s per refinement step");
            } else {
                logger.info("Spent " + totalFloat + " seconds executing tests, avg.: "
                        + (totalFloat / this.iterationId) + "s per refinement step");
            }

        }
        // EASE THE PARSING
        logger.info("==================");
        logger.info("Number of tests: " + this.expectedResults.size());
        logger.info("Number of dependencies: " + totalNumberOfDependencies);
        logger.info("Number of dependencies added while checking for missing: " + this.dependenciesAddedWhileCheckingForMissing.size());
        logger.info("Number of false dependencies: " + this.falseDependencies.size());
        logger.info("Number of manifest dependencies: " + manifestDependencies.size());
        int numberOfMissingDependencies = this.getMissingDependencies(dependencyGraph);
        logger.info("Number of missing dependencies recovered: " + numberOfMissingDependencies);
        logger.info("Executions: " + totalTests);
        if (this.iterationId != 0) {
            logger.info("Avg executions: " + (totalTests / this.iterationId));
        }
        logger.info("Max executions: " + this.expectedResults.size() * totalNumberOfDependencies);
        if(totalFloat > 60.0){
            if(totalFloat > 3600)
                logger.info("Time: " + totalFloat/3600.0 + " h");
            else
                logger.info("Time: " + totalFloat/60.0 + " min");
        }
        else
            logger.info("Time: " + totalFloat + "s");
        if (this.iterationId != 0) {
            logger.info("Avg time: " + (totalFloat / this.iterationId) + "s");
        }
        logger.info("==================");
        if(Properties.GROUND_TRUTH_CHECK && this.falseDependencies.size() > 0){
            // print false dependencies
            String toPrint = this.falseDependencies.stream().map(GraphEdge::toString).collect(Collectors.joining(","));
            logger.info("False dependencies filtered while checking ground truth graph: " + toPrint);
        }
    }

    private void checkMissingDependencies(Set<GraphNode<String>> schedule,
                                          Graph<GraphNode<String>, GraphEdge> dependencyGraph,
                                          GraphEdge dataDependencyUnderTest,
                                          TestCaseExecutor<String> testCaseExecutor,
                                          List<Integer> testCounts,
                                          List<Long> executionTimes) throws InterruptedException, ExecutionException, IOException, InvalidScheduleException {
        // check that the failure is not due to a missing dependency. If the schedule fails even if
        // the dependency is not inverted then there is a missing dependency that the developer has to
        // insert manually
        logger.info("Checking if there is any missing dependency causing execution to fail.");

        // execution failed while executing graphSourceNode of DUT before graphTargetNode. This means
        // either that DUT is manifest or that the failure is due to a missing dependency. In order to
        // distinguish these two cases try to see if the execution fails even if we execute graphTargetNode
        // of DUT before graphSourceNode. If it does then there is a missing dependency, otherwise DUT is a manifest dependency.
        // the original schedule respects the preconditions of graphTargetNode so it is safe to put it
        // before graphSourceNode in the following schedule
        Set<GraphNode<String>> scheduleCheckMissing = this.refinementScheduler
                .computeOriginalSchedule(dependencyGraph,dataDependencyUnderTest);
        logger.info("Schedule while testing " + dataDependencyUnderTest
                + " for a missing dependency: " + scheduleCheckMissing);

        Map<GraphNode<String>, TestResult> resultsCheckMissing =
                this.executeTests(scheduleCheckMissing, testCaseExecutor, testCounts, executionTimes);
        Optional<GraphNode<String>> failingTestCaseOptional = this.getFailingTestCase(resultsCheckMissing);

        if(failingTestCaseOptional.isPresent()){
            this.testCasesThatFailedWhileCheckingForMissing.add(failingTestCaseOptional.get());
            this.addMissingDependencies(failingTestCaseOptional.get(), schedule,
                    dataDependencyUnderTest, dependencyGraph);
        } else {
            logger.info("NO failure occurred while testing " + dataDependencyUnderTest
                    + " for a missing dependency");
            logger.info("Found Manifest Dependency: " + dataDependencyUnderTest + ". Iteration id: "
                    + this.iterationId + " \n");
            // TODO Double check if tests are flaky
            dataDependencyUnderTest.setManifest(true);
            logger.info("Schedule of manifest dependency " + dataDependencyUnderTest + ": " + scheduleCheckMissing);
        }
    }

    private void addMissingDependencies(GraphNode<String> testCaseWithUnexpectedResult,
                                        Set<GraphNode<String>> schedule,
                                        GraphEdge dataDependencyUnderTest,
                                        Graph<GraphNode<String>, GraphEdge> dependencyGraph){
        Set<GraphNode<String>> candidatePreconditions =
                SetsUtils.setsDifference(this.nodesOrder, schedule).stream()
                        .filter(candidatePrecondition -> candidatePrecondition.getNumOrder()
                                < testCaseWithUnexpectedResult.getNumOrder())
                        .collect(Collectors.toSet());
        // I guess here we are assuming that the graph is weakly connected
        Preconditions.checkState(!candidatePreconditions.isEmpty(),
                "There must be at least one candidate precondition!");
        if(candidatePreconditions.size() > 1){
            logger.info("FAILURE occurred while testing " + dataDependencyUnderTest
                    + " for a missing dependency. There must be" +
                    " some missing dependency in this schedule " + schedule +
                    " that leads test " + testCaseWithUnexpectedResult + " to failure." +
                    " Test case " + testCaseWithUnexpectedResult + " probably depends on one of the" +
                    " following test cases: " + candidatePreconditions);
            if(Properties.ADD_MISSING_DEPS_MANUALLY){
                logger.info("Add target node of a missing dependency manually...");
                GraphEdge edgeAdded = GraphUtils.addEdgeToGraphManually(dependencyGraph,
                        candidatePreconditions, testCaseWithUnexpectedResult);
                logger.info("Dependency " + edgeAdded + " successfully added to the graph!");
            } else {
                logger.info("Add missing dependencies automatically.");

                // Two implementations possible. 1. Add as many edges as the number of candidate
                // preconditions failingTestCase -> candidatePrecondition and let the algorithm
                // validate these dependencies. 2. Add all the preconditions in the schedule before
                // the failingTestCase and remove the unnecessary ones

                for (GraphNode<String> candidatePrecondition : candidatePreconditions) {
                    Optional<GraphEdge> edgeOptional = GraphUtils.addEdgeToGraphAutomatically(dependencyGraph, testCaseWithUnexpectedResult,
                            candidatePrecondition, false);
                    if(edgeOptional.isPresent()){
                        logger.info("Dependency " + edgeOptional.get() + " added to the graph. Needs to be validated");
                        this.dependenciesAddedWhileCheckingForMissing.add(edgeOptional.get());
                    }
                }
            }
        } else {
            logger.info("FAILURE occurred while testing " + dataDependencyUnderTest
                    + " for a missing dependency. There must be" +
                    " some missing dependency in this schedule " + schedule +
                    " that leads test " + testCaseWithUnexpectedResult + " to failure." +
                    " Test case " + testCaseWithUnexpectedResult + " depends on test case " +
                    candidatePreconditions.toArray()[0]);
            logger.info("Adding edge " + testCaseWithUnexpectedResult + "->" +
                    candidatePreconditions.toArray()[0]);
            Optional<GraphEdge> edgeOptional = GraphUtils.addEdgeToGraphAutomatically(dependencyGraph,
                    testCaseWithUnexpectedResult,
                    (GraphNode<String>) candidatePreconditions.toArray()[0], false);
            if(edgeOptional.isPresent()){
                logger.info("Dependency " + edgeOptional.get() + " added to the graph. It is already validated.");
                this.dependenciesAddedWhileCheckingForMissing.add(edgeOptional.get());
            } else {
                throw new RuntimeException("Edge " + testCaseWithUnexpectedResult + "->"
                        + candidatePreconditions.toArray()[0] + " must not exist in the dependency graph but it does!!");
            }
        }
    }

    /**
     * @implNote for now we assume that all test cases if executed in the correct order will pass.
     * We don't actually execute the entire test suite in the correct order to check it.
     * */
    private Map<GraphNode<String>, TestResult> buildExpectedResults(Set<GraphNode<String>> nodesOrder){
        Map<GraphNode<String>, TestResult> expectedResults = new HashMap<>();
        for(GraphNode<String> graphNode: nodesOrder){
            expectedResults.put(graphNode, TestResult.PASS);
        }
        return expectedResults;
    }

    private RefinementStrategy<String> getRefinementStrategy() {
        String refinementStrategy = Properties.REFINEMENT_STRATEGY;
        for (String strategy: RefinementStrategies.getValues()){
            if(strategy.equals(RefinementStrategies.Strategy.RANDOM.getStrategyName()) && refinementStrategy.equals(strategy)){
                return new RandomStrategy<>();
            } else if(strategy.equals(RefinementStrategies.Strategy.SIMPLE_RANDOM.getStrategyName()) && refinementStrategy.equals(strategy)){
                return new SimpleRandomStrategy<>();
            } else if(strategy.equals(RefinementStrategies.Strategy.SINK_FIRST.getStrategyName()) && refinementStrategy.equals(strategy)){
                return new SinkFirstStrategy<>();
            } else if(strategy.equals(RefinementStrategies.Strategy.SOURCE_FIRST.getStrategyName()) && refinementStrategy.equals(strategy)){
                return new SourceFirstStrategy<>();
            }
        }
        throw new IllegalArgumentException("Unknown refinement strategy " + refinementStrategy + ". See " + RefinementStrategies.class + " for reference.");
    }

    private Map<GraphNode<String>, TestResult> executeTests(Set<GraphNode<String>> schedule, TestCaseExecutor<String> testCaseExecutor,
                                                            List<Integer> testCounts, List<Long> executionTimes) throws InterruptedException, ExecutionException, IOException {
        long start = System.currentTimeMillis();
        logger.debug("Start test execution at: " + start);

        Map<GraphNode<String>, TestResult> results = new HashMap<>(testCaseExecutor
                .executeTestsRemoteJUnitCore(schedule));

        logger.debug("Stop test execution at: " + (System.currentTimeMillis() - start));
        executionTimes.add(System.currentTimeMillis() - start);
        testCounts.add(results.size());

        return results;
    }

    private int getMissingDependencies(Graph<GraphNode<String>, GraphEdge> dependencyGraph){
        int missingDependenciesCount = 0;
        Set<GraphEdge> edgeSet = dependencyGraph.edgeSet();
        for (GraphEdge graphEdge : edgeSet) {
            Collection<StringValue> values = graphEdge.getDependentValues();
            for (StringValue value : values) {
                if(value.getValueType().equals(ValueType.CONSTANT)){
                    List<String> constantValues = value.getValues();
                    boolean match = constantValues.stream()
                            .anyMatch(constantValue -> constantValue.contains(GraphUtils.missingDepPrefix)
                                    && graphEdge.isManifest());
                    if(match){
                        // missing dep is counted only once for each edge
                        missingDependenciesCount++;
                        break;
                    }
                }
            }
        }
        return missingDependenciesCount;
    }

    public List<Long> getExecutionTimes() {
        return new ArrayList<>(executionTimes);
    }

    public int getIterationId() {
        return iterationId;
    }

    public Set<GraphEdge> getFalseDependencies() {
        return this.falseDependencies;
    }

    public Set<GraphEdge> getUninterestingDependencies() {
        return uninterestingDependencies;
    }

    public Map<GraphEdge, Graph<GraphNode<String>, GraphEdge>> getGraphSnapshotsFalsePositivesRemoval() {
        return graphSnapshotsFalsePositivesRemoval;
    }
}
