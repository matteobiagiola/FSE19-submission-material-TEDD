package org.mb.tedd.algorithm.execution;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


public class ResultsCollector implements Callable<Map.Entry<Integer, List<String>>> {

    private final static Logger logger = Logger.getLogger(ResultsCollector.class.getName());
    private ServerSocket server;
    private int port;

    public ResultsCollector(ServerSocket server, int port){
        this.server = server;
        this.port = port;
    }

    /**
     * @implNote only failed test cases are collected and sent back to the caller
     * */
    @Override
    public Map.Entry<Integer, List<String>> call() {
        try (Socket clientSocket = server.accept()) {
            logger.debug("Server listening to " + port);
            logger.debug("RemoteJUnitCore joined the execution. Waiting for the results...");
            // This blocks
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            ObjectInputStream in = new ObjectInputStream(
                    new BufferedInputStream(clientSocket.getInputStream()));

            final Long runTime = (Long) in.readObject();
            final Integer runCount = (Integer) in.readObject();
            final Integer ignored = (Integer) in.readObject();
            final Integer failureCount = (Integer) in.readObject();

            final List<String> failed = new ArrayList<>();

            for (int i = 0; i < failureCount; i++) {
                String failedTest = (String) in.readObject();
                String stackTrace = (String) in.readObject();
                logger.info("Test " + failedTest + " failed");
                if(stackTrace.split("\n").length > 0){
                    if(stackTrace.split("\n")[0].contains("AssertionError")){
                        logger.info("Assertion error: " + stackTrace);
                    } else {
                        logger.info("Failure message: " + stackTrace.split("\n")[0]);
                    }
                }
                logger.debug("Failed with stack trace:\n " + stackTrace);
                failed.add(failedTest);
            }

            return new AbstractMap.SimpleEntry<>(runCount, failed);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
