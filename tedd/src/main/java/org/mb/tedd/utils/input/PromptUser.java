package org.mb.tedd.utils.input;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.mb.tedd.algorithm.filter.nlp.VerbType;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.utils.Properties;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class PromptUser {

    private List<String> testsOrder;
    private static final int maxAttemptsConstant = 3;
    private final static Logger logger = Logger.getLogger(PromptUser.class.getName());

    public PromptUser(){
        this.testsOrder = Arrays.asList(Properties.tests_order);
    }

    public GraphNode<String> getSourceNode(){
        return this.getNode(true);
    }

    public GraphNode<String> getTargetNode(){
        return this.getNode(false);
    }

    @SuppressWarnings("unchecked")
    // TODO add better input validation: prompt string instead of int and check if string it is a number
    // (promptInt throws error if thing typed is not a number but it would be better to give users a certain number of
    // attempts)
    // TODO outsource logic of retry
    public GraphNode<String> chooseInSet(Set<GraphNode<String>> candidatePreconditions){
        int index = this.promptUserToTypeInt("Choose a test among " + candidatePreconditions
                + ". Type the index of the list to select the element. Index starts from 0 and must be"
                + " [0," + (candidatePreconditions.size() - 1) + "]");
        int counter = maxAttemptsConstant;
        // TODO to test
        while(!(index >= 0 && index <= candidatePreconditions.size() - 1) && counter > 1){
            index = this.promptUserToTypeInt("Choose a test among " + candidatePreconditions
                    + ". Type the index of the list to select the element. Index starts from 0 and must be"
                    + " [0," + (candidatePreconditions.size() - 1) + "]. Attempts left "
                    + (counter - 1) + "/" + maxAttemptsConstant + ".");
            counter--;
        }
        if(counter == 0)
            throw new RuntimeException("Entered a wrong value for " + maxAttemptsConstant + " times");
        return (GraphNode<String>) candidatePreconditions.toArray()[index];
    }

    public boolean promptAreYouSure(String promptMessage){
        String answer = this.promptUserToTypeString(promptMessage + " Type 'yes' for confirmation or 'no' to decline.");
        if(answer.equalsIgnoreCase("yes"))
            return true;
        if(answer.equalsIgnoreCase("no"))
            return false;
        throw new RuntimeException("Typed an unknown value: " + answer + ". Expected 'yes' or 'no'.");
    }

    public VerbType promptVerbType(String verb){
        List<VerbType> verbTypes = Arrays.asList(VerbType.values());
        int index = this.promptUserToTypeInt("Choose a classification for the verb " + verb
                + " among " + verbTypes + ". Type the index of the list to select the element. " +
                "Index starts from 0 and must be" + " [0," + (verbTypes.size() - 1) + "]. Choose IGNORE "
                + " if the verb prompted by the program is not what you expect (POS tagger did not work properly). " +
                "Otherwise choose either READ or WRITE.");
        Preconditions.checkArgument(index >= 0 && index <= verbTypes.size() - 1, "Index " + index
                + " not valid. Must be [0, " + (verbTypes.size() - 1) + "]");
        return verbTypes.get(index);
    }

    // TODO outsource logic of retry
    private GraphNode<String> getNode(boolean source){
        String sourceOrTarget;
        if(source)
            sourceOrTarget = "source";
        else
            sourceOrTarget = "target";
        String testNode = this.promptUserToTypeString("Enter " + sourceOrTarget + " node of the dependency: ");
        boolean contained = testsOrder.contains(testNode);
        int counter = maxAttemptsConstant;
        while(!contained && counter > 1){
            System.out.println("Test case with name "
                    + testNode + " does not exist in the default order " + this.testsOrder + ". Attempts left "
                    + (counter - 1) + "/" + maxAttemptsConstant + ".");
            testNode = this.promptUserToTypeString("Enter " + sourceOrTarget + " node of the dependency: ");
            contained = testsOrder.contains(testNode);
            counter--;
        }
        if(counter == 0)
            throw new RuntimeException("Entered a wrong value for " + maxAttemptsConstant + " times");
        int testNum = -1;
        for (int i = 0; i < testsOrder.size(); i++) {
            if(testsOrder.get(i).equals(testNode))
                testNum = i;
        }
        Preconditions.checkState(testNum != -1, "Error in setting order of execution of test case " + testNode);
        return new GraphNode<>(testNode, testNum);
    }

    private String promptUserToTypeString(String promptMessage){
        Scanner reader = new Scanner(System.in);
        logger.info(promptMessage);
        System.out.println(promptMessage);
        return reader.next();
    }

    private int promptUserToTypeInt(String promptMessage){
        Scanner reader = new Scanner(System.in);
        logger.info(promptMessage);
        System.out.println(promptMessage);
        return reader.nextInt();
    }
}
