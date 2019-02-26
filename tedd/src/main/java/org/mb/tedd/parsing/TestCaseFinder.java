package org.mb.tedd.parsing;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.mb.tedd.utils.Properties;

import java.util.function.Function;
import java.util.function.Predicate;

import java.util.stream.Collectors;

public class TestCaseFinder {

    private final static Logger logger = Logger.getLogger(TestCaseFinder.class.getName());

    public TestCaseFinder(){
    }

    private List<File> getClassFiles(File file){
        List<File> result = new ArrayList<>();
        if(file.isDirectory()){
            for (File child: file.listFiles()){
                result.addAll(this.getClassFiles(child));
            }
        }else if(file.getName().contains(".java")) {
            result.add(file);
        }
        return result;
    }

    private static Function<String, Function<Optional<CtClass<?>>, CtClass<?>>> getCtClassFromOptionalCtClass = errorMessage ->
        optional -> {
            if(optional.isPresent()) return optional.get();
            else throw new IllegalStateException(errorMessage);
        };

    private static Predicate<Optional<?>> isEmptyOptional = Optional::isPresent;

    public List<CtClass<?>> getTestCaseParsedRepresentation(File file){
        List<File> classFiles = this.getClassFiles(file);
        List<CtClass<?>> parsedClasses =
                classFiles.stream()
                .map(file1 -> {
                    TestCaseParser testCaseParser = new TestCaseParser(file1);
                    return testCaseParser.getCtRepresentationOfClass(); // optional because some of them may not be classes
                })
                .filter(isEmptyOptional)
                .map(getCtClassFromOptionalCtClass.apply(this.getClass().getName() + ": file " + file.getAbsolutePath() + " is not a class"))
                .collect(Collectors.toList());
        List<String> testsToConsider = Arrays.asList(Properties.tests_order);
        parsedClasses = parsedClasses.stream().filter(parsedClass ->
                testsToConsider.contains(parsedClass.getSimpleName())).collect(Collectors.toList());
        return parsedClasses;
    }

    public String[] getTestCaseOrder(){
        File testSuiteFile = new File(Properties.TEST_SUITE_PATH);
        TestCaseParser testCaseParser = new TestCaseParser(testSuiteFile);
        Optional<CtClass<?>> ctClassTestSuiteOptional = testCaseParser.getCtRepresentationOfClass();
        Preconditions.checkState(ctClassTestSuiteOptional.isPresent(),
                "Impossible to get compile time representation of class " + testSuiteFile.getAbsolutePath());
        CtClass<?> ctClassTestSuite = ctClassTestSuiteOptional.get();
        Optional<CtAnnotation<?>> suiteClassesAnnotationOptional = ctClassTestSuite.getAnnotations()
                .stream()
                .filter(ctAnnotation -> ctAnnotation.getAnnotationType().getSimpleName().equals("SuiteClasses"))
                .findAny();
        Preconditions.checkState(suiteClassesAnnotationOptional.isPresent(),
                "Test suite " + testSuiteFile + " must have the annotation org.junit.runners.Suite.SuiteClasses" );
        CtAnnotation<?> suiteClassesAnnotation = suiteClassesAnnotationOptional.get();
        Preconditions.checkState(suiteClassesAnnotation.getValues().size() == 1,
                "SuiteClasses annotation must have only one argument");
        CtExpression<?> annotationValue = suiteClassesAnnotation.getValue("value");
        Preconditions.checkState(annotationValue instanceof CtNewArray,
                "Annotation value must be an array");
        CtNewArray<?> arrayOfTestCases = (CtNewArray<?>) annotationValue;
        List<String> testCaseNames = new ArrayList<>();
        for (CtExpression<?> expression : arrayOfTestCases.getElements()) {
            CtFieldRead<?> testCaseFieldRead = (CtFieldRead<?>) expression;
            String qualifiedTestName = testCaseFieldRead.getTarget().toString();
            int indexOfLastDot = qualifiedTestName.lastIndexOf(".");
            testCaseNames.add(qualifiedTestName.substring(indexOfLastDot + 1, qualifiedTestName.length()));
        }
        return testCaseNames.toArray(new String[0]);
    }
}
