package org.mb.tedd.algorithm.filter.nlp;

import com.google.common.base.Preconditions;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.apache.log4j.Logger;
import org.mb.tedd.utils.TestCaseUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MaxEntropyTagger implements POSTaggerInterface {

    private MaxentTagger maxentTagger;
    private final static String NOVERB = "noverb";
    private final static String VERB_BASE_FORM = "VB";
    private final static Logger logger = Logger.getLogger(MaxEntropyTagger.class.getName());

    public MaxEntropyTagger(){
        this.maxentTagger = new MaxentTagger(
                "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
    }

    @Override
    public Optional<String> tagVerb(String testCaseName) {
        List<String> words = TestCaseUtils.splitTestCaseName(testCaseName, true);
        List<Word> testCaseSentence = words.stream().map(Word::new).collect(Collectors.toList());
        List<TaggedWord> taggedWords = this.maxentTagger.tagSentence(testCaseSentence);
        if(this.containsVerb(taggedWords)){
            return Optional.of(this.getVerb(taggedWords));
        } else {
            return this.canBeVerb(taggedWords);
        }
    }

    @Override
    public String tagSentence(String testCaseName, String separatorTagWord, String wordDelimiter){
        Preconditions.checkNotNull(separatorTagWord, "Separator must not be null.");
        Preconditions.checkNotNull(wordDelimiter, "Word delimiter must not be null.");
        List<String> words = TestCaseUtils.splitTestCaseName(testCaseName, true);
        List<Word> testCaseSentence = words.stream().map(Word::new).collect(Collectors.toList());
        List<TaggedWord> taggedWords = this.maxentTagger.tagSentence(testCaseSentence);
        if(this.containsVerb(taggedWords)){
            return this.getTaggedTestCase(separatorTagWord, taggedWords, wordDelimiter);
        }
        Optional<String> optionalVerb = this.canBeVerb(taggedWords);
        if(optionalVerb.isPresent()){
            return this.getTaggedTestCase(optionalVerb.get(), separatorTagWord, taggedWords, wordDelimiter);
        } else {
            return this.getTaggedTestCase(separatorTagWord, taggedWords, wordDelimiter);
        }
    }

    private boolean containsVerb(List<TaggedWord> taggedWords){
        return taggedWords.stream().anyMatch(taggedWord -> taggedWord.tag().contains("V"));
    }

    /**
     * @return first word that can be a verb according to Wordnet
     * */
    private Optional<String> canBeVerb(List<TaggedWord> taggedWords){
        for (TaggedWord taggedWord : taggedWords) {
            if(Wordnet.getInstance().getVerbTag(taggedWord.word()))
                return Optional.of(taggedWord.word());
        }
        return Optional.empty();
    }

    private String getVerb(List<TaggedWord> taggedWords){
        for (TaggedWord taggedWord : taggedWords) {
            if(taggedWord.tag().startsWith("V"))
                return taggedWord.word();
        }
        throw new IllegalStateException("No verb found in " + taggedWords);
    }

    private String getTaggedTestCase(String verb, String separatorTagWord, List<TaggedWord> taggedWords, String wordDelimiter){
        return taggedWords.stream().map(taggedWord -> {
            if(taggedWord.word().equals(verb)){
                return taggedWord.word() + separatorTagWord + VERB_BASE_FORM;
            }
            return taggedWord.word() + separatorTagWord + taggedWord.tag();
        }).collect(Collectors.joining(wordDelimiter));
    }

    private String getTaggedTestCase(String separatorTagWord, List<TaggedWord> taggedWords, String wordDelimiter){
        return taggedWords.stream().map(taggedWord -> taggedWord.word() + separatorTagWord + taggedWord.tag())
                .collect(Collectors.joining(wordDelimiter));
    }
}
