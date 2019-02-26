package org.mb.tedd.algorithm.execution;

import org.apache.log4j.Logger;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.utils.Properties;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class TestCaseExecutor<T> {

    private String projectClasspath;
    private String thisProjectClasspath;
    private String projectPath;
    private final static Logger logger = Logger.getLogger(TestCaseExecutor.class.getName());
    private final static ExecutorService testExecutorThreadsPool = Executors.newFixedThreadPool(1);

    public TestCaseExecutor(){

        String projectClasspath = Properties.PROJECT_CLASSPATH;
        String testsPath = Properties.TESTS_PATH;
        int indexOfMvnPackageStructure = testsPath.indexOf(Properties.mvn_package_structure);

        this.projectPath = testsPath.substring(0, indexOfMvnPackageStructure - 1); // -1 without counting the '/' character

        this.projectClasspath = projectClasspath + Properties.classpath_separator
                + this.addProjectClasses(projectPath);

        this.thisProjectClasspath = this.addProjectClasses(Properties.user_dir);
    }

    private String addProjectClasses(String projectPath){
        return projectPath + Properties.file_separator + "target" + Properties.file_separator + "classes";
    }

    /*
     * Spin off a new JVM with JUnitCore inside, this is faster than CUT but
     * works only in the local VM ATM.
     */
    public Map<GraphNode<T>, TestResult> executeTestsRemoteJUnitCore(Set<GraphNode<T>> theSchedule)
            throws IOException, InterruptedException, ExecutionException {

        List<String> schedule = new ArrayList<>();
        for (GraphNode<T> graphNode : theSchedule) {
            schedule.add(graphNode.getTestCase().toString());
        }

        Map.Entry<Integer, List<String>> testResult = this.remoteExecutionWithJUnitCore(schedule);

        if (testResult.getKey() != schedule.size()) {
            logger.debug("ERROR TEST COUNT DOES NOT RUN !!! ");
            logger.debug(testResult.getKey() + " != " + schedule.size());
            throw new RuntimeException("Some tests did not run ! ");
        }

        Map<GraphNode<T>, TestResult> ret = new HashMap<>();
        for (String test: schedule) {
            Optional<GraphNode<T>> graphNodeFoundOptional = theSchedule.stream()
                    .filter(graphNode -> graphNode.getTestCase().toString().equals(test))
                    .findAny();
            if(graphNodeFoundOptional.isPresent()){
                ret.put(graphNodeFoundOptional.get(), TestResult.PASS);
            } else {
                throw new RuntimeException("GraphNode named " + test + " not found");
            }
        }

        for (String failed: testResult.getValue()) {
            Optional<GraphNode<T>> graphNodeFoundOptional = theSchedule.stream()
                    .filter(graphNode -> graphNode.getTestCase().toString().equals(failed))
                    .findAny();
            if(graphNodeFoundOptional.isPresent()){
                ret.put(graphNodeFoundOptional.get(), TestResult.FAIL);
            } else {
                throw new RuntimeException("GraphNode named " + failed + " not found");
            }
        }

        return ret;
    }

    private Map.Entry<Integer, List<String>> remoteExecutionWithJUnitCore(List<String> schedule)
            throws IOException, InterruptedException, ExecutionException {
        try (ServerSocket server = new ServerSocket(0)) {
            final int port = server.getLocalPort();

            // PROBABLY OPENING THE SOCKET SHALL BE DONE ONLY ONCE, THEN WE
            // SHOULD USE just accept
            Future<Map.Entry<Integer, List<String>>> future = testExecutorThreadsPool
                    .submit(new ResultsCollector(server, port));

            // Probably we can check here if the port is already open and wait
            // otherwise... but without triggering the accept on the server side

            // TODO: Let's hope that between now and pb.start the server started
            // Prepare the invocation of the remote JUnitCore !
            String jvm_location = System.getProperties().getProperty("java.home")
                    + Properties.file_separator + "bin" + Properties.file_separator + "java";

            String classpath = this.projectClasspath + Properties.classpath_separator
                    + System.getProperty("java.class.path") + Properties.classpath_separator
                    + this.thisProjectClasspath;

            List<String> args = new ArrayList<>();
            args.add(jvm_location);

            args.add("-enableassertions");

            args.add("-cp");
            args.add(classpath);

            args.add(RemoteJUnitCore.class.getName());

            args.add("--port");
            args.add(String.valueOf(port));

            args.add("--mvn-package-structure");
            args.add(Properties.mvn_package_structure);

            args.add("--test-list");
            args.addAll(schedule);

            args.add("--tests-path");
            args.add(Properties.TESTS_PATH);

            args.add("--reset-qualified-class-name");
            args.add(Properties.RESET_CLASS_NAME);

            args.add("--reset-method-name");
            args.add(Properties.RESET_METHOD_NAME);

            /// TODO Not sure why but empty arguments break the thing...

            // Note that we need probably to split the test string if more than
            // one
            // test is there
            ProcessBuilder processBuilder = new ProcessBuilder(args);

            processBuilder.directory(new File(this.projectPath));
            processBuilder.inheritIO();

            Process slaveJVM = processBuilder.start();

            // Wait for everything to finish ... ?
            int exitCode = slaveJVM.waitFor();
            if (exitCode != 0) {
                logger.debug("ERROR !! Remote test execution FAILED !!!");
                System.exit(1);
            }

            return future.get();
        }
    }
}
