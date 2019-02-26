package org.mb.tedd.algorithm.filter.nlp;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Optional;

public class StanfordDependencyParser extends AbstractVerbObjectDetector {

    private MaxEntropyTagger maxEntropyTagger;
    private static final String MODEL_PATH = DependencyParser.DEFAULT_MODEL;
    private DependencyParser dependencyParser;
    private final static Logger logger = Logger.getLogger(StanfordDependencyParser.class.getName());

    public StanfordDependencyParser(){
        this.maxEntropyTagger = new MaxEntropyTagger();
        this.dependencyParser = DependencyParser.loadFromModelFile(MODEL_PATH);
    }

    @Override
    public VerbObjectType getVerbObject(String testCaseName) {

        String taggedSentence = this.maxEntropyTagger.tagSentence(testCaseName, SEPARATOR_TAG_WORD, WORD_DELIMITER);
        List<TaggedWord> taggedWords = this.fixTags(taggedSentence);
        GrammaticalStructure gs = this.dependencyParser.predict(taggedWords);

        Optional<TypedDependency> directObjectDependencyOptional = gs.allTypedDependencies().stream()
                .filter(typedDependency -> {
                    IndexedWord dep = typedDependency.dep();
                    IndexedWord gov = typedDependency.gov();
                    return gs.getGrammaticalRelation(gov, dep).getShortName().equals("dobj");
                }).findAny();

        if(directObjectDependencyOptional.isPresent()){
            logger.info("Dobj found: " + directObjectDependencyOptional.get() + ". Tagged sentence: " + taggedSentence + ", tagged words: " + taggedWords);
            return new VerbObjectType(directObjectDependencyOptional.get().dep().value(), directObjectDependencyOptional.get().gov().value());
        } else {
            Optional<String> verbOptional = this.getVerb(taggedWords);
            if(verbOptional.isPresent()){
                return new VerbObjectType(verbOptional.get());
            }
        }
        return new VerbObjectType();
    }

}
