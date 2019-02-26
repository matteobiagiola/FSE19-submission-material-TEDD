package org.mb.tedd.algorithm.extraction.string;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.algorithm.extraction.DependencyGraphExtractor;
import org.mb.tedd.graph.DependencyGraphManager;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.parsing.TestCaseParser;
import org.mb.tedd.statement.Action;
import org.mb.tedd.statement.ActionParameter;
import org.mb.tedd.statement.AssertEqualsStatement;
import org.mb.tedd.statement.AssertStatement;
import org.mb.tedd.statement.AssertThatStatement;
import org.mb.tedd.statement.AssertType;
import org.mb.tedd.statement.DriverStatement;
import org.mb.tedd.statement.FlakinessFixerStatement;
import org.mb.tedd.statement.LiteralParameter;
import org.mb.tedd.statement.SelectStatement;
import org.mb.tedd.statement.SeleniumStatement;
import org.mb.tedd.utils.ExecutionTime;
import org.mb.tedd.utils.Properties;
import spoon.reflect.declaration.CtClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.stream.Collectors;

public class StringAnalysisDependencyGraphExtractor implements DependencyGraphExtractor {

    private final static Logger logger = Logger.getLogger(StringAnalysisDependencyGraphExtractor.class.getName());
    private List<CtClass<?>> testClasses;

    public StringAnalysisDependencyGraphExtractor(List<CtClass<?>> testClasses){
        this.testClasses = new LinkedList<>(testClasses);
    }

    @Override
    public DependencyGraphManager<String> computeDependencies(){
        long start = System.currentTimeMillis();
        Map<String, List<SeleniumStatement>> map = new LinkedHashMap<>(this.mapTestsToSeleniumStatements(this.testClasses));
        List<String> testNames = Arrays.asList(Properties.tests_order);
        List<String> testsToAnalyze = new ArrayList<>(testNames);
        DependencyGraphManager<String> dependencyGraphManager = new DependencyGraphManager<>();
        for (int i = 0; i < testNames.size() - 1; i++) {
            String testName = testNames.get(i);
            testsToAnalyze.remove(testName);
            Set<StringValue> writtenValues = this.findStringValuesWrittenInTestCase(map.get(testName));
            GraphNode<String> graphTargetNode = new GraphNode<>(testName, i);
            for (String testToAnalyze : testsToAnalyze) {
                Set<StringValue> readValues = this.findStringValuesInTestCase(map.get(testToAnalyze), writtenValues);
                if (!readValues.isEmpty()) {
                    int indexOfTestToAnalyze = testNames.indexOf(testToAnalyze);
                    GraphNode<String> graphSourceNode = new GraphNode<>(testToAnalyze, indexOfTestToAnalyze);
                    GraphEdge graphEdge = new GraphEdge(readValues, graphSourceNode, graphTargetNode);
                    dependencyGraphManager.addDependency(graphSourceNode, graphTargetNode, graphEdge);
                }
            }
        }

        Graph<GraphNode<String>, GraphEdge> dependencyGraph = dependencyGraphManager.getDependencyGraph();
        int numberOfDependenciesInComputedGraph = dependencyGraph.edgeSet().size();
        int numberOfDependenciesInBaseline = this.computeNumberOfDependenciesInBaseline();
        if(numberOfDependenciesInBaseline == numberOfDependenciesInComputedGraph){
            logger.warn("String analysis produces the same graph as the baseline");
        }
        long timeElapsed = System.currentTimeMillis() - start;
        logger.info("Time to compute the initial dependency graph: " + new ExecutionTime().computeExecutionTime(timeElapsed));
        return dependencyGraphManager;
    }

    private int computeNumberOfDependenciesInBaseline(){
        int numberOfTestCases = Properties.tests_order.length;
        int counter = 0;
        for (int i = numberOfTestCases - 1; i > 0; i--) {
            counter = counter + i;
        }
        return counter;
    }

    private List<SeleniumStatement> getTestSeleniumStatements(CtClass testClass){
        TestCaseParser testCaseParser = new TestCaseParser();
        return testCaseParser.getSeleniumStatements(testClass);
    }

    private Map<String, List<SeleniumStatement>> mapTestsToSeleniumStatements(List<CtClass<?>> testClasses){
        Map<String, List<SeleniumStatement>> map = new LinkedHashMap<>();
        List<String> testClassNames = new ArrayList<>();
        for(CtClass testClass: testClasses){
            testClassNames.add(testClass.getSimpleName());
            map.put(testClass.getSimpleName(), this.getTestSeleniumStatements(testClass));
        }
        this.checkClassNamesWithInputTestNames(testClassNames);
        return map;
    }

    private void checkClassNamesWithInputTestNames(List<String> testClassNames){
        List<String> testInputNames = Arrays.asList(Properties.tests_order);
        for (String testInputName: testInputNames){
            if(!testClassNames.contains(testInputName)){
                throw new IllegalArgumentException("There is no test named " + testInputName + " in " + Properties.TESTS_PATH);
            }
        }
    }

    /**
     *
     * @implNote assumption: locator container actions are always read action
     * @implNote assumption: locator container has only one parameter that is an action && a locator
     * @see DriverStatement#getLocator()
     * */
    private Set<StringValue> findStringValuesWrittenInTestCase(List<SeleniumStatement> testCaseStatements){
        List<String> writeActionNames = Arrays.asList(Properties.WRITE_ACTIONS);
        Set<StringValue> stringValues = new LinkedHashSet<>();
        for (SeleniumStatement seleniumStatement: testCaseStatements){
            List<Action> actions = seleniumStatement.getActions();
            List<Action> writeActions = actions.stream()
                    .filter(action -> {
                        List<ActionParameter> actionParameters = action.getActionParameters();
                        return this.paramsContainWriteAction(writeActionNames, actionParameters)
                                || writeActionNames.contains(action.getName());
                    })
                    .collect(Collectors.toList());
            for (Action writeAction : writeActions) {
                StringValue stringValue;
                if(writeAction.isLocatorContainer()){
                    // should arrive here only because locator container contains a locator whose name
                    // is a write action
                    List<ActionParameter> actionParameters = writeAction.getActionParameters();
                    Optional<Action> locatorActionOptional = actionParameters.stream()
                            .filter(actionParameter -> {
                                if(actionParameter.isAction()){
                                    Action action = (Action) actionParameter;
                                    if(action.isLocator())
                                        return writeActionNames.contains(action.getName());
                                    return false;
                                }
                                return false;
                            })
                            .map(actionParameter -> (Action) actionParameter)
                            .findAny();
                    Preconditions.checkState(locatorActionOptional.isPresent(), "No locator contained in " +
                            "locator container action " + writeAction);
                    stringValue = new StringLocatorValue(locatorActionOptional.get());
                } else {
                    Action locator = seleniumStatement.getLocator();
                    stringValue = new StringDriverValue(locator, writeAction);
                }
                stringValues.add(stringValue);
            }
        }
        return stringValues;
    }

    /**
     * @return true if there is at least one action in @param actionParameters
     * that has the same name of an item of @param writeActionNames (i.e. is a
     * write action)
     * */
    private boolean paramsContainWriteAction(List<String> writeActionNames, List<ActionParameter> actionParameters){
        boolean result = false;
        for (ActionParameter actionParameter : actionParameters) {
            if(actionParameter.isAction()){
                Action action = (Action) actionParameter;
                result = writeActionNames.contains(action.getName())
                        || this.paramsContainWriteAction(writeActionNames, action.getActionParameters());
            } else if(actionParameter.isLiteral()) {
                result = false;
            } else {
                throw new IllegalStateException("Unknown action parameter type: " + actionParameter);
            }
        }
        return result;
    }

    private Set<StringValue> findStringValuesInTestCase(List<SeleniumStatement> testCaseStatements,
                                                        Set<StringValue> writtenValues){
        Set<StringValue> stringValues = new LinkedHashSet<>();
        for(SeleniumStatement testCaseStatement: testCaseStatements){
            Action locator = testCaseStatement.getLocator();
            if(testCaseStatement instanceof DriverStatement
                    || testCaseStatement instanceof FlakinessFixerStatement
                    || testCaseStatement instanceof SelectStatement) {
                stringValues.addAll(this.getReadActionsInDriverLikeStatement(testCaseStatement, locator,
                        writtenValues, false));
            } else if(testCaseStatement instanceof AssertStatement) {
                AssertStatement assertStatement = (AssertStatement) testCaseStatement;
                if(assertStatement.getAssertType().equals(AssertType.EQUALS)
                        || assertStatement.getAssertType().equals(AssertType.THAT)){
                    LiteralParameter valueToBeChecked;
                    if(assertStatement.getAssertType().equals(AssertType.EQUALS)){
                        valueToBeChecked = ((AssertEqualsStatement) assertStatement).getValueToBeChecked();
                    } else {
                        valueToBeChecked = ((AssertThatStatement) assertStatement).getValueToBeChecked();
                    }
                    boolean match = writtenValues.stream()
                            .anyMatch(writtenValue -> {
                                if(writtenValue.getValueType().equals(ValueType.DRIVER)){
                                    StringDriverValue stringDriverValue = (StringDriverValue) writtenValue;
                                    Action driverAction = stringDriverValue.getDriverAction();
                                    return driverAction.getActionParameters().stream()
                                            .anyMatch(actionParameter -> actionParameter.equals(valueToBeChecked));
                                }
                                return false;
                            });
                    if(match){
                        List<Action> assertStatementActions = assertStatement.getDriverStatement().getActions();
                        Action lastAction = assertStatementActions.get(assertStatementActions.size() - 1);
                        stringValues.add(new StringAssertValue(locator, lastAction, valueToBeChecked));
                    }
                } else if(assertStatement.getAssertType().equals(AssertType.TRUE)
                        || assertStatement.getAssertType().equals(AssertType.FALSE)) {
                    stringValues.addAll(this.getReadActionsInDriverLikeStatement(assertStatement.getDriverStatement(),
                            locator, writtenValues, true));
                } else {
                    throw new IllegalStateException("Unknown assert type " + assertStatement.getAssertType().getValue()
                            + " in assert statement " + assertStatement);
                }
            } else {
                throw new IllegalStateException("Unknown statement " + testCaseStatement);
            }
        }
        return stringValues;
    }

    /**
     * @implNote It assumes that the last action of a driver statement and the locator of that driver statement
     * may contain written values
     * */
    private Set<StringValue> getReadActionsInDriverLikeStatement(SeleniumStatement seleniumStatement,
                                                                 Action locator,
                                                                 Set<StringValue> writtenValues,
                                                                 boolean actionInAssert){
        Set<StringValue> writtenAndReadValues = new LinkedHashSet<>();
        Action lastAction = seleniumStatement.getActions().get(seleniumStatement.getActions().size() - 1);
        if(this.isReadAction(writtenValues, lastAction, actionInAssert)){
            writtenAndReadValues.add(new StringDriverValue(locator, lastAction));
        }
        if(this.isReadAction(writtenValues, locator, true)){
            writtenAndReadValues.add(new StringLocatorValue(locator));
        }
        return writtenAndReadValues;
    }

    /**
     * @param containsComparison if true values are compared using a contains operation; if false equals operation is applied
     * */
    private boolean isReadAction(Set<StringValue> writtenValues, Action action, boolean containsComparison){
        return writtenValues.stream()
                .anyMatch(writtenValue -> {
                    if(writtenValue.getValueType().equals(ValueType.DRIVER)){
                        StringDriverValue writtenDriverValue = (StringDriverValue) writtenValue;
                        Action writtenDriverAction = writtenDriverValue.getDriverAction();
                        List<ActionParameter> writtenActionParameters = writtenDriverAction.getActionParameters();
                        List<ActionParameter> actionParameters = action.getActionParameters();
                        return this.anyMatch(writtenActionParameters, actionParameters, containsComparison);
                    } else if(writtenValue.getValueType().equals(ValueType.LOCATOR)){
                        StringLocatorValue stringLocatorValue = (StringLocatorValue) writtenValue;
                        List<ActionParameter> writtenActionParameters = stringLocatorValue.getLocator().getActionParameters();
                        List<ActionParameter> actionParameters = action.getActionParameters();
                        return this.anyMatch(writtenActionParameters, actionParameters, containsComparison);
                    }
                    throw new UnsupportedOperationException("Written value " + writtenValue + " with type "
                            + writtenValue.getValueType() + " not supported in read/write matching");
                });
    }

    /**
     * @param containsComparison if true values are compared using a contains operation; if false equals operation is applied
     * @return true if a written parameter is equals to a parameter of an action
     * */
    private boolean anyMatch(List<ActionParameter> writtenActionParameters, List<ActionParameter> actionParameters, boolean containsComparison){
        if(containsComparison){
            return writtenActionParameters.stream()
                    .anyMatch(writtenActionParameter ->
                            actionParameters.stream().anyMatch(actionParameter -> {
                                if(writtenActionParameter.isLiteral() && actionParameter.isLiteral()){
                                    LiteralParameter writtenLiteralParameter = (LiteralParameter) writtenActionParameter;
                                    LiteralParameter literalParameter = (LiteralParameter) actionParameter;
                                    return literalParameter.contains(writtenLiteralParameter);
                                }
                                return false;
                            })
                    );
        }
        return writtenActionParameters.stream()
                .anyMatch(writtenActionParameter ->
                        actionParameters.stream().anyMatch(actionParameter -> {
                            if(writtenActionParameter.isAction() || actionParameter.isAction())
                                return false;
                            return actionParameter.equals(writtenActionParameter);
                        })
                );
    }

}