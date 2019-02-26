package org.mb.tedd.parsing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.mb.tedd.statement.Action;
import org.mb.tedd.statement.ActionParameter;
import org.mb.tedd.statement.AssertEqualsStatement;
import org.mb.tedd.statement.AssertFalseStatement;
import org.mb.tedd.statement.AssertStatement;
import org.mb.tedd.statement.AssertThatStatement;
import org.mb.tedd.statement.AssertTrueStatement;
import org.mb.tedd.statement.AssertType;
import org.mb.tedd.statement.DriverStatement;
import org.mb.tedd.statement.FlakinessFixerStatement;
import org.mb.tedd.statement.LiteralParameter;
import org.mb.tedd.statement.SelectStatement;
import org.mb.tedd.statement.SeleniumStatement;
import org.mb.tedd.utils.Properties;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;

public class TestCaseParser {

    private final static Logger logger = Logger.getLogger(TestCaseParser.class.getName());
    private Factory factory;
    private SpoonAPI spoon;

    public TestCaseParser(File classFile){
        SpoonAPI spoon = new Launcher();
        spoon.getEnvironment().setNoClasspath(true);
        spoon.addInputResource(classFile.getAbsolutePath());
        spoon.buildModel();
        this.spoon = spoon;
        this.factory = spoon.getFactory();
    }

    public TestCaseParser(){

    }

    /**
     * @implNote if the parsed class is a PO, it transforms it according to rules
     * defined by the creation of the CUT.
     * */
    public Optional<CtClass<?>> getCtRepresentationOfClass(){
        Optional<CtClass<?>> ctClassOptional;
        List<CtClass<?>> ctClasses = this.spoon.getModel().getElements(ctClass -> true);
        if(ctClasses.isEmpty()){
            ctClassOptional = Optional.empty();
        }else{
            CtClass<?> ctClass = ctClasses.get(0);
            ctClassOptional = Optional.of(ctClass);
        }
        return ctClassOptional;
    }

    public List<SeleniumStatement> getSeleniumStatements(CtClass<?> testClass){
        Set<CtMethod<?>> methods = testClass.getMethods();
        List<SeleniumStatement> result = new ArrayList<>();
        if(!Properties.PAGE_OBJECTS){
            Optional<List<SeleniumStatement>> optionalResult =
                    methods.stream()
                        .map(method -> {
                            logger.debug("method " + method.getSimpleName());
                            CtBlock<?> body = method.getBody();
                            return body.getStatements()
                                    .stream()
                                    .filter(statementToSelect)
                                    .map(TestCaseParser::handleStatement)
                                    .collect(Collectors.toList());
                        })
                        .reduce((list1, list2) -> {
                            List<SeleniumStatement> sum = new ArrayList<>(list1);
                            sum.addAll(list2);
                            return sum;
                        });
            if(optionalResult.isPresent()){
                result = optionalResult.get();
            } else {
                throw new IllegalStateException("Selenium statement list must be present");
            }
        }
        if(result.isEmpty())
            throw new IllegalStateException("No selenium statements in test class " + testClass.getSimpleName());
        return result;
    }


    private static Predicate<CtStatement> statementToSelect = statement -> {
      if(statement instanceof CtInvocation){
          CtInvocation invocation = (CtInvocation) statement;
          if(isAssertStatement(invocation) || isDriverStatement(invocation)
                  || isFlakinessFixerStatement(invocation) || isSelectStatement(invocation)) return true;
          else logger.warn("Invocation statement " + statement + " " + statement.getClass() + " ignored");
      }
      logger.warn("Statement " + statement + " " + statement.getClass() + " ignored");
      return false;
    };

    private static boolean isAssertStatement(CtInvocation<?> invocation){
        int indexFirstParenthesis = invocation.toString().indexOf("(");
        if(invocation.toString().substring(0, indexFirstParenthesis).contains("assert")){
            if(invocation.toString().substring(0, indexFirstParenthesis).contains("assert" + AssertType.TRUE.getValue())
                    || invocation.toString().substring(0, indexFirstParenthesis).contains("assert" + AssertType.EQUALS.getValue())
                    || invocation.toString().substring(0, indexFirstParenthesis).contains("assert" + AssertType.FALSE.getValue())
                    || invocation.toString().substring(0, indexFirstParenthesis).contains("assert" + AssertType.THAT.getValue())){
                return true;
            } else {
                throw new UnsupportedOperationException("Assertion " + invocation.toString() + " not supported yet!");
            }
        } else {
            return false;
        }
    }

    private static boolean isDriverStatement(CtInvocation<?> invocation){
        int indexFirstParenthesis = invocation.toString().indexOf("(");
        return invocation.toString().substring(0, indexFirstParenthesis).contains("driver.findElement");
    }

    private static boolean isSelectStatement(CtInvocation<?> invocation){
        return invocation.toString().contains("new org.openqa.selenium.support.ui.Select");
    }

    /**
     * @implNote filter all actions that has as a parameter a WebElement (action without locator)
     * */
    private static boolean isFlakinessFixerStatement(CtInvocation<?> invocation){
        return SeleniumActions.isContained(invocation.toString())
                && invocation.getExecutable().getParameters()
                    .stream().anyMatch(executableParameter
                        -> !executableParameter.getSimpleName().equals("WebElement"));
    }

    private static SeleniumStatement handleStatement(CtStatement statement){
        logger.debug("Statement: " + statement + ": " + statement.getClass());
        if(statement instanceof CtInvocation){
            CtInvocation invocation = (CtInvocation) statement;
            if(isDriverStatement(invocation)) {
                DriverStatement driverStatement = handleDriverStatement(invocation);
                logger.debug("Driver statement: " + driverStatement);
                return driverStatement;
            } else if(isAssertStatement(invocation)){
                AssertStatement assertStatement = handleAssertStatement(invocation);
                logger.debug("Assert statement: " + assertStatement);
                return assertStatement;
            } else if(isFlakinessFixerStatement(invocation)){
                FlakinessFixerStatement flakinessFixerStatement = handleFlakinessFixerStatement(invocation);
                logger.debug("Flakiness fixer statement: " + flakinessFixerStatement);
                return flakinessFixerStatement;
            } else if(isSelectStatement(invocation)){
                SelectStatement selectStatement = handleSelectStatement(invocation);
                logger.debug("Select statement: " + selectStatement);
                return selectStatement;
            }
            else {
                throw new IllegalStateException("Statement " + statement + " is not a driver nor an assert statement nor a flakiness fixer statement nor a select statement");
            }
        } else {
            throw new UnsupportedOperationException("Statement " + statement + " " + statement.getClass() + " not supported");
        }
    }

    // e.g. driver.findElement(By.id("foo")).sendKeys("abcd");
    private static DriverStatement handleDriverStatement(CtInvocation<?> invocation){
        return new DriverStatement(handleInvocationStatement(invocation));
    }

    // e.g. new Select(driver.findElement(By.id("foo"))).selectByVisibleText("abcd");
    private static SelectStatement handleSelectStatement(CtInvocation<?> invocation){
        if(invocation.getTarget() instanceof CtConstructorCall){
            List<CtExpression<?>> selectActionArguments = invocation.getArguments();
            List<ActionParameter> selectActionArgumentsString = handleInvocationArgumentsStatement
                    (selectActionArguments, invocation);
            Action selectAction = new Action(handleExecutableStatement(invocation.getExecutable()),
                    selectActionArgumentsString);
            CtConstructorCall constructorCall = (CtConstructorCall) invocation.getTarget();
            Preconditions.checkState(constructorCall.getArguments().size() == 1,
                    "Select constructor " + invocation.getTarget()
                            + " must have only 1 argument. Found " + constructorCall.getArguments().size());
            Preconditions.checkState(constructorCall.getArguments().get(0) instanceof CtInvocation,
                    "Argument of constructor call " + constructorCall.getArguments().get(0)
                            + " must be an invocation statement. Found: "
                            + constructorCall.getArguments().get(0).getClass());
            CtInvocation ctInvocation = (CtInvocation) constructorCall.getArguments().get(0);
            Preconditions.checkState(isDriverStatement(ctInvocation), "Argument of constructor call "
                    + ctInvocation + " must be a driver statement.");
            DriverStatement driverStatement = new DriverStatement(handleInvocationStatement(ctInvocation));
            return new SelectStatement(selectAction, driverStatement);
        } else {
            throw new IllegalStateException("Target must be a constructor call. Found: "
                    + invocation.getTarget() + " " + invocation.getTarget().getClass());
        }
    }

    // e.g. objectFixer.sendKeys(By.id("foo"), "abcd")
    private static FlakinessFixerStatement handleFlakinessFixerStatement(CtInvocation<?> invocation){
        String callerName = invocation.getTarget().toString();
        if(invocation.getTarget() instanceof CtInvocation){
            throw new UnsupportedOperationException("Flakiness fixer statement supports only one method call after caller name "
                    + callerName + ". Found " + invocation);
        } else if(invocation.getTarget() instanceof CtFieldRead
                || invocation.getTarget() instanceof CtTypeAccess){
            List<ActionParameter> actionParameters = handleInvocationArgumentsStatement(
                    invocation.getArguments(), invocation);
            String actionName = handleExecutableStatement(invocation.getExecutable());
            Optional<Action> optionalLocator = actionParameters.stream()
                    .filter(actionParameter -> {
                        if(actionParameter.isAction()){
                            Action action = (Action) actionParameter;
                            return action.isLocator();
                        }
                        return false;
                    })
                    .map(actionParameter -> (Action) actionParameter)
                    .findAny();
            Preconditions.checkState(optionalLocator.isPresent(), "Flakiness fixer statement has no locator: " + invocation);
            Action locator = optionalLocator.get();
            List<ActionParameter> actionParametersWithoutLocator = actionParameters.stream()
                    .filter(actionParameter -> {
                        if(actionParameter.isAction()){
                            Action action = (Action) actionParameter;
                            return !action.isLocator();
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            Action seleniumAction = new Action(actionName, actionParametersWithoutLocator);
            return new FlakinessFixerStatement(locator, callerName, seleniumAction);
        }
        else {
            throw new IllegalStateException("Invocation statement target unknown: " + invocation + " " + invocation.getTarget() + " " + invocation.getTarget().getClass());
        }
    }

    // e.g. assertTrue(driver.findElement(By.id("foo")).getText().equals("abcd"))
    // e.g. assertEquals(driver.findElement(By.id("foo")).getText(), "abcd")
    private static AssertStatement handleAssertStatement(CtInvocation<?> invocation){
        Preconditions.checkArgument(!invocation.getArguments().isEmpty(),
                "Invocation assert " + invocation + " must have at least one argument");
        if(invocation.getArguments().size() == 2){
            if(invocation.toString().contains("assert" + AssertType.EQUALS.getValue())
                    || invocation.toString().contains("assert" + AssertType.THAT.getValue())){
                CtExpression<?> firstArgument = invocation.getArguments().get(0);
                CtExpression<?> secondArgument = invocation.getArguments().get(1);
                if(firstArgument instanceof CtLiteral){
                    String valueToBeChecked = firstArgument.toString();
                    if(secondArgument instanceof CtInvocation){
                        LiteralParameter literalParameter = new LiteralParameter(valueToBeChecked);
                        if(invocation.toString().contains("assert" + AssertType.EQUALS.getValue())){
                            return new AssertEqualsStatement(literalParameter,
                                    handleDriverStatement((CtInvocation<?>) secondArgument));
                        } else if (invocation.toString().contains("assert" + AssertType.THAT.getValue())){
                            List<CtElement> driverStatements = invocation
                                    .getElements(ctElement -> ctElement.toString().startsWith("driver")
                                            && ctElement.toString().endsWith("()") // to get the driver with the last action
                                            && ctElement.toString().contains("driver.findElement")
                                            && ctElement instanceof CtStatement);
                            Preconditions.checkState(!driverStatements.isEmpty() && driverStatements.size() == 1,
                                    "There must be only one driver statement in "
                                            + invocation + ". Found: " + driverStatements);
                            CtInvocation driverStatement = (CtInvocation) driverStatements.get(0);
                            logger.info("AssertThatStatement: " + (new AssertThatStatement(literalParameter, handleDriverStatement(driverStatement)).toString()));
                            return new AssertThatStatement(literalParameter, handleDriverStatement(driverStatement));
                        }
                        throw new IllegalStateException("Unknown assert statement " + invocation);
                    } else {
                        throw new UnsupportedOperationException("second argument of assertion "
                                + invocation + " must be a driver invocation. Found " + secondArgument.getClass());
                    }
                } else {
                    throw new UnsupportedOperationException("first argument of assertion "
                            + invocation + " must be a literal. Found " + firstArgument.getClass());
                }
            } else {
                throw new UnsupportedOperationException("Assert with two arguments must be AssertEquals. Found: " + invocation);
            }
        } else if(invocation.getArguments().size() == 1) {
            CtExpression<?> firstArgument = invocation.getArguments().get(0);
            if(firstArgument instanceof CtInvocation){
                if(invocation.toString().contains("assert" + AssertType.TRUE.getValue())){
                    return new AssertTrueStatement(handleDriverStatement((CtInvocation<?>) firstArgument));
                }else if(invocation.toString().contains("assert" + AssertType.FALSE.getValue())){
                    return new AssertFalseStatement(handleDriverStatement((CtInvocation<?>) firstArgument));
                } else {
                    throw new UnsupportedOperationException("Assert with one argument must be assertTrue or assertFalse. Found: " + invocation);
                }
            } else {
                throw new UnsupportedOperationException("first argument of assertion "
                        + invocation + " must be a driver invocation. Found " + firstArgument.getClass());
            }
        } else {
            throw new IllegalArgumentException("Invocation assert "
                    + invocation + " cannot have " + invocation.getArguments().size() + " arguments. Required 1 or 2.");
        }
    }

    private static List<Action> handleInvocationStatement(CtInvocation<?> invocation){
        List<Action> result = new ArrayList<>();
        if(invocation.getTarget() instanceof CtInvocation){
            List<CtExpression<?>> driverActionArguments = invocation.getArguments();
            List<ActionParameter> driverActionArgumentStrings = handleInvocationArgumentsStatement
                    (driverActionArguments, invocation);
            Action action = new Action(handleExecutableStatement(invocation.getExecutable()),
                    driverActionArgumentStrings);
            result.add(action);
            logger.debug("Invocation statement target invocation: " + invocation + ", action: " + action);
            result.addAll(handleInvocationStatement((CtInvocation) invocation.getTarget()));
        } else if(invocation.getTarget() instanceof CtFieldRead
                || invocation.getTarget() instanceof CtTypeAccess){
            List<CtExpression<?>> driverActionArguments = invocation.getArguments();
            List<ActionParameter> driverActionArgumentStrings = handleInvocationArgumentsStatement
                    (driverActionArguments, invocation);
            Action action = new Action(handleExecutableStatement(invocation.getExecutable()),
                    driverActionArgumentStrings);
            result.add(action);
            logger.debug("Invocation statement target fieldRead/typeAccess: " + invocation + ", action: " + action);
        } else if(invocation.getTarget() instanceof CtThisAccess){
            List<CtElement> locators = invocation.getElements(ctElement ->
                    ctElement.toString().startsWith("org.openqa.selenium.By")
                    && ctElement.toString().endsWith(")")
                    && ctElement instanceof CtInvocation);
            Preconditions.checkState(!locators.isEmpty() && locators.size() == 1,
                    "There must be only one locator in " + invocation + ". Found: " + locators);
            List<CtExpression<?>> driverActionArguments = invocation.getArguments();
            List<ActionParameter> driverActionArgumentStrings = handleInvocationArgumentsStatement
                    (driverActionArguments, invocation);
            Action action = new Action(handleExecutableStatement(invocation.getExecutable()),
                    driverActionArgumentStrings);
            result.add(action);
        } else {
            logger.debug("Invocation statement target unknown: " + invocation + " " + invocation.getTarget() + " " + invocation.getTarget().getClass());
        }
        Preconditions.checkState(!result.isEmpty(),
                "List of actions cannot be empty for statement " + invocation + " "
                        + invocation.getTarget().getClass());
        return result;
    }

    private static String handleExecutableStatement(CtExecutableReference executableReference){
        return executableReference.getSimpleName();
    }

    private static List<ActionParameter> handleInvocationArgumentsStatement(List<CtExpression<?>> arguments,
                                                                            CtInvocation invocation){
        List<ActionParameter> result = new ArrayList<>();
        CtExecutableReference ctExecutableReference = invocation.getExecutable();
        String actionName = null;
        if(ctExecutableReference != null){
            actionName = handleExecutableStatement(ctExecutableReference);
        }
        for(CtExpression<?> argument: arguments){
            if(argument instanceof CtInvocation){
                List<Action> actions = handleInvocationStatement((CtInvocation<?>) argument);
                result.addAll(actions);
            } else if(argument instanceof CtLiteral){
                if(actionName != null && actionName.equals("matches")){
                    result.add(new LiteralParameter(argument.toString(), true));
                } else {
                    result.add(new LiteralParameter(argument.toString()));
                }
            } else if(argument instanceof CtFieldRead){
                logger.warn("Ignoring field with value assigned at runtime as a parameter of a selenium action: " + argument);
            } else if(argument instanceof CtBinaryOperator){
                CtBinaryOperator ctBinaryOperator = (CtBinaryOperator) argument;
                if(ctBinaryOperator.getLeftHandOperand() instanceof CtLiteral)
                    result.add(new LiteralParameter((ctBinaryOperator.getLeftHandOperand()).toString()));
                else if(ctBinaryOperator.getRightHandOperand() instanceof CtLiteral)
                    result.add(new LiteralParameter((ctBinaryOperator.getRightHandOperand()).toString()));
            }
            else {
                throw new IllegalStateException("Unknown argument type: " + argument + " " + argument.getClass());
            }
        }
        return result;
    }

}
