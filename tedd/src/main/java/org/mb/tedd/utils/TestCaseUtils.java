package org.mb.tedd.utils;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCaseUtils {

    private final static Logger logger = Logger.getLogger(TestCaseUtils.class.getName());

    // identify stop words automatically (words that are constant in all test cases)
    // remove stop words only once: if stop word Name is present twice in a test case it is removed
    // only once by the test case name
    public static List<String> splitTestCaseName(String testCaseName, boolean removeStopWords){
        List<String> result = new ArrayList<>();
        Pattern pattern;
        if(Properties.TEST_CASE_WORDS_SEPARATOR.isEmpty()){
            pattern = Pattern.compile("[A-Z][a-z]+");
            // upper case first letter if it is lower case
            testCaseName = testCaseName.substring(0,1).toUpperCase() + testCaseName.substring(1);
        } else {
            return Arrays.asList(testCaseName.split(Properties.TEST_CASE_WORDS_SEPARATOR));
        }
        Matcher matcher = pattern.matcher(testCaseName);
        List<String> stopWords = Arrays.asList(Properties.stop_words);
        while(matcher.find()){
            String word = matcher.group();
            if(removeStopWords){
                if(!stopWords.contains(word)){
                    result.add(matcher.group());
                } else {
                    stopWords = new ArrayList<>(stopWords);
                    stopWords.remove(word);
                }
            }
            else
                result.add(matcher.group());
        }

        if(removeStopWords)
            logger.debug("Test case " + testCaseName + " without stop words: " + result);

        return result;
    }
}
