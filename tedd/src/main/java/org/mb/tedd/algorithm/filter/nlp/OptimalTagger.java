package org.mb.tedd.algorithm.filter.nlp;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.mb.tedd.utils.Properties;
import org.mb.tedd.utils.TestCaseUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OptimalTagger implements POSTaggerInterface {

    private final static Logger logger = Logger.getLogger(OptimalTagger.class.getName());
    private Map<String, String> expectedResults;

    public OptimalTagger(){
        this.expectedResults = this.buildExpectedResults();
    }

    @Override
    public Optional<String> tagVerb(String testCaseName) {
        String verb = this.expectedResults.get(testCaseName);
        if(verb != null)
            return Optional.of(verb);
        return Optional.empty();
    }

    @Override
    public String tagSentence(String testCaseName, String separatorTagWord, String wordDelimiter) {
        throw new UnsupportedOperationException("Tag sentence not supported with OptimalTagger not supported yet!");
    }

    /**
     * @return map a test case with the corresponding verb (assuming that there is
     * always a verb in the test case name)
     * */
    private Map<String, String> buildExpectedResults(){
        Map<String, String> result = new LinkedHashMap<>();
        for (String testCaseName : Properties.tests_order) {
            List<String> words = TestCaseUtils.splitTestCaseName(testCaseName, false);
            Preconditions.checkState(Properties.VERB_POSITION_IN_TEST_CASE_NAME < words.size(),
                    "Test case " + testCaseName + " does not have " + Properties.VERB_POSITION_IN_TEST_CASE_NAME + " words.");
            result.put(testCaseName, words.get(Properties.VERB_POSITION_IN_TEST_CASE_NAME));
        }
        return result;
    }

}
