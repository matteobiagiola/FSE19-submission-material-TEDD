package org.mb.tedd.algorithm.execution;

import org.apache.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TestCaseExecutionListener extends RunListener {

    private final static Logger logger = Logger.getLogger(TestCaseExecutionListener.class.getName());
    private Set<Description> runOrder = new HashSet<>();
    private List<Integer> staticallyFailedTests;
    private Map<Integer, String> staticallyFailedTestTraces;
    private int testID = 0;

    public TestCaseExecutionListener(List<Integer> staticallyFailedTests,
                                     Map<Integer, String> staticallyFailedTestTraces){
        this.staticallyFailedTests = staticallyFailedTests;
        this.staticallyFailedTestTraces = staticallyFailedTestTraces;
    }

    @Override
    public void testStarted(Description description) {
        // Descriptions are unique per test execution
        runOrder.add(description);
        testID++;
    }

    @Override
    public void testFailure(Failure failure) {
        if (!runOrder.contains(failure.getDescription())
                && failure.getDescription().getMethodName() == null) {
            this.staticallyFailedTests.add(testID);
            this.staticallyFailedTestTraces.put(testID, failure.getTrace());
            testID++; // Start is not invoked, we need to force
            // this
        }
    }

    public List<Integer> getStaticallyFailedTests() {
        return staticallyFailedTests;
    }

    public Map<Integer, String> getStaticallyFailedTestTraces() {
        return staticallyFailedTestTraces;
    }
}
