package org.mb.tedd.algorithm.filter.nlp;

import java.util.Optional;

public interface POSTaggerInterface {

    Optional<String> tagVerb(String testCaseName);

    String tagSentence(String testCaseName, String separatorTagWord, String wordDelimiter);

}
