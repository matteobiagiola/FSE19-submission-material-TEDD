package org.mb.tedd.algorithm.filter.nlp;

import edu.stanford.nlp.ling.TaggedWord;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NounsFinder extends AbstractVerbObjectDetector {

    private static final String SEPARATOR_TAG_WORD = "/";
    private static final String WORD_DELIMITER = ":";
    private POSTaggerInterface posTagger;
    private final static Logger logger = Logger.getLogger(NounsFinder.class.getName());

    public NounsFinder(POSTaggerInterface posTagger){
        this.posTagger = posTagger;
    }

    public VerbNouns getNouns(String testCaseName) {
        String taggedSentence = this.posTagger.tagSentence(testCaseName, SEPARATOR_TAG_WORD, WORD_DELIMITER);
        List<TaggedWord> taggedWords = this.fixTags(taggedSentence);
        List<String> nouns = this.getNouns(taggedWords);
        Optional<String> verbOptional = this.getVerb(taggedWords);
        if(verbOptional.isPresent()){
            return new VerbNouns(nouns, verbOptional.get());
        } else {
            return new VerbNouns(nouns);
        }
    }

    private List<String> getNouns(List<TaggedWord> taggedWords){
        return taggedWords.stream()
                .filter(taggedWord -> taggedWord.tag().startsWith("N"))
                .map(TaggedWord::word)
                .collect(Collectors.toList());
    }

    @Override
    VerbObjectType getVerbObject(String testCaseName) {
        throw new UnsupportedOperationException("Not possible to get verb object using NounsFinder");
    }
}
