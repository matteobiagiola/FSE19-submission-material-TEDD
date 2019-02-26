package org.mb.tedd.algorithm.execution;

import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.log4j.Logger;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * This class wraps JUnitCore to execute the provided tests in order and publish
 * results using sockets using a listener.
 *
 * The execution goes as follows: - we check if the schedule is the result of an
 * already parallelized execution by checking that at the level of TestClasses
 * we have a dependency cycle. Or in other words, that we have situations like:
 * T1.m1, T(!=1).m*, T1.m(!=1). In this case we parallelize at method level.
 *
 * Otherwise, we use a conservative solution and parallelize at class level.
 *
 *
 * TODO: Can we redirect test output to file ? TODO: Can we return the Failure message ?
 */
public class RemoteJUnitCore {

    private interface ParsingInterface {

        @Option(longName = { "port" })
        Integer getPort();

        @Option(longName = { "mvn-package-structure" }, defaultValue = "none")
        String getMvnPackageStructure();

        @Option(longName = { "test-list" }, defaultToNull = true)
        List<String> getTestList();

        @Option(longName = { "tests-path" }, defaultValue = "none")
        String getTestsPath();

        @Option(longName = { "reset-qualified-class-name" }, defaultValue = "none")
        String getQualifiedResetClassName();

        @Option(longName = { "reset-method-name" }, defaultValue = "none")
        String getResetMethodName();
    }

    private final static Logger logger = Logger.getLogger(RemoteJUnitCore.class.getName());

    // By default we exit with error if input is wrong !
    public static void main(String... args) {

        ParsingInterface cli = CliFactory.parseArguments(ParsingInterface.class, args);
        try {

            try (Socket socket = new Socket("localhost", cli.getPort());
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                         new BufferedOutputStream(socket.getOutputStream()))) {

                JUnitCore core = new JUnitCore();

                String testsPath = cli.getTestsPath();
                String mvnPackageStructure = cli.getMvnPackageStructure();
                String projectPackageStructure;

                if(!testsPath.equals("none") && !mvnPackageStructure.equals("none")){
                    int indexOfMvnPackageStructure = testsPath.indexOf(mvnPackageStructure);
                    int charactersInMvnPackageStructure = mvnPackageStructure.length();
                    projectPackageStructure = testsPath.substring(indexOfMvnPackageStructure
                            + charactersInMvnPackageStructure + 1, testsPath.length()); // +1 is the '/' character
                } else {
                    throw new RuntimeException("Please specify the tests path and maven package structure");
                }

                List<String> testList = new ArrayList<>(cli.getTestList());

                // Add Statically Failing Test listener
                List<Integer> staticallyFailedTests = new ArrayList<>();
                Map<Integer, String> staticallyFailedTestTraces = new HashMap<>();

                TestCaseExecutionListener staticallyFailingTestsListener = new TestCaseExecutionListener(staticallyFailedTests, staticallyFailedTestTraces);
                core.addListener(staticallyFailingTestsListener);

                staticallyFailedTests = staticallyFailingTestsListener.getStaticallyFailedTests();
                staticallyFailedTestTraces = staticallyFailingTestsListener.getStaticallyFailedTestTraces();

//                logger.debug("Tests to execute " + testList.size() + " ");

                /*
                 * https://stackoverflow.com/questions/41261889/is-there-a- way-
                 * to-disable-org-junit-runner-junitcores-stdout-output Result
                 * result = new JUnitCore().runMain(new NoopPrintStreamSystem(),
                 * args); System.exit(result.wasSuccessful() ? 0 : 1);
                 */
//                logger.debug("Start of test execution ");

                // reset app state before each scheduled execution
                String resetClassName = cli.getQualifiedResetClassName();
                String resetMethodName = cli.getResetMethodName();
                if(!resetClassName.equals("none") && !resetMethodName.equals("none")){
                    Class clazz = Class.forName(resetClassName);
                    Object obj = clazz.newInstance();
                    Method method = clazz.getDeclaredMethod(resetMethodName);
                    method.invoke(obj);
                } else {
                    throw new RuntimeException("Please specify resetClassName and resetMethodName");
                }


                long startTime = System.currentTimeMillis();

                // TODO: Check if the problem is when the request is created,
                // maybe we need to defer the request creation till the very
                // last second (stream?)
                List<Result> results = new ArrayList<>();

                for (String test: testList){
                    Result r = core.run(Class.forName(getTestCaseClassName(test, projectPackageStructure)));
                    results.add(r);
                }
                long executionTime = System.currentTimeMillis() - startTime;

//                logger.info("End of test execution ");
//                logger.info("Test execution took: " + executionTime);

                // Merge Results:
                int runCount = 0;
                int failureCount = 0;
                int ignoreCount = 0;
                long runTime = 0;
                List<Failure> failures = new ArrayList<>();

                for (Result result : results) {
                    runCount = runCount + result.getRunCount();
                    failureCount = failureCount + result.getFailureCount();
                    ignoreCount = ignoreCount + result.getIgnoreCount();
                    runTime = runTime + result.getRunTime();
                    failures.addAll(result.getFailures());
                }

                logger.info("Run " + runCount + " + " + staticallyFailedTests.size());
                logger.info("Failed " + failureCount);
                logger.info("Ignored " + ignoreCount);

                // We need to remove the reference to test classes here, just
                // names and test method shall remain

                // We need only to pass the list of failed for the moment
                // objectOutputStream.writeObject(result);
                objectOutputStream.writeObject(runTime);
                // Patch to include statically failing tests
                objectOutputStream.writeObject(runCount + staticallyFailedTests.size());
                //
                objectOutputStream.writeObject(ignoreCount);
                // Failure count includes already staticallyFailingTests but not
                // assumption failed tests
                objectOutputStream.writeObject(failureCount);
                // To deserialize Description we need the actual class that we
                // do not have on the other side... so we
                // pass the string version of it !

                // TODO THis might be tricky
                // NOTE That we do not have this data for statically failing
                // tests, so we need to patch that in...
                // However, we rely on the sequential nature of the execution
                for (Failure f : failures) {
                    // Skip Statically Failing tests
                    if (f.getDescription().getMethodName() == null)
                        continue;
//                    objectOutputStream.writeObject(f.getDescription().getClassName()
//                            + "." + f.getDescription().getMethodName());
                    // not qualified test class name
                    objectOutputStream.writeObject(f.getDescription().getTestClass().getSimpleName());
                    // Write also WHY the test failed ?
                    objectOutputStream.writeObject(f.getTrace());
                }
                // Public the Others
                for (Integer i : staticallyFailedTests) {
                    objectOutputStream.writeObject(testList.get(i));
                    // Write also WHY the test failed ?
                    objectOutputStream.writeObject(staticallyFailedTestTraces.get(i));
                }

                objectOutputStream.flush();

            } // This will close stream and socket

        } catch (InstantiationError | Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static String getTestCaseClassName(String testCaseName, String projectPackageStructure){
        return projectPackageStructure + "." + testCaseName;
    }

}
