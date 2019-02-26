package org.mb.tedd.algorithm.execution;

public enum TestResult {

    PASS ("pass"),
    FAIL ("fail");

    private String testResult;

    TestResult(String testResult){
        this.testResult = testResult;
    }

    public String getTestResult(){
        return this.testResult;
    }
}
