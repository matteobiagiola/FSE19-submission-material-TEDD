package org.mb.tedd.algorithm.filter.nlp;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.log4j.Logger;
import org.mb.tedd.utils.TestCaseUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StopWordsFinder {

    private final static Logger logger = Logger.getLogger(StopWordsFinder.class.getName());

    /**
     * @implNote if a word is present in all test cases (at least one) then it is a stop word
     * */
    public String[] getStopWords(String[] testsOrder){
        List<String> result = new ArrayList<>();
        List<String> testsOrderList = Arrays.asList(testsOrder);
        Multiset<String> wordsInTestCases = HashMultiset.create();
        for (String testCaseName : testsOrderList) {
            List<String> testCaseWords = TestCaseUtils.splitTestCaseName(testCaseName, false);
            wordsInTestCases.addAll(testCaseWords);
        }
        int numberOfTestCases = testsOrderList.size();
        for (String testCaseWord : wordsInTestCases.elementSet()) {
            int count = wordsInTestCases.count(testCaseWord);
            if(count >= numberOfTestCases)
                result.add(testCaseWord);
        }
        return result.toArray(new String[0]);
    }
}
