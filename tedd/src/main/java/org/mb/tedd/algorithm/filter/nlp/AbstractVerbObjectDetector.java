package org.mb.tedd.algorithm.filter.nlp;

import com.google.common.base.Preconditions;
import edu.stanford.nlp.ling.TaggedWord;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractVerbObjectDetector {

    public static final String NO_OBJ = "no_obj";
    public static final String NO_VERB = "no_verb";
    protected static final String SEPARATOR_TAG_WORD = "/";
    protected static final String WORD_DELIMITER = ":";

    abstract VerbObjectType getVerbObject(String testCaseName);

    protected List<TaggedWord> fixTags(String taggedSentence){
        List<TaggedWord> taggedWords = Stream.of(taggedSentence.split(WORD_DELIMITER))
                .map(stringTaggedWord -> {
                    TaggedWord taggedWord = new TaggedWord();
                    String[] wordTag = stringTaggedWord.split(SEPARATOR_TAG_WORD);
                    Preconditions.checkState(wordTag.length == 2,
                            "Word " + stringTaggedWord + " must be formatted word" + SEPARATOR_TAG_WORD + "tag.");
                    String word = wordTag[0];
                    String tag = wordTag[1];
                    taggedWord.setWord(word);
                    taggedWord.setTag(tag);
                    return taggedWord;
                }).collect(Collectors.toList());
        // assumption only the first verb counts while dependency parsing is performing
        // the other verbs, if any, are turned into nouns
//        boolean verbFound = false;
//        for (TaggedWord taggedWord : taggedWords) {
//            boolean isVerb = taggedWord.tag().contains("V");
//            if(verbFound && taggedWord.tag().contains("V")
//                    && Wordnet.getInstance().isNoun(taggedWord.word()))
//                taggedWord.setTag("NN");
//            if(isVerb)
//                verbFound = true;
//        }
        return taggedWords;
    }

    protected Optional<String> getVerb(List<TaggedWord> taggedWords){
        for (TaggedWord taggedWord : taggedWords) {
            if(taggedWord.tag().startsWith("V"))
                return Optional.of(taggedWord.word());
        }
        return Optional.empty();
    }
}
